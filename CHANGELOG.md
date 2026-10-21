# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0] - 2026-10-21

First public release.

### Added
- Booking flow: movie, showtime, interactive seat map, snacks and checkout, with prices, seat availability and snack stock checked on the server.
- Seat reservations stored per booking; a seat can only be sold once per showtime, even when two customers check out at the same moment.
- Stripe card payments, plus a demo mode that confirms bookings without charging when no Stripe keys are configured.
- Loyalty program: points for tickets and snacks, Bronze/Silver/Gold tiers and a free-ticket reward at checkout.
- Streaming-style home page, movie catalog with title and genre search, and booking history.
- Self-service password reset with short-lived 6-digit codes.
- Account settings for every role: update profile details and change password.
- Contact form with an admin inbox.
- Admin console: dashboard, sales analytics, movie and snack management, booking management, loyalty members and feedback trends.
- Employee portal with this month's shifts and salary.
- Role-based access control for customers, employees and admins, with CSRF protection on every state-changing request.
- Themed 403, 404 and error pages.
- Docker image, Docker Compose setup and demo data.
- CI, CodeQL scanning, Dependabot and a tag-driven release workflow that publishes the jar and a container image.

[Unreleased]: https://github.com/AnnusShahzad/AbsoluteCinema/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/AnnusShahzad/AbsoluteCinema/releases/tag/v1.0.0
