package com.vehiclerental.repository;

import com.vehiclerental.model.Reservation;
import com.vehiclerental.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Reservation CRUD and custom availability queries.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Get all reservations made by a specific customer */
    List<Reservation> findByCustomer_IdOrderByStartDateDesc(Long customerId);

    /** Get all reservations for a specific vehicle */
    List<Reservation> findByVehicle_Id(Long vehicleId);

    /**
     * Availability overlap check.
     *
     * A conflict exists when:
     *   existing.startDate <= requested.endDate  AND
     *   existing.endDate   >= requested.startDate
     *
     * Cancelled reservations are excluded from the check.
     * Uses <> (not equals) which is valid JPQL — not != which is not standard.
     */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.vehicle.id = :vehicleId
              AND r.startDate <= :endDate
              AND r.endDate   >= :startDate
              AND r.status <> :cancelled
            """)
    List<Reservation> findOverlappingReservations(
            @Param("vehicleId")  Long vehicleId,
            @Param("startDate")  LocalDate startDate,
            @Param("endDate")    LocalDate endDate,
            @Param("cancelled")  ReservationStatus cancelled
    );

    /** All reservations ordered by newest first (for admin view) */
    List<Reservation> findAllByOrderByIdDesc();

    /** Count non-cancelled reservations for a specific customer */
    long countByCustomer_IdAndStatusNot(Long customerId, ReservationStatus status);
}
