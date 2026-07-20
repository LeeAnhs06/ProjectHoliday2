-- 1. Create Database if not exists
CREATE DATABASE IF NOT EXISTS `tcgdb` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `tcgdb`;

-- Disable foreign key checks temporarily to drop tables cleanly if needed
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `auction_bids`;
DROP TABLE IF EXISTS `auctions`;
DROP TABLE IF EXISTS `wishlist_items`;
DROP TABLE IF EXISTS `collection_items`;
DROP TABLE IF EXISTS `price_alerts`;
DROP TABLE IF EXISTS `order_items`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `listings`;
DROP TABLE IF EXISTS `price_records`;
DROP TABLE IF EXISTS `cards`;
DROP TABLE IF EXISTS `card_sets`;
DROP TABLE IF EXISTS `games`;
DROP TABLE IF EXISTS `users`;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. Create Tables
CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `email` VARCHAR(255) UNIQUE NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `display_name` VARCHAR(255) NOT NULL,
  `phone` VARCHAR(50),
  `address` VARCHAR(255),
  `avatar_url` VARCHAR(255),
  `role` VARCHAR(50) NOT NULL,
  `status` VARCHAR(50) NOT NULL,
  `balance` DOUBLE DEFAULT 0.0
) ENGINE=InnoDB;

CREATE TABLE `games` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) UNIQUE NOT NULL,
  `code` VARCHAR(50) UNIQUE NOT NULL
) ENGINE=InnoDB;

CREATE TABLE `card_sets` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `game_id` BIGINT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `code` VARCHAR(50) NOT NULL,
  FOREIGN KEY (`game_id`) REFERENCES `games` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `cards` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `set_id` BIGINT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `rarity` VARCHAR(100),
  `collector_number` VARCHAR(50),
  `language` VARCHAR(10) NOT NULL,
  `is_foil` BOOLEAN DEFAULT FALSE,
  `image_url` VARCHAR(255),
  `description` TEXT,
  FOREIGN KEY (`set_id`) REFERENCES `card_sets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `price_records` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `card_id` BIGINT NOT NULL,
  `recorded_at` DATETIME NOT NULL,
  `lowest_price` DOUBLE,
  `median_price` DOUBLE,
  `market_price` DOUBLE,
  `data_source` VARCHAR(255),
  FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `listings` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `seller_id` BIGINT NOT NULL,
  `card_id` BIGINT NOT NULL,
  `card_condition` VARCHAR(50) NOT NULL,
  `quantity` INT NOT NULL,
  `price` DOUBLE NOT NULL,
  `image_urls` TEXT,
  `description` TEXT,
  `status` VARCHAR(50) NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `orders` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_code` VARCHAR(100) UNIQUE NOT NULL,
  `buyer_id` BIGINT NOT NULL,
  `seller_id` BIGINT NOT NULL,
  `shipping_address` VARCHAR(255) NOT NULL,
  `payment_method` VARCHAR(50) NOT NULL,
  `platform_fee` DOUBLE DEFAULT 0.0,
  `total_amount` DOUBLE DEFAULT 0.0,
  `status` VARCHAR(50) NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`buyer_id`) REFERENCES `users` (`id`),
  FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `order_items` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `listing_id` BIGINT NOT NULL,
  `price` DOUBLE NOT NULL,
  `quantity` INT NOT NULL,
  FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`listing_id`) REFERENCES `listings` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `price_alerts` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `card_id` BIGINT NOT NULL,
  `target_price` DOUBLE NOT NULL,
  `condition_filter` VARCHAR(50),
  `notify_channel` VARCHAR(50) NOT NULL,
  `frequency` VARCHAR(50) NOT NULL,
  `status` VARCHAR(50) NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `collection_items` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `card_id` BIGINT NOT NULL,
  `card_condition` VARCHAR(50) NOT NULL,
  `quantity` INT NOT NULL,
  `purchase_price` DOUBLE,
  `note` TEXT,
  `image_url` VARCHAR(255),
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `wishlist_items` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `card_id` BIGINT NOT NULL,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `auctions` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `seller_id` BIGINT NOT NULL,
  `card_id` BIGINT NOT NULL,
  `start_price` DOUBLE NOT NULL,
  `min_increment` DOUBLE NOT NULL,
  `current_price` DOUBLE NOT NULL,
  `end_time` DATETIME NOT NULL,
  `card_condition` VARCHAR(50) NOT NULL,
  `status` VARCHAR(50) NOT NULL,
  `winner_id` BIGINT,
  `is_paid` BOOLEAN DEFAULT FALSE,
  `description` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`winner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `auction_bids` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `auction_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `bid_price` DOUBLE NOT NULL,
  `bid_time` DATETIME NOT NULL,
  FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Insert Initial Data
-- passwords are BCrypt hashes of: adminpassword, buyerpassword, seller1password, seller2password
INSERT INTO `users` (`id`, `email`, `password`, `display_name`, `phone`, `address`, `avatar_url`, `role`, `status`, `balance`) VALUES
(1, 'admin@tcg.com', '$2a$10$0d77kG2rOQ451rVnL.X90.t10PUpW9X.w2yU1w.i0hH1.P2x1Vv2m', 'TCG Admin', '0123456789', 'Hanoi, Vietnam', NULL, 'ADMIN', 'ACTIVE', 1000000.0),
(2, 'buyer@tcg.com', '$2a$10$K7C6dG5hJ8yL4eF.T8U8Z.x90VpW2X.yU1w.i0hH1.P2x1Vv2m', 'John Buyer', '0987654321', 'Ho Chi Minh City, Vietnam', NULL, 'MEMBER', 'ACTIVE', 5000000.0),
(3, 'seller1@tcg.com', '$2a$10$w6M6cG5hJ8yL4eF.T8U8Z.y90VpW2X.zU1w.i0hH1.P2x1Vv2m', 'Dragon TCG Store', '0909090909', 'Danang, Vietnam', NULL, 'SELLER', 'ACTIVE', 0.0),
(4, 'seller2@tcg.com', '$2a$10$x6M6cG5hJ8yL4eF.T8U8Z.z90VpW2X.aU1w.i0hH1.P2x1Vv2m', 'Gamer Paradise', '0808080808', 'Haiphong, Vietnam', NULL, 'SELLER', 'ACTIVE', 0.0);

INSERT INTO `games` (`id`, `name`, `code`) VALUES
(1, 'Pokemon TCG', 'pokemon'),
(2, 'Yu-Gi-Oh!', 'yugioh');

INSERT INTO `card_sets` (`id`, `game_id`, `name`, `code`) VALUES
(1, 1, 'Obsidian Flames', 'sv03'),
(2, 2, 'Legend of Blue Eyes White Dragon', 'lob');

INSERT INTO `cards` (`id`, `set_id`, `name`, `rarity`, `collector_number`, `language`, `is_foil`, `image_url`, `description`) VALUES
(1, 1, 'Charizard ex (Special Illustration Rare)', 'Special Illustration Rare', '223/197', 'EN', 1, 'https://images.pokemontcg.io/sv3/223.png', 'Flame Pokémon. Weakness: Grass, Retreat Cost: 2.'),
(2, 2, 'Blue-Eyes White Dragon', 'Ultra Rare', 'LOB-001', 'EN', 1, 'https://images.ygoprodeck.com/images/cards/89631139.jpg', 'This legendary dragon is a powerful engine of destruction. Virtually invincible, very few have faced this awesome creature and lived to tell the tale.'),
(3, 2, 'Dark Magician', 'Ultra Rare', 'LOB-005', 'EN', 1, 'https://images.ygoprodeck.com/images/cards/46986414.jpg', 'The ultimate wizard in terms of attack and defense.');

-- 7 days historical price records
INSERT INTO `price_records` (`card_id`, `recorded_at`, `lowest_price`, `median_price`, `market_price`, `data_source`) VALUES
(1, NOW() - INTERVAL 7 DAY, 1300000, 1400000, 1400000, 'Market Index'),
(1, NOW() - INTERVAL 5 DAY, 1320000, 1420000, 1420000, 'Market Index'),
(1, NOW() - INTERVAL 3 DAY, 1350000, 1450000, 1450000, 'Market Index'),
(1, NOW(), 1390000, 1480000, 1480000, 'Market Index'),
(2, NOW() - INTERVAL 7 DAY, 3200000, 3500000, 3500000, 'Market Index'),
(2, NOW() - INTERVAL 4 DAY, 3300000, 3600000, 3600000, 'Market Index'),
(2, NOW(), 3420000, 3800000, 3800000, 'Market Index');

INSERT INTO `listings` (`id`, `seller_id`, `card_id`, `card_condition`, `quantity`, `price`, `image_urls`, `description`, `status`, `created_at`) VALUES
(1, 3, 1, 'NEAR_MINT', 5, 1480000.0, 'https://images.pokemontcg.io/sv3/223.png', 'Pack fresh Near Mint Charizard ex. Sent in a bubble mailer.', 'ACTIVE', NOW() - INTERVAL 5 HOUR),
(2, 4, 1, 'LIGHTLY_PLAYED', 2, 1390000.0, 'https://images.pokemontcg.io/sv3/223.png', 'Slight corner wear, otherwise clean back.', 'ACTIVE', NOW() - INTERVAL 2 HOUR),
(3, 3, 2, 'MINT', 1, 3800000.0, 'https://images.ygoprodeck.com/images/cards/89631139.jpg', 'PSA grade candidate. Absolutely pristine condition.', 'ACTIVE', NOW() - INTERVAL 1 DAY),
(4, 4, 3, 'MODERATELY_PLAYED', 3, 1100000.0, 'https://images.ygoprodeck.com/images/cards/46986414.jpg', 'Playable condition. Small scratch on the back.', 'ACTIVE', NOW() - INTERVAL 12 HOUR);
