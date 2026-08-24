package com.vehiclerental.repository;

import com.vehiclerental.model.Reservation;
import com.vehiclerental.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByCustomerIdOrderByStartDateDesc(Long customerId);

    List<Reservation> findByVehicleId(Long vehicleId);

    @Query("""
            SELECT r FROM Reservation r
            WHERE r.vehicleId = :vehicleId
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

    List<Reservation> findAllByOrderByIdDesc();

    long countByCustomerIdAndStatusNot(Long customerId, ReservationStatus status);
}
