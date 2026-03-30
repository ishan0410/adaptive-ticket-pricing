# Adaptive Ticket Pricing

**A real-time dynamic pricing engine for event tickets** — built in 36 hours as a full-stack showcase combining a live interactive frontend, production-grade Spring Boot backend, and AWS-ready infrastructure.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?style=flat-square)
![React](https://img.shields.io/badge/React-18-blue?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square)
![AWS](https://img.shields.io/badge/AWS-EC2%20%7C%20RDS%20%7C%20ALB-FF9900?style=flat-square)

---

## The Problem

Fixed-price ticketing leaves money on the table and creates unfair access. A GA ticket priced at $50 three months out shouldn't cost the same when the event is 3 days away with only 8 seats left. Airlines, hotels, and ride-sharing platforms solved this years ago — event ticketing hasn't caught up.

## The Solution

An adaptive pricing engine that adjusts ticket prices in real time based on three independent factors:

```
finalPrice = basePrice × demandFactor × urgencyFactor × scarcityFactor
```

| Factor | Formula | What It Does |
|--------|---------|-------------|
| Demand | `1 + (soldRatio × 0.45)` | Price rises as more tickets sell (0% sold = 1.0x, 77% sold = 1.35x) |
| Urgency | Stepped: `<7d → 1.20x`, `<30d → 1.10x`, `<90d → 1.03x` | Price rises as event date approaches |
| Scarcity | `<10 left → 1.15x`, `<30 left → 1.06x` | Spike when a tier is almost gone |

The algorithm recalculates every 15 minutes via a scheduled background task and logs every price change with the trigger reason for full audit history.

## Live Demo

**Open `interactive-demo.html` in any browser** — no install, no server, no dependencies.

This is a fully self-contained single-file app (React 18 + Tailwind CSS loaded from CDN) that runs the complete user experience:

1. **Browse Events** — 4 real events with live adaptive pricing and availability bars
2. **Sign Up / Log In** — create an account or use `demo@example.com` / `demo1234`
3. **Select Tickets** — pick a tier, choose quantity, watch the price update in real time
4. **Checkout** — full booking flow with order confirmation, ticket codes, and seat assignments
5. **Dashboard** — view all your bookings with order history
6. **Engineering Panel** — click "Engineering" in the nav to explore architecture, API contracts, database schema, AWS infrastructure, and performance metrics

The frontend uses a mock API layer that mirrors the exact REST contracts, request/response shapes, and error handling of the Spring Boot backend. Every interaction you see maps 1:1 to a real backend endpoint.

## Project Structure

This project is split across two branches:

| Branch | What's In It |
|--------|-------------|
| `master` | Interactive frontend demo (`interactive-demo.html`), React source (`adaptive-ticket-pricing-site/`) |
| `backend` | Complete Spring Boot project — entities, services, controllers, security, tests, Docker, AWS configs |

## Tech Stack

**Frontend:** React 18, Tailwind CSS, Vite, mock API layer with simulated latency

**Backend:** Java 17, Spring Boot 3.2, Spring Security + JWT (jjwt 0.12.5), Spring Data JPA, Hibernate, HikariCP, MySQL 8

**Infrastructure:** AWS EC2 Auto Scaling (2–6 instances), RDS Multi-AZ, Application Load Balancer, CloudFront CDN, CloudWatch dashboards + alarms, SNS alerting

**DevOps:** Docker multi-stage builds, docker-compose for local dev, CloudFormation IaC, rolling deployment via ASG instance refresh

## Key Engineering Decisions

**Concurrent ticket purchases** — Two-layer locking strategy: `PESSIMISTIC_WRITE` (SELECT FOR UPDATE) on the tier row as the primary lock, plus `@Version` optimistic locking as a safety net. If two users try to buy the last ticket simultaneously, one waits for the DB lock; if inventory runs out while waiting, it throws `InsufficientInventoryException` (HTTP 422).

**N+1 query fix** — Event listing initially fired a separate SELECT for each event's tiers. Fixed with `JOIN FETCH` in the repository query, reducing 11 queries to 1.

**HikariCP tuning** — Default pool size of 10 caused connection exhaustion under 200+ concurrent users during load testing. Tuned to max 20 / min idle 5 with 60-second leak detection.

**Stateless auth** — JWT tokens with HMAC-SHA256 signing. No server-side sessions. The `JwtAuthenticationFilter` runs on every request, validates the token, and sets the SecurityContext so controllers can use `@AuthenticationPrincipal`.

## API Reference

```
POST   /api/auth/register              → 201 + JWT token
POST   /api/auth/login                 → 200 + JWT token
GET    /api/events?page=0&size=10      → Paginated events with live pricing
GET    /api/events/{id}                → Event detail + all tiers with current prices
POST   /api/tickets/purchase           → 201 + order confirmation with ticket codes
GET    /api/tickets/my-tickets         → User's booking history
GET    /api/events/{id}/pricing-history → Chronological price change audit log
```

## Performance Results

| Metric | Before Optimization | After | Improvement |
|--------|-------------------|-------|-------------|
| Concurrent users supported | ~200 | ~300+ | +50% |
| Event listing page load | 2.4s | 1.56s | -35% |
| API latency (p95) | 480ms | 190ms | -60% |
| DB queries per event listing | 11 | 1 | -91% |
| Uptime | Best effort | 99.9% target | SLA-backed |

## Quick Start

```bash
# Frontend — just open the file
open interactive-demo.html

# Or run the React dev server
cd adaptive-ticket-pricing-site
npm install && npm run dev
# → http://localhost:5173

# Backend (requires Java 17 + Docker)
cd backend
docker compose up -d        # starts MySQL + seeds data
mvn spring-boot:run         # starts API on :8080

# Run tests
mvn test                    # uses H2 in-memory, no MySQL needed
```
