package com.vehiclerental.service;

import com.vehiclerental.model.Vehicle;
import com.vehiclerental.model.VehicleCategory;
import com.vehiclerental.model.VehicleStatus;
import com.vehiclerental.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);
    }

    public List<Vehicle> getVehiclesByCategory(VehicleCategory category) {
        return vehicleRepository.findByCategoryAndStatus(category, VehicleStatus.AVAILABLE);
    }

    public List<Vehicle> getAvailableVehiclesForDates(LocalDate startDate, LocalDate endDate, VehicleCategory category) {
        if (startDate == null || endDate == null) {
            return category == null ? getAvailableVehicles() : getVehiclesByCategory(category);
        }
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date must be before or equal to end date.");
        }

        // Call RESERVATION-SERVICE to get IDs of vehicles that are reserved during these dates
        List<Long> reservedIds;
        try {
            reservedIds = webClientBuilder.build()
                    .get()
                    .uri("http://reservation-service/api/reservations/reserved-ids?startDate={start}&endDate={end}", startDate, endDate)
                    .retrieve()
                    .bodyToFlux(Long.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            // Fallback: log error and assume no vehicles are reserved (resilient)
            System.err.println("Error calling reservation-service: " + e.getMessage());
            reservedIds = Collections.emptyList();
        }

        List<Vehicle> available = getAvailableVehicles();
        
        // Filter out reserved vehicles and filter by category if specified
        List<Long> finalReservedIds = reservedIds != null ? reservedIds : Collections.emptyList();
        return available.stream()
                .filter(v -> !finalReservedIds.contains(v.getId()))
                .filter(v -> category == null || v.getCategory() == category)
                .toList();
    }

    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle with ID " + id + " not found."));
    }

    public Vehicle addVehicle(Vehicle vehicle) {
        if (vehicle.getStatus() == null) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(Long id, Vehicle details) {
        Vehicle existing = getVehicleById(id);

        if (details.getName()        != null) existing.setName(details.getName());
        if (details.getCategory()    != null) existing.setCategory(details.getCategory());
        if (details.getType()        != null) existing.setType(details.getType());
        if (details.getPricePerDay() != null) existing.setPricePerDay(details.getPricePerDay());
        if (details.getRating()      != null) existing.setRating(details.getRating());
        if (details.getImageUrl()    != null) existing.setImageUrl(details.getImageUrl());
        if (details.getStatus()      != null) existing.setStatus(details.getStatus());

        return vehicleRepository.save(existing);
    }

    public void deleteVehicle(Long id) {
        if (!vehicleRepository.existsById(id)) {
            throw new RuntimeException("Vehicle with ID " + id + " not found.");
        }
        vehicleRepository.deleteById(id);
    }
}
