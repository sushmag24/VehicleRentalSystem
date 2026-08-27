package com.vehiclerental.config;

import com.vehiclerental.model.Role;
import com.vehiclerental.model.User;
import com.vehiclerental.model.Vehicle;
import com.vehiclerental.model.VehicleCategory;
import com.vehiclerental.model.VehicleStatus;
import com.vehiclerental.repository.UserRepository;
import com.vehiclerental.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Admin User if not present
        if (userRepository.findByEmail("admin@vehiclerental.com").isEmpty()) {
            User admin = new User();
            admin.setName("Admin User");
            admin.setEmail("admin@vehiclerental.com");
            admin.setPassword("admin123");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println(">>> Initialized default Admin: admin@vehiclerental.com / admin123");
        }

        // 2. Seed Sample Vehicles if catalog is empty
        if (vehicleRepository.count() == 0) {
            List<Vehicle> initialVehicles = Arrays.asList(
                createVehicle("Toyota Fortuner", VehicleCategory.FOUR_WHEELER, "SUV", 4500.00, 4.8, "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?w=800"),
                createVehicle("Honda City", VehicleCategory.FOUR_WHEELER, "Sedan", 2500.00, 4.5, "https://images.unsplash.com/photo-1542281286-9e0a16bb7366?w=800"),
                createVehicle("Mahindra Thar", VehicleCategory.FOUR_WHEELER, "Off-road", 3500.00, 4.7, "https://images.unsplash.com/photo-1603386329225-868f9b1ee6c9?w=800"),
                createVehicle("Hyundai Verna", VehicleCategory.FOUR_WHEELER, "Sedan", 2200.00, 4.4, "https://images.unsplash.com/photo-1619682817481-e994891cd1f5?w=800"),
                createVehicle("Maruti Suzuki Swift", VehicleCategory.FOUR_WHEELER, "Hatchback", 2000.00, 4.3, "https://images.unsplash.com/photo-1590362891991-f776e747a588?w=800"),
                createVehicle("Tata Nexon", VehicleCategory.FOUR_WHEELER, "Compact SUV", 2800.00, 4.6, "https://images.unsplash.com/photo-1621359981975-2639be5070f6?w=800"),
                createVehicle("Kia Seltos", VehicleCategory.FOUR_WHEELER, "SUV", 3200.00, 4.7, "https://images.unsplash.com/photo-1594502184342-2e12f877aa73?w=800"),
                createVehicle("Mahindra XUV700", VehicleCategory.FOUR_WHEELER, "Premium SUV", 5500.00, 4.9, "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=800"),
                createVehicle("Royal Enfield Classic 350", VehicleCategory.TWO_WHEELER, "Cruiser", 1200.00, 4.9, "https://images.unsplash.com/photo-1558981806-ec527fa84c39?w=800"),
                createVehicle("KTM Duke 390", VehicleCategory.TWO_WHEELER, "Sport", 1500.00, 4.6, "https://images.unsplash.com/photo-1591637333184-19aa84b3e01f?w=800"),
                createVehicle("Activa 6G", VehicleCategory.TWO_WHEELER, "Scooter", 600.00, 4.3, "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800"),
                createVehicle("Bajaj Pulsar 220F", VehicleCategory.TWO_WHEELER, "Sport", 900.00, 4.2, "https://images.unsplash.com/photo-1444491741275-3747c53c99b4?w=800"),
                createVehicle("Yamaha R15 V4", VehicleCategory.TWO_WHEELER, "Sport", 1100.00, 4.7, "https://images.unsplash.com/photo-1558981285-6f0c94958bb6?w=800"),
                createVehicle("TVS Apache RTR 160", VehicleCategory.TWO_WHEELER, "Naked", 800.00, 4.4, "https://images.unsplash.com/photo-1599819811279-d5ad9cccf838?w=800"),
                createVehicle("Ola S1 Pro", VehicleCategory.TWO_WHEELER, "Electric Scooter", 1800.00, 4.4, "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800")
            );
            vehicleRepository.saveAll(initialVehicles);
            System.out.println(">>> Seeded " + initialVehicles.size() + " initial vehicles into the database.");
        }
    }

    private Vehicle createVehicle(String name, VehicleCategory category, String type, double price, double rating, String imageUrl) {
        Vehicle v = new Vehicle();
        v.setName(name);
        v.setCategory(category);
        v.setType(type);
        v.setPricePerDay(BigDecimal.valueOf(price));
        v.setRating(BigDecimal.valueOf(rating));
        v.setImageUrl(imageUrl);
        v.setStatus(VehicleStatus.AVAILABLE);
        return v;
    }
}
