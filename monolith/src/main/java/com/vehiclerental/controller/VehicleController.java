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

/**
 * Public vehicle endpoints used by customers.
 *
 * GET /api/vehicles               — all AVAILABLE vehicles (no date filter)
 * GET /api/vehicles/available     — AVAILABLE vehicles for a specific date range
 * GET /api/vehicles/category/{cat} — AVAILABLE vehicles by category
 * GET /api/vehicles/{id}          — single vehicle details
 */
@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "*")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    /**
     * Returns all vehicles with AVAILABLE status.
     * Use this for the initial page load before dates are selected.
     */
    @GetMapping
    public ResponseEntity<List<Vehicle>> getAvailableVehicles() {
        return ResponseEntity.ok(vehicleService.getAvailableVehicles());
    }

    /**
     * Returns vehicles by category (TWO_WHEELER or FOUR_WHEELER).
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Vehicle>> getVehiclesByCategory(@PathVariable VehicleCategory category) {
        return ResponseEntity.ok(vehicleService.getVehiclesByCategory(category));
    }

    /**
     * Returns AVAILABLE vehicles with no overlapping reservations in the date range.
     * This is the preferred search method when the customer has selected dates.
     *
     * @param startDate rental start (ISO format: yyyy-MM-dd)
     * @param endDate   rental end   (ISO format: yyyy-MM-dd)
     */
    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailableForDates(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "category", required = false) VehicleCategory category) {
        return ResponseEntity.ok(vehicleService.getAvailableVehiclesForDates(startDate, endDate, category));
    }

    /** Returns a single vehicle by its ID */
    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicle(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }
}
