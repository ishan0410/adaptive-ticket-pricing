-- ============================================================
-- Adaptive Ticket Pricing — Database Schema
-- MySQL 8.x on AWS RDS
-- ============================================================

CREATE DATABASE IF NOT EXISTS adaptive_ticket
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE adaptive_ticket;

-- ── Users ──
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            ENUM('ADMIN', 'ORGANIZER', 'BUYER') NOT NULL DEFAULT 'BUYER',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_email (email)
) ENGINE=InnoDB;

-- ── Events ──
CREATE TABLE events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    organizer_id    BIGINT          NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    venue           VARCHAR(200)    NOT NULL,
    city            VARCHAR(100)    NOT NULL,
    category        VARCHAR(50),
    event_date      DATETIME        NOT NULL,
    total_capacity  INT             NOT NULL,
    description     TEXT,
    image_url       VARCHAR(500),
    status          ENUM('DRAFT', 'ACTIVE', 'SOLD_OUT', 'PAST') NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_event_date (event_date),
    INDEX idx_event_status (status),
    INDEX idx_event_organizer (organizer_id),
    CONSTRAINT fk_event_organizer FOREIGN KEY (organizer_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- ── Ticket Tiers ──
CREATE TABLE ticket_tiers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id        BIGINT          NOT NULL,
    tier_name       VARCHAR(50)     NOT NULL,
    base_price      DECIMAL(10, 2)  NOT NULL,
    current_price   DECIMAL(10, 2)  NOT NULL,
    total_quantity  INT             NOT NULL,
    sold            INT             NOT NULL DEFAULT 0,
    version         BIGINT          NOT NULL DEFAULT 0,  -- optimistic locking

    INDEX idx_tier_event (event_id),
    CONSTRAINT fk_tier_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ── Orders ──
CREATE TABLE orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    order_number    VARCHAR(20)     NOT NULL UNIQUE,
    total_amount    DECIMAL(10, 2)  NOT NULL,
    purchased_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_order_user (user_id),
    INDEX idx_order_number (order_number),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- ── Tickets ──
CREATE TABLE tickets (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT          NOT NULL,
    tier_id         BIGINT          NOT NULL,
    ticket_code     VARCHAR(20)     NOT NULL UNIQUE,
    price_paid      DECIMAL(10, 2)  NOT NULL,
    seat_label      VARCHAR(20),

    INDEX idx_ticket_order (order_id),
    INDEX idx_ticket_code (ticket_code),
    CONSTRAINT fk_ticket_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_tier  FOREIGN KEY (tier_id)  REFERENCES ticket_tiers(id)
) ENGINE=InnoDB;

-- ── Pricing History ──
CREATE TABLE pricing_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tier_id         BIGINT          NOT NULL,
    price           DECIMAL(10, 2)  NOT NULL,
    trigger_reason  ENUM('DEMAND', 'TIME', 'SCARCITY', 'MANUAL') NOT NULL,
    recorded_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_pricing_tier (tier_id),
    INDEX idx_pricing_recorded (recorded_at),
    CONSTRAINT fk_pricing_tier FOREIGN KEY (tier_id) REFERENCES ticket_tiers(id) ON DELETE CASCADE
) ENGINE=InnoDB;
