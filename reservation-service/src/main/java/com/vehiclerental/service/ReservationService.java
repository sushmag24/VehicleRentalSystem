package com.vehiclerental.service;

import com.vehiclerental.dto.*;
import com.vehiclerental.model.Reservation;
import com.vehiclerental.model.ReservationStatus;
import com.vehiclerental.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    public List<ReservationResponse> getAllReservations() {
        List<Reservation> reservations = reservationRepository.findAllByOrderByIdDesc();
        return reservations.stream()
                .map(this::compileReservationResponse)
                .toList();
    }

    public List<ReservationResponse> getReservationsByCustomerId(Long customerId) {
        List<Reservation> reservations = reservationRepository.findByCustomerIdOrderByStartDateDesc(customerId);
        return reservations.stream()
                .map(this::compileReservationResponse)
                .toList();
    }

    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation #" + id + " not found."));
        return compileReservationResponse(reservation);
    }

    public List<Long> getReservedVehicleIds(LocalDate startDate, LocalDate endDate) {
        List<Reservation> activeReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> r.getStartDate().compareTo(endDate) <= 0 && r.getEndDate().compareTo(startDate) >= 0)
                .toList();
        return activeReservations.stream()
                .map(Reservation::getVehicleId)
                .distinct()
                .toList();
    }

    public ReservationResponse createReservation(Long customerId, BookVehicleRequest request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (startDate == null || endDate == null) {
            throw new RuntimeException("Start date and End date are required.");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Start date cannot be in the past.");
        }
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date must be before or equal to end date.");
        }

        // 1. Verify Vehicle Availability status from vehicle-service
        VehicleDTO vehicle = fetchVehicle(request.getVehicleId());
        if ("MAINTENANCE".equalsIgnoreCase(vehicle.getStatus())) {
            throw new RuntimeException("Vehicle is under maintenance and cannot be reserved.");
        }

        // 2. Check overlap query
        List<Reservation> conflicts = reservationRepository.findOverlappingReservations(
                request.getVehicleId(), startDate, endDate, ReservationStatus.CANCELLED);
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Vehicle is already reserved for the selected dates.");
        }

        // 3. Create Pending Reservation in DB
        Reservation reservation = new Reservation();
        reservation.setCustomerId(customerId);
        reservation.setVehicleId(request.getVehicleId());
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setStatus(ReservationStatus.PENDING);
        Reservation savedReservation = reservationRepository.save(reservation);

        // 4. Pricing details
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        BigDecimal originalAmount = vehicle.getPricePerDay().multiply(BigDecimal.valueOf(days));

        // 5. Count non-cancelled bookings to apply the FIRST15 coupon rules
        long previousBookingsCount = reservationRepository.countByCustomerIdAndStatusNot(customerId, ReservationStatus.CANCELLED);

        // 6. Call payment-service to process payment and calculate discount amounts
        RentalDTO rental;
        try {
            Map<String, Object> paymentPayload = new HashMap<>();
            paymentPayload.put("reservationId", savedReservation.getId());
            paymentPayload.put("customerId", customerId);
            paymentPayload.put("originalAmount", originalAmount);
            paymentPayload.put("couponCode", request.getCouponCode());
            paymentPayload.put("paymentMethod", request.getPaymentMethod());
            paymentPayload.put("previousBookingsCount", previousBookingsCount);

            rental = webClientBuilder.build()
                    .post()
                    .uri("http://payment-service/api/payments")
                    .bodyValue(paymentPayload)
                    .retrieve()
                    .bodyToMono(RentalDTO.class)
                    .block();
        } catch (Exception e) {
            // Rollback reservation if payment fails
            reservationRepository.delete(savedReservation);
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }

        // 7. Complete reservation confirmation
        savedReservation.setStatus(ReservationStatus.CONFIRMED);
        savedReservation = reservationRepository.save(savedReservation);

        // 8. Update vehicle status to RESERVED in vehicle-service
        updateVehicleStatus(vehicle.getId(), "RESERVED");

        // 9. Trigger Asynchronous booking logging / notifications event
        triggerBookingNotification(savedReservation);

        // Map complete response
        ReservationResponse response = new ReservationResponse();
        response.setId(savedReservation.getId());
        response.setStartDate(savedReservation.getStartDate());
        response.setEndDate(savedReservation.getEndDate());
        response.setStatus(savedReservation.getStatus().name());
        response.setCustomer(fetchUser(customerId));
        response.setVehicle(vehicle);
        response.setRental(rental);

        return response;
    }

    public ReservationResponse cancelReservation(Long id, Long currentUserId, String currentUserRole) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation #" + id + " not found."));

        // Only the booking customer or an admin can cancel
        if (!"ADMIN".equalsIgnoreCase(currentUserRole) && !reservation.getCustomerId().equals(currentUserId)) {
            throw new RuntimeException("Access denied. You cannot cancel someone else's booking.");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("Reservation is already cancelled.");
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new RuntimeException("Completed bookings cannot be cancelled.");
        }

        // Update status to CANCELLED
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // Free vehicle status to AVAILABLE
        updateVehicleStatus(reservation.getVehicleId(), "AVAILABLE");

        // Trigger payment refund status update in payment-service
        try {
            webClientBuilder.build()
                    .post()
                    .uri("http://payment-service/api/payments/refund/{reservationId}", reservation.getId())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to trigger payment refund downstream: " + e.getMessage());
        }

        return compileReservationResponse(reservation);
    }

    public ReservationResponse updateReservationStatus(Long id, String statusStr) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation #" + id + " not found."));

        ReservationStatus newStatus;
        try {
            newStatus = ReservationStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: '" + statusStr + "'. Valid values: PENDING, CONFIRMED, CANCELLED, COMPLETED");
        }

        reservation.setStatus(newStatus);
        Reservation saved = reservationRepository.save(reservation);

        if (newStatus == ReservationStatus.CANCELLED || newStatus == ReservationStatus.COMPLETED) {
            updateVehicleStatus(reservation.getVehicleId(), "AVAILABLE");
        } else if (newStatus == ReservationStatus.CONFIRMED) {
            updateVehicleStatus(reservation.getVehicleId(), "RESERVED");
        }

        return compileReservationResponse(saved);
    }

    // ── ASYNC EVENT LOGGING ──────────────────────────────────────

    @Async
    public void triggerBookingNotification(Reservation reservation) {
        System.out.println("[ASYNC EVENT] Initializing booking notification flow for Reservation ID: " + reservation.getId());
        try {
            Thread.sleep(1500); // Simulate background mail server delivery / logging delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("[ASYNC EVENT] Confirmation email sent successfully to Customer ID: " + reservation.getCustomerId());
    }

    // ── INTER-SERVICE FETCHES (CONCURRENT VIA COMPLETABLEFUTURE) ──

    private ReservationResponse compileReservationResponse(Reservation r) {
        ReservationResponse res = new ReservationResponse();
        res.setId(r.getId());
        res.setStartDate(r.getStartDate());
        res.setEndDate(r.getEndDate());
        res.setStatus(r.getStatus().name());

        // Perform fetches concurrently
        CompletableFuture<UserDTO> customerFuture = CompletableFuture.supplyAsync(() -> fetchUser(r.getCustomerId()));
        CompletableFuture<VehicleDTO> vehicleFuture = CompletableFuture.supplyAsync(() -> fetchVehicle(r.getVehicleId()));
        CompletableFuture<RentalDTO> rentalFuture = CompletableFuture.supplyAsync(() -> fetchRental(r.getId()));

        try {
            CompletableFuture.allOf(customerFuture, vehicleFuture, rentalFuture).join();
            res.setCustomer(customerFuture.get());
            res.setVehicle(vehicleFuture.get());
            res.setRental(rentalFuture.get());
        } catch (Exception e) {
            System.err.println("Error zipping DTOs for reservation #" + r.getId() + ": " + e.getMessage());
            // Fallback content to keep UI running
            res.setCustomer(customerFuture.getNow(createFallbackUser(r.getCustomerId())));
            res.setVehicle(vehicleFuture.getNow(createFallbackVehicle(r.getVehicleId())));
            res.setRental(rentalFuture.getNow(createFallbackRental(r.getId())));
        }

        return res;
    }

    private UserDTO fetchUser(Long userId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://auth-service/api/auth/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserDTO.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to fetch user #" + userId + ": " + e.getMessage());
            return createFallbackUser(userId);
        }
    }

    private VehicleDTO fetchVehicle(Long vehicleId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://vehicle-service/api/vehicles/{id}", vehicleId)
                    .retrieve()
                    .bodyToMono(VehicleDTO.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to fetch vehicle #" + vehicleId + ": " + e.getMessage());
            return createFallbackVehicle(vehicleId);
        }
    }

    private RentalDTO fetchRental(Long reservationId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://payment-service/api/payments/reservation/{id}", reservationId)
                    .retrieve()
                    .bodyToMono(RentalDTO.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to fetch rental for reservation #" + reservationId + ": " + e.getMessage());
            return createFallbackRental(reservationId);
        }
    }

    private void updateVehicleStatus(Long vehicleId, String status) {
        try {
            Map<String, String> statusPayload = Map.of("status", status);
            webClientBuilder.build()
                    .put()
                    .uri("http://vehicle-service/api/admin/vehicles/{id}", vehicleId)
                    .bodyValue(statusPayload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to update vehicle status downstream: " + e.getMessage());
        }
    }

    // ── FALLBACK CREATORS ────────────────────────────────────────

    private UserDTO createFallbackUser(Long id) {
        UserDTO u = new UserDTO();
        u.setId(id);
        u.setName("Unknown User");
        u.setEmail("N/A");
        u.setRole("CUSTOMER");
        return u;
    }

    private VehicleDTO createFallbackVehicle(Long id) {
        VehicleDTO v = new VehicleDTO();
        v.setId(id);
        v.setName("Unknown Vehicle");
        v.setCategory("FOUR_WHEELER");
        v.setType("N/A");
        v.setPricePerDay(BigDecimal.ZERO);
        v.setStatus("AVAILABLE");
        return v;
    }

    private RentalDTO createFallbackRental(Long resId) {
        RentalDTO r = new RentalDTO();
        r.setReservationId(resId);
        r.setOriginalAmount(BigDecimal.ZERO);
        r.setDiscountAmount(BigDecimal.ZERO);
        r.setFinalAmount(BigDecimal.ZERO);
        r.setPaymentMethod("UPI");
        r.setPaymentStatus("UNPAID");
        return r;
    }
}
