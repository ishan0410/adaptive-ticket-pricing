-- ============================================================
-- Seed Data — Local Development
-- Run after schema.sql
-- ============================================================

USE adaptive_ticket;

-- ── Users (passwords are BCrypt hash of "demo1234") ──
INSERT INTO users (name, email, password_hash, role) VALUES
('Ishan Madhani', 'ishan@example.com', '$2a$12$LJ3m4ys6Gx0bJxNPOIF9/.aBGMGj3CYCdjv7R7bbrVgjmSbTFkWUe', 'ADMIN'),
('Sarah Chen', 'sarah@example.com', '$2a$12$LJ3m4ys6Gx0bJxNPOIF9/.aBGMGj3CYCdjv7R7bbrVgjmSbTFkWUe', 'ORGANIZER'),
('Demo User', 'demo@example.com', '$2a$12$LJ3m4ys6Gx0bJxNPOIF9/.aBGMGj3CYCdjv7R7bbrVgjmSbTFkWUe', 'BUYER');

-- ── Events ──
INSERT INTO events (organizer_id, name, venue, city, category, event_date, total_capacity, description, image_url, status) VALUES
(2, 'Austin Tech Conference 2025', 'Austin Convention Center', 'Austin, TX', 'Technology',
 '2025-09-15 10:00:00', 500,
 'Three days of talks, workshops, and networking with the best in tech. Featuring keynotes from industry leaders on AI, cloud infrastructure, and distributed systems.',
 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=600&h=340&fit=crop', 'ACTIVE'),

(2, 'Indie Music Festival', 'Zilker Park Amphitheater', 'Austin, TX', 'Music',
 '2025-10-22 16:00:00', 2000,
 'A full-day outdoor music festival featuring 12 indie bands, food trucks, and local craft vendors. Rain or shine.',
 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&h=340&fit=crop', 'ACTIVE'),

(2, 'Startup Pitch Night', 'Capital Factory', 'Austin, TX', 'Business',
 '2025-11-05 18:30:00', 150,
 'Watch 10 early-stage startups pitch to a panel of VCs. Network with founders, investors, and fellow tech enthusiasts.',
 'https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=600&h=340&fit=crop', 'ACTIVE'),

(2, 'Cloud Architecture Summit', 'Marriott Downtown', 'San Francisco, CA', 'Technology',
 '2025-12-10 09:00:00', 300,
 'Deep dives into AWS, GCP, and Azure architectures. Hands-on labs for auto-scaling, serverless, and container orchestration.',
 'https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=600&h=340&fit=crop', 'ACTIVE');

-- ── Ticket Tiers ──
-- Event 1: Austin Tech Conference
INSERT INTO ticket_tiers (event_id, tier_name, base_price, current_price, total_quantity, sold) VALUES
(1, 'General Admission', 49.99, 49.99, 300, 231),
(1, 'VIP',               149.99, 149.99, 150, 98),
(1, 'Workshop Pass',     89.99, 89.99, 50, 18);

-- Event 2: Indie Music Festival
INSERT INTO ticket_tiers (event_id, tier_name, base_price, current_price, total_quantity, sold) VALUES
(2, 'General Admission',      35.00, 35.00, 1500, 1220),
(2, 'Front Stage',            85.00, 85.00, 400, 245),
(2, 'Backstage Meet & Greet', 200.00, 200.00, 100, 24);

-- Event 3: Startup Pitch Night
INSERT INTO ticket_tiers (event_id, tier_name, base_price, current_price, total_quantity, sold) VALUES
(3, 'Attendee',        25.00, 25.00, 120, 38),
(3, 'Investor Circle', 75.00, 75.00, 30, 4);

-- Event 4: Cloud Architecture Summit
INSERT INTO ticket_tiers (event_id, tier_name, base_price, current_price, total_quantity, sold) VALUES
(4, 'Conference Pass',       129.00, 129.00, 200, 188),
(4, 'Conference + Labs',     249.00, 249.00, 100, 90);

-- ── Sample Pricing History (simulates past price changes) ──
INSERT INTO pricing_history (tier_id, price, trigger_reason, recorded_at) VALUES
(1, 49.99, 'DEMAND', '2025-06-01 00:00:00'),
(1, 52.00, 'DEMAND', '2025-07-01 00:00:00'),
(1, 55.50, 'DEMAND', '2025-07-15 00:00:00'),
(1, 58.00, 'DEMAND', '2025-08-01 00:00:00'),
(1, 61.25, 'TIME',   '2025-08-15 00:00:00'),
(1, 64.00, 'TIME',   '2025-09-01 00:00:00'),
(2, 149.99, 'DEMAND', '2025-06-01 00:00:00'),
(2, 155.00, 'DEMAND', '2025-07-01 00:00:00'),
(2, 168.00, 'DEMAND', '2025-08-01 00:00:00'),
(2, 182.00, 'TIME',   '2025-09-01 00:00:00');
