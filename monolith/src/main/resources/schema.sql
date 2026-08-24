-- ============================================================
--  Vehicle Rental System - MySQL Schema
--  Run this in MySQL Workbench or CLI: mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS vehicle_rental
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE vehicle_rental;

-- ─── users ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    password        VARCHAR(255)    NOT NULL  COMMENT 'Plain-text for MVP; use BCrypt in production',
    license_number  VARCHAR(50),
    role            ENUM('ADMIN','CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
    PRIMARY KEY (id),
    UNIQUE  INDEX idx_user_email (email),
    INDEX         idx_user_role  (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── vehicles ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vehicles (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(150)    NOT NULL,
    category        ENUM('TWO_WHEELER', 'FOUR_WHEELER') NOT NULL,
    type            VARCHAR(50)     NOT NULL,
    price_per_day   DECIMAL(10,2)   NOT NULL,
    rating          DECIMAL(2,1)    DEFAULT 4.5,
    image_url       TEXT,
    status          ENUM('AVAILABLE','RESERVED','RENTED','MAINTENANCE')
                    NOT NULL DEFAULT 'AVAILABLE',
    PRIMARY KEY (id),
    INDEX idx_vehicle_status (status),
    INDEX idx_vehicle_type   (type),
    INDEX idx_vehicle_cat    (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── reservations ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reservations (
    id              BIGINT  NOT NULL AUTO_INCREMENT,
    customer_id     BIGINT  NOT NULL,
    vehicle_id      BIGINT  NOT NULL,
    start_date      DATE    NOT NULL,
    end_date        DATE    NOT NULL,
    status          ENUM('PENDING','CONFIRMED','CANCELLED','COMPLETED')
                    NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (id),
    CONSTRAINT fk_res_customer FOREIGN KEY (customer_id)
        REFERENCES users(id)    ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_res_vehicle  FOREIGN KEY (vehicle_id)
        REFERENCES vehicles(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_res_customer (customer_id),
    INDEX idx_res_vehicle  (vehicle_id),
    INDEX idx_res_dates    (start_date, end_date),
    INDEX idx_res_status   (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── rentals (financial record) ──────────────────────────────
CREATE TABLE IF NOT EXISTS rentals (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    reservation_id  BIGINT          NOT NULL,
    original_amount DECIMAL(10,2)   NOT NULL,
    discount_amount DECIMAL(10,2)   DEFAULT 0.00,
    final_amount    DECIMAL(10,2)   NOT NULL,
    coupon_code     VARCHAR(20),
    payment_method  ENUM('GPAY','PHONEPE','PAYTM','UPI','CARD','NET_BANKING') NOT NULL,
    payment_status  ENUM('UNPAID','PAID','REFUNDED') NOT NULL DEFAULT 'UNPAID',
    PRIMARY KEY (id),
    CONSTRAINT uq_rental_reservation UNIQUE (reservation_id),
    CONSTRAINT fk_rental_reservation FOREIGN KEY (reservation_id)
        REFERENCES reservations(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Seed Data (Safe for existing database) ──────────────────
-- Admin account
INSERT IGNORE INTO users (name, email, password, license_number, role) VALUES
  ('Admin User', 'admin@vehiclerental.com', 'admin123', NULL, 'ADMIN');

-- Sample vehicles (Only inserted if names don't conflict)
-- We add a unique index on name first to support INSERT IGNORE
ALTER TABLE vehicles ADD UNIQUE INDEX IF NOT EXISTS idx_vehicle_name_unique (name);

INSERT IGNORE INTO vehicles (name, category, type, price_per_day, rating, image_url, status) VALUES
  ('Toyota Fortuner', 'FOUR_WHEELER', 'SUV', 4500.00, 4.8, 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?w=800', 'AVAILABLE'),
  ('Honda City', 'FOUR_WHEELER', 'Sedan', 2500.00, 4.5, 'https://images.unsplash.com/photo-1542281286-9e0a16bb7366?w=800', 'AVAILABLE'),
  ('Mahindra Thar', 'FOUR_WHEELER', 'Off-road', 3500.00, 4.7, 'https://images.unsplash.com/photo-1603386329225-868f9b1ee6c9?w=800', 'AVAILABLE'),
  ('Hyundai Verna', 'FOUR_WHEELER', 'Sedan', 2200.00, 4.4, 'https://images.unsplash.com/photo-1619682817481-e994891cd1f5?w=800', 'AVAILABLE'),
  ('Maruti Suzuki Swift', 'FOUR_WHEELER', 'Hatchback', 2000.00, 4.3, 'https://images.unsplash.com/photo-1590362891991-f776e747a588?w=800', 'AVAILABLE'),
  ('Tata Nexon', 'FOUR_WHEELER', 'Compact SUV', 2800.00, 4.6, 'https://images.unsplash.com/photo-1621359981975-2639be5070f6?w=800', 'AVAILABLE'),
  ('Kia Seltos', 'FOUR_WHEELER', 'SUV', 3200.00, 4.7, 'https://images.unsplash.com/photo-1594502184342-2e12f877aa73?w=800', 'AVAILABLE'),
  ('Volkswagen Virtus', 'FOUR_WHEELER', 'Sedan', 2600.00, 4.5, 'https://images.unsplash.com/photo-1625232900010-8b0101b091f3?w=800', 'AVAILABLE'),
  ('Mahindra XUV700', 'FOUR_WHEELER', 'Premium SUV', 5500.00, 4.9, 'https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=800', 'AVAILABLE'),
  ('Toyota Innova Hycross', 'FOUR_WHEELER', 'MPV', 6000.00, 4.8, 'https://images.unsplash.com/photo-1583121274602-3e2820c69888?w=800', 'AVAILABLE'),
  
  ('Royal Enfield Classic 350', 'TWO_WHEELER', 'Cruiser', 1200.00, 4.9, 'https://images.unsplash.com/photo-1558981403-c5f97cb94ad2?w=800', 'AVAILABLE'),
  ('KTM Duke 390', 'TWO_WHEELER', 'Sport', 1500.00, 4.6, 'https://images.unsplash.com/photo-1591637333184-19aa84b3e01f?w=800', 'AVAILABLE'),
  ('Activa 6G', 'TWO_WHEELER', 'Scooter', 600.00, 4.3, 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800', 'AVAILABLE'),
  ('Bajaj Pulsar 220F', 'TWO_WHEELER', 'Sport', 900.00, 4.2, 'https://images.unsplash.com/photo-1444491741275-3747c53c99b4?w=800', 'AVAILABLE'),
  ('Yamaha R15 V4', 'TWO_WHEELER', 'Sport', 1100.00, 4.7, 'https://images.unsplash.com/photo-1558981285-6f0c94958bb6?w=800', 'AVAILABLE'),
  ('TVS Apache RTR 160', 'TWO_WHEELER', 'Naked', 800.00, 4.4, 'https://images.unsplash.com/photo-1599819811279-d5ad9cccf838?w=800', 'AVAILABLE'),
  ('Suzuki Access 125', 'TWO_WHEELER', 'Scooter', 550.00, 4.5, 'https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?w=800', 'AVAILABLE'),
  ('Honda CB350', 'TWO_WHEELER', 'Retro', 1300.00, 4.8, 'https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=800', 'AVAILABLE'),
  ('Hero Splendor Plus', 'TWO_WHEELER', 'Commuter', 400.00, 4.6, 'https://images.unsplash.com/photo-1595079676339-1534801ad6cf?w=800', 'AVAILABLE'),
  ('Ola S1 Pro', 'TWO_WHEELER', 'Electric Scooter', 1800.00, 4.4, 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800', 'AVAILABLE');
