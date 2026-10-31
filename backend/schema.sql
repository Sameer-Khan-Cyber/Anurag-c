-- ========================================================
-- FIND MY PG: Database Schema (MySQL / InnoDB / UTF-8mb4)
-- Roles: 'owner' and 'user' only (Admin removed)
-- ========================================================

CREATE DATABASE IF NOT EXISTS `pg_locator` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `pg_locator`;

DROP TABLE IF EXISTS `enquiries`;
DROP TABLE IF EXISTS `pg_listings`;
DROP TABLE IF EXISTS `users`;

-- 1. Users Table (Roles: owner, user)
CREATE TABLE `users` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `full_name` VARCHAR(150) NOT NULL,
  `email` VARCHAR(191) NOT NULL UNIQUE,
  `phone` VARCHAR(25) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `role` ENUM('owner', 'user') NOT NULL DEFAULT 'user',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_users_email` (`email`),
  INDEX `idx_users_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. PG Listings Table (status: approved, rejected; default approved)
CREATE TABLE `pg_listings` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `owner_id` INT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT NOT NULL,
  `address` VARCHAR(255) NOT NULL,
  `latitude` DECIMAL(10, 8) NOT NULL,
  `longitude` DECIMAL(11, 8) NOT NULL,
  `monthly_rent` DECIMAL(10, 2) NOT NULL,
  `amenities` TEXT NOT NULL COMMENT 'JSON array or comma-separated list: WiFi, AC, Food, Laundry, etc.',
  `rules` TEXT DEFAULT NULL,
  `image_url` VARCHAR(500) DEFAULT NULL,
  `is_occupied` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0: Available/Vacant, 1: Fully Occupied',
  `status` ENUM('approved', 'rejected') NOT NULL DEFAULT 'approved',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  INDEX `idx_pg_status` (`status`),
  INDEX `idx_pg_owner` (`owner_id`),
  INDEX `idx_pg_rent` (`monthly_rent`),
  INDEX `idx_pg_occupied` (`is_occupied`),
  INDEX `idx_pg_location` (`latitude`, `longitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Enquiries Table
CREATE TABLE `enquiries` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `pg_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `owner_id` INT NOT NULL,
  `message` TEXT NOT NULL,
  `status` ENUM('pending', 'replied', 'closed') NOT NULL DEFAULT 'pending',
  `owner_reply` TEXT DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`pg_id`) REFERENCES `pg_listings` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  INDEX `idx_enquiries_user` (`user_id`),
  INDEX `idx_enquiries_owner` (`owner_id`),
  INDEX `idx_enquiries_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initial Seed Data: Only Owner and User (Tenant)
-- Password for all seed users is: Password@123
INSERT INTO `users` (`id`, `full_name`, `email`, `phone`, `password_hash`, `role`) VALUES
(2, 'Rohit Sharma (Owner)', 'owner@pglocator.com', '+91 98765 43210', '$2y$10$wW2W1sI7Kz0T6cQ0W0hGne7R2cR1wL6v1.E17c3j4p5t0wK9v5f8u', 'owner'),
(3, 'Sneha Patel (Tenant)', 'user@pglocator.com', '+91 98765 43211', '$2y$10$wW2W1sI7Kz0T6cQ0W0hGne7R2cR1wL6v1.E17c3j4p5t0wK9v5f8u', 'user');

INSERT INTO `pg_listings` (`id`, `owner_id`, `title`, `description`, `address`, `latitude`, `longitude`, `monthly_rent`, `amenities`, `rules`, `image_url`, `is_occupied`, `status`) VALUES
(1, 2, 'Greenfield Luxury PG for Men & Women', 'Spacious air-conditioned rooms with high-speed WiFi, 3 meals daily, daily housekeeping, and 24/7 biometric security.', '42 Koramangala 4th Block, Bengaluru, KA', 12.93453300, 77.61902900, 12500.00, '["WiFi","AC","Food","Laundry","Security","Power Backup"]', 'No smoking inside rooms. Gate closes at 11:00 PM. Visitors allowed in lounge only.', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80', 0, 'approved'),
(2, 2, 'Skyline Executive PG Suites', 'Modern co-living space ideal for IT professionals. Includes dedicated work desks, gym, and rooftop cafeteria.', '15 Hitec City Main Rd, Madhapur, Hyderabad, TS', 17.44741200, 78.37623000, 15000.00, '["WiFi","AC","Food","Gym","Laundry","Security"]', 'Strictly vegetarian kitchen. Valid company ID required at check-in.', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80', 0, 'approved'),
(3, 2, 'Serene Residency Student PG', 'Budget friendly and quiet study atmosphere near university campuses. High-speed optic fiber and filtered water.', '88 North Campus Road, New Delhi, DL', 28.69234000, 77.21156000, 8500.00, '["WiFi","Food","Security","Water Purifier"]', 'Study hours observed between 9 PM to 6 AM. No loud music.', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80', 1, 'approved');

INSERT INTO `enquiries` (`id`, `pg_id`, `user_id`, `owner_id`, `message`, `status`, `owner_reply`) VALUES
(1, 1, 3, 2, 'Hi, I would like to schedule a room visit this Saturday afternoon. Is double sharing available?', 'replied', 'Hello Sneha! Yes, double sharing is available in Block B. You are welcome to visit between 2 PM and 6 PM this Saturday.'),
(2, 2, 3, 2, 'Is car parking space available in the basement for monthly residents?', 'pending', NULL);
