package com.vehiclerental.repository;

import com.vehiclerental.model.Vehicle;
import com.vehiclerental.model.VehicleCategory;
import com.vehiclerental.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Vehicle CRUD and date-range availability queries.
 */
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /** Find all vehicles with a given status (e.g., AVAILABLE) */
    List<Vehicle> findByStatus(VehicleStatus status);

    /** Find vehicles by category and status */
    List<Vehicle> findByCategoryAndStatus(VehicleCategory category, VehicleStatus status);

    /**
     * Find vehicles that are AVAILABLE AND have no conflicting reservations
     * in the requested date range.
     *
     * A vehicle is available only if no active reservation overlaps:
     *   existing.startDate <= requested.endDate  AND
     *   existing.endDate   >= requested.startDate
     *
     * Active = not CANCELLED.
     */
    @Query("""
            SELECT v FROM Vehicle v
            WHERE v.status = 'AVAILABLE'
              AND v.id NOT IN (
                  SELECT r.vehicle.id FROM Reservation r
                  WHERE r.startDate <= :endDate
                    AND r.endDate   >= :startDate
                    AND r.status <> 'CANCELLED'
              )
            """)
    List<Vehicle> findAvailableVehiclesForDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );
}
