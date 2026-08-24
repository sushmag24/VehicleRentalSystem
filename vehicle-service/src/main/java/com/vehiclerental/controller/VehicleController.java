package com.vehiclerental.controller;

import com.vehiclerental.model.Vehicle;
import com.vehiclerental.model.VehicleCategory;
import com.vehiclerental.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController

public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    // ── CUSTOMER ENDPOINTS ──────────────────────────────────────

    @GetMapping("/api/vehicles")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles() {
        return ResponseEntity.ok(vehicleService.getAvailableVehicles());
    }

    @GetMapping("/api/vehicles/category/{category}")
    public ResponseEntity<List<Vehicle>> getVehiclesByCategory(@PathVariable VehicleCategory category) {
        return ResponseEntity.ok(vehicleService.getVehiclesByCategory(category));
    }

    @GetMapping("/api/vehicles/available")
    public ResponseEntity<List<Vehicle>> getAvailableForDates(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "category", required = false) VehicleCategory category) {
        return ResponseEntity.ok(vehicleService.getAvailableVehiclesForDates(startDate, endDate, category));
    }

    @GetMapping("/api/vehicles/{id}")
    public ResponseEntity<Vehicle> getVehicle(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    // ── ADMIN ENDPOINTS ─────────────────────────────────────────

    @GetMapping("/api/admin/vehicles")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @PostMapping("/api/admin/vehicles")
    public ResponseEntity<Vehicle> addVehicle(@RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(vehicleService.addVehicle(vehicle));
    }

    @PutMapping("/api/admin/vehicles/{id}")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, vehicle));
    }

    @DeleteMapping("/api/admin/vehicles/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok(Map.of("message", "Vehicle #" + id + " deleted successfully."));
    }
}
