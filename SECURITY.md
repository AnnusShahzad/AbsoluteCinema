# Security Policy

## Supported versions

Security fixes are made on the latest release and on `main`.

## Reporting a vulnerability

Please **do not open a public issue** for security problems.

Report them privately through [GitHub Security Advisories](https://github.com/AnnusShahzad/AbsoluteCinema/security/advisories/new). Include the steps to reproduce, the impact, and any suggested fix. You can expect an acknowledgement within a few days, and we will keep you updated until a fix is released. Reporters are credited in the release notes unless they prefer otherwise.

## Deploying safely

- Set `DB_PASSWORD`, `STRIPE_PUBLIC_KEY` and `STRIPE_SECRET_KEY` through environment variables or a secret manager; never commit them.
- The demo accounts in `scripts/demo-data.sql` share a published password. Do not load that file into a production database.
- Password reset codes are written to the application log because the project ships without a mail server. Connect a real email or SMS sender before going live.
