package com.vehiclerental.dto;

import java.time.LocalDate;

public class ReservationResponse {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private UserDTO customer;
    private VehicleDTO vehicle;
    private RentalDTO rental;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UserDTO getCustomer() {
        return customer;
    }

    public void setCustomer(UserDTO customer) {
        this.customer = customer;
    }

    public VehicleDTO getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleDTO vehicle) {
        this.vehicle = vehicle;
    }

    public RentalDTO getRental() {
        return rental;
    }

    public void setRental(RentalDTO rental) {
        this.rental = rental;
    }
}
