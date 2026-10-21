# Contributing to Absolute Cinema

Thanks for your interest in improving Absolute Cinema! Bug reports, feature ideas, documentation fixes and code are all welcome.

## Ways to contribute

- **Report a bug** or **suggest a feature** using the [issue templates](https://github.com/AnnusShahzad/AbsoluteCinema/issues/new/choose).
- **Pick up an issue.** Issues labelled `good first issue` are a good place to start. Leave a comment so others know you are working on it.
- **Improve the docs.** If something in the README was confusing, a pull request that clarifies it is very welcome.

## Local setup

Requirements: Docker, or JDK 17+ with a PostgreSQL database.

```bash
git clone https://github.com/AnnusShahzad/AbsoluteCinema.git
cd AbsoluteCinema
docker compose up --build
# in another terminal, once the app has started:
docker compose exec -T db psql -U cinema -d absolutecinema < scripts/demo-data.sql
```

Open <http://localhost:8080> and sign in as `demo`, `employee` or `admin` (password `demo1234`).

To run the app outside Docker, copy `.env.example` to `.env`, point it at your database and run `./gradlew bootRun`.

## Project layout

| Path | What lives there |
| --- | --- |
| `controller/` | Page controllers and the `/api` REST controllers. Keep them thin. |
| `service/` | Business rules (pricing, seats, loyalty, analytics, accounts). Most logic and most tests belong here. |
| `repository/` | Spring Data JPA repositories. |
| `entity/` | JPA entities. |
| `config/SecurityConfig.java` | Who can reach which URL. Update it, and `SecurityConfigTest`, whenever you add a route. |
| `templates/` | Pebble views, grouped by area. |
| `static/` | CSS, JavaScript and images. |

## Making a change

1. Fork the repository and create a branch from `main`, e.g. `fix/seat-map-mobile` or `feat/gift-cards`.
2. Keep the change focused on one thing. Match the style of the surrounding code.
3. Add or update tests for behaviour you change. Run the full build before pushing:
   ```bash
   ./gradlew build
   ```
4. Write commit messages in the imperative mood ("Add seat hold timer", not "Added...").
5. Open a pull request and fill in the template. CI must pass before merging.

## Coding guidelines

- **Prices, seats and points are decided on the server.** The browser only sends what the customer picked.
- **Validate input in the service layer** and throw `IllegalArgumentException` with a message that can be shown to the user.
- **Every state-changing request needs a CSRF token.** Use `apiRequest` in `static/admin/admin.js` or copy the `_csrf` meta tag pattern from existing pages.
- **Don't commit secrets.** Configuration comes from environment variables; see `.env.example`.

## Releases

Maintainers release by adding the version to `CHANGELOG.md` and pushing a tag such as `v1.2.0`. The [release workflow](.github/workflows/release.yml) runs the tests, publishes the Docker image to GitHub Container Registry and creates a GitHub release with the jar attached.

By contributing you agree that your contributions are licensed under the [MIT License](LICENSE), and to follow our [Code of Conduct](CODE_OF_CONDUCT.md).
