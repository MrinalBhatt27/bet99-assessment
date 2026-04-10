# bet99 Bug Tracker

[![CI](https://github.com/MrinalBhatt27/bet99-assessment/actions/workflows/ci.yml/badge.svg)](https://github.com/MrinalBhatt27/bet99-assessment/actions/workflows/ci.yml)

A simple web application that lets users log and view bugs.  
Built with **Spring MVC 5**, **Hibernate 5**, **MySQL 8**, **JSP** and **jQuery**.

---

## Quick start (Docker — recommended)

The only things you need installed are **Docker** and **Docker Compose**.  
Java, Maven and MySQL are all provided inside the containers.

### Linux / Ubuntu — one-time Docker permission setup

On Linux, Docker runs as root by default. Run this **once** so you can use Docker without `sudo`, then open a new terminal (or log out and back in):

```bash
sudo usermod -aG docker $USER
newgrp docker          # applies the group change in the current shell immediately
```

> If you prefer not to change your user groups, prefix every `docker` command below with `sudo`.

### Start the app

```bash
# 1. Clone the repo
git clone https://github.com/MrinalBhatt27/bet99-assessment.git
cd bet99-assessment

# 2. Create your local env file  ← REQUIRED, do not skip
cp .env.example .env

# 3. Build and start everything (first run downloads images and compiles — allow ~2 min)
docker compose up
```

> **`.env` is required.** Docker Compose reads database credentials from it.  
> Running `docker compose up` without this file leaves all variables blank and MySQL will fail to start.

Wait until you see:

```
bet99-app  | [INFO] Started Jetty Server
```

Then open **http://localhost:8080/** in a browser.

> **Port conflicts?** Change `APP_PORT` or `MYSQL_PORT` in your `.env` file before running.

### Useful Docker commands

| Task | Command |
|------|---------|
| Start in background | `docker compose up -d` |
| View app logs | `docker compose logs -f app` |
| Stop everything | `docker compose down` |
| Stop + wipe database | `docker compose down -v` |
| Rebuild after code changes | `docker compose up --build` |

---

## Local development (without Docker for the app)

Use this when you want fast iteration — edit code, Jetty reloads, no Docker rebuild needed.

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 8+ (Java 21 is fine) |
| Maven | 3.8+ |
| Docker | for MySQL only |

**Install on Ubuntu:**
```bash
# Java 21
sudo apt update && sudo apt install -y openjdk-21-jdk

# Maven
sudo apt install -y maven

# Verify
java -version
mvn -version
```

### Steps

```bash
# 1. Create env file and start only the database
cp .env.example .env
docker compose up -d mysql

# 2. Run the app locally
mvn jetty:run
```

Open **http://localhost:8080/**.

Override the port if 8080 is in use:

**Windows PowerShell**
```powershell
mvn --% -Djetty.port=8081 jetty:run
```

**macOS / Linux**
```bash
mvn -Djetty.port=8081 jetty:run
```

### Run the tests

Tests are pure unit tests (JUnit 4 + Mockito) — no database or server required.

```bash
mvn test
```

---

## Running the client and server on different machines

The browser makes AJAX calls to the API. By default those calls go to the same host that
served the page. If you run the **JSP client** on one VM and the **API server** on another,
tell the client where the API lives by setting `API_BASE_URL`.

**With Docker (set in `.env` on the client VM):**
```env
API_BASE_URL=http://<api-server-ip>:8080
```
Then `docker compose up` as normal.

**Without Docker (environment variable before `mvn jetty:run`):**

Windows PowerShell
```powershell
$env:API_BASE_URL="http://<api-server-ip>:8080"
mvn jetty:run
```

macOS / Linux
```bash
API_BASE_URL="http://<api-server-ip>:8080" mvn jetty:run
```

The API server has CORS enabled for all origins on `/api/**`, so cross-origin requests
from the browser work without any extra configuration on the server side.

---

## REST API

All endpoints are under `/api/v1/bugs`.  
Validation errors return `400 Bad Request` with `{"message": "..."}`.  
Unknown IDs return `404 Not Found` with `{"message": "Bug not found: id=N"}`.

Valid `severity` values: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`  
Valid `status` values: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`

### Create a bug

```
POST /api/v1/bugs
Content-Type: application/json
```

```json
{
  "bugTitle": "Login button not working",
  "description": "Clicking login does nothing on Firefox",
  "severity": "HIGH",
  "status": "OPEN"
}
```

Returns `200 OK` with the created bug.

### List / filter bugs

```
GET /api/v1/bugs                            ← all bugs
GET /api/v1/bugs?severity=HIGH              ← filter by severity
GET /api/v1/bugs?status=OPEN                ← filter by status
GET /api/v1/bugs?severity=HIGH&status=OPEN  ← combined server-side filter

# Search by title / description is applied client-side after the API response
```

Returns `200 OK` with a JSON array.

### Update all fields of a bug

```
PUT /api/v1/bugs/{id}
Content-Type: application/json
```

```json
{
  "bugTitle": "Updated title",
  "description": "Updated description",
  "severity": "CRITICAL",
  "status": "IN_PROGRESS"
}
```

Returns `200 OK` with the updated bug.

### Update status only

```
PATCH /api/v1/bugs/{id}/status
Content-Type: application/json
```

```json
{ "status": "RESOLVED" }
```

Returns `200 OK` with the updated bug.

### Delete a bug

```
DELETE /api/v1/bugs/{id}
```

Returns `204 No Content`.

---

## Architecture

### Backend layers

```
controller/   ← Spring MVC @RestController + @Controller (page)
service/      ← Business logic (@Service, @Transactional)
repository/   ← Data access via Hibernate Session (@Repository)
model/        ← JPA @Entity (Bug), enums (Severity, BugStatus)
dto/          ← CreateBugRequest (input), BugResponse (output)
```

**Request flow:**  
Browser → `DispatcherServlet` → `BugApiController` → `BugServiceImpl` → `HibernateBugRepository` → MySQL

### Spring context split

The application uses the standard two-context Spring MVC setup:

- **Root context** (`root-context.xml`) — DataSource, SessionFactory, TransactionManager, services, repositories.
- **Servlet context** (`servlet-context.xml`) — Controllers, MVC configuration, view resolver, CORS.

### Frontend

`bugs.jsp` is the only page. It contains the submit form and the bugs table.  
All interaction is handled by `assets/app.js` using jQuery AJAX — no page reloads.

- **Submit** — `POST /api/v1/bugs`; on success the form clears and the table refreshes.
- **Edit** — Clicking the ✏ button pre-fills the form; Submit calls `PUT /api/v1/bugs/{id}`.
- **Inline status** — The Status column is a live dropdown; changing it calls `PATCH /api/v1/bugs/{id}/status`.
- **Delete** — Clicking 🗑 shows a confirm dialog then calls `DELETE /api/v1/bugs/{id}`.
- **Filters** — Severity and Status dropdowns call `GET /api/v1/bugs?severity=X&status=Y` and re-render the table.
- **Search box** — The free-text search input filters the already-loaded rows client-side (debounced, 250 ms) against both `bugTitle` and `description`. It composes with the server-side filters: e.g. you can narrow to `severity=HIGH` via the dropdown and then search within those results.
- `window.API_BASE_URL` is injected by `PageController` from the `api.baseUrl` property
  (set via the `API_BASE_URL` environment variable). Every AJAX call goes through `apiUrl()` so
  no URL is ever hardcoded to `localhost`.

### Database schema

```sql
CREATE TABLE bugs (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  bug_title   VARCHAR(255) NOT NULL,
  description TEXT         NOT NULL,
  severity    VARCHAR(32)  NOT NULL,
  status      VARCHAR(32)  NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
```

Hibernate is configured with `hbm2ddl.auto=update`, so schema changes to the entity are
applied automatically on startup during development.

---

## Project structure

```
├── Dockerfile                          # Builds and runs the app in a container
├── docker-compose.yml                  # Orchestrates MySQL + app containers
├── .dockerignore
├── .env.example                        # Copy to .env before first run
├── .editorconfig                       # Consistent coding style across editors
├── pom.xml                             # Maven build, dependencies, Jetty plugin
├── CHANGELOG.md                        # Full history of changes
├── .github/workflows/ci.yml            # GitHub Actions CI (build + test on Java 8 & 21)
├── docker/
│   └── mysql/init/01_schema.sql        # DB init (runs once on first container start)
└── src/
    ├── main/
    │   ├── java/com/bet99/bugtracker/
    │   │   ├── controller/             # BugApiController, PageController, GlobalExceptionHandler
    │   │   ├── filter/                 # RequestLoggingFilter (MDC request-ID per request)
    │   │   ├── service/                # BugService interface + BugServiceImpl
    │   │   ├── repository/             # BugRepository interface + HibernateBugRepository
    │   │   ├── model/                  # Bug entity, Severity enum, BugStatus enum
    │   │   ├── dto/                    # CreateBugRequest, UpdateBugRequest, UpdateStatusRequest, BugResponse
    │   │   └── exception/              # BugNotFoundException
    │   ├── resources/
    │   │   ├── application.properties
    │   │   └── logback.xml             # Logback config with MDC pattern
    │   └── webapp/
    │       ├── assets/                 # app.js, style.css
    │       └── WEB-INF/
    │           ├── jsp/bugs.jsp
    │           └── spring/             # root-context.xml, servlet-context.xml
    └── test/
        └── java/com/bet99/bugtracker/
            ├── controller/
            │   └── BugApiControllerTest.java  # 22 MockMvc tests for all endpoints
            └── service/
                └── BugServiceImplTest.java    # 12 Mockito unit tests for service layer
```
