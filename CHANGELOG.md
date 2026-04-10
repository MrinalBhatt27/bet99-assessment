# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- Column sorting on all table headers (ID, Title, Severity, Status, Created) with ascending/descending toggle
- Toast notification system — slide-in/out success & error messages replace the static inline span
- Confirmation dialog for delete now shows the bug title ("Delete "Login broken"?")
- Empty-state messages differentiate between "no bugs yet", "no filter match", and "no search match"
- Input character counter on the Title field (e.g. 42 / 255) with a warning colour near the limit
- `updated_at` column on the `bugs` table, populated via `@PrePersist` / `@PreUpdate` JPA callbacks
- `updatedAt` and `self` link (`/api/v1/bugs/{id}`) fields in every `BugResponse`
- API versioned under `/api/v1/` prefix
- HikariCP connection pool (max 10, min 2) replacing `DriverManagerDataSource`
- `RequestLoggingFilter` attaches a short UUID `reqId` to every log line via SLF4J MDC;
  also returned as `X-Request-Id` response header for client-side correlation
- Switched logging implementation from `slf4j-simple` to Logback for MDC support
- `logback.xml` with per-package log levels and MDC pattern
- Catch-all `Exception` handler in `GlobalExceptionHandler` — unexpected errors return
  `500 {"message": "An unexpected error occurred…"}` without leaking stack traces
- `@Size(max = 255)` server-side validation on `bugTitle` in `CreateBugRequest` and `UpdateBugRequest`
- `X-Requested-With` added to CORS `allowed-headers` for cross-VM jQuery AJAX compatibility
- Client-side search box filtering bugs by title and description (debounced, 250 ms)
- Client-side pagination with configurable page size (5 / 10 / 50 / 100)
- Status filter dropdown alongside severity filter
- Full bug edit mode (PUT `/api/v1/bugs/{id}`) — Edit button pre-fills the form
- Inline status update (PATCH `/api/v1/bugs/{id}/status`) via in-table dropdown
- Delete bug (DELETE `/api/v1/bugs/{id}`) with confirmation
- SLF4J logging across controller and service layers (INFO / WARN / DEBUG)
- `GlobalExceptionHandler` for consistent JSON error responses (400, 404, 500)
- `BugNotFoundException` custom unchecked exception mapped to 404
- `@Transactional(readOnly = true)` on all read-only service methods
- GitHub Actions CI pipeline (`build-and-test` on Java 8 & 21, `docker-build` job)
- Docker Compose setup (`app` + `mysql` services) for one-command startup
- Responsive UI with mobile filter layout and table horizontal scrolling

## [1.0.0] – Initial release

### Added
- Spring MVC + Hibernate bug tracker with JSP/jQuery AJAX frontend
- Store `bugTitle`, `description`, `severity`, `status` in MySQL
- Form to submit bugs via AJAX (no page reload)
- Table displaying all bugs
- Severity filter dropdown
- Docker Compose with MySQL initialisation script
- README for newly-hired developer (environment setup, Docker quick-start, API reference)
