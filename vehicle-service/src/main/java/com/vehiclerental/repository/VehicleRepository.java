package com.vehiclerental.repository;

import com.vehiclerental.model.Vehicle;
import com.vehiclerental.model.VehicleCategory;
import com.vehiclerental.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStatus(VehicleStatus status);
    List<Vehicle> findByCategoryAndStatus(VehicleCategory category, VehicleStatus status);
}
