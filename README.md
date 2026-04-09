# bet99 Bug Tracker

A simple web application that lets users log and view bugs.  
Built with **Spring MVC 5**, **Hibernate 5**, **MySQL 8**, **JSP** and **jQuery**.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 8+ (Java 21 is fine) |
| Maven | 3.8+ |
| Docker Desktop | any recent version |

---

## Getting started

### 1. Configure the database

Copy the example env file and adjust values if needed:

```bash
cp .env.example .env
```

The defaults work out of the box; only change them if port `3306` is already taken on your machine (`MYSQL_PORT`).

### 2. Start MySQL

```bash
docker compose up -d
```

Wait until the container is healthy:

```bash
docker ps
```

MySQL will be reachable at `localhost:${MYSQL_PORT}` (default `3306`).  
The init script at `docker/mysql/init/01_schema.sql` creates the `bugs` table automatically on first start.

### 3. Run the server

```bash
mvn jetty:run
```

Open `http://localhost:8080/` in a browser.

If port `8080` is in use, override it:

**Windows PowerShell**
```powershell
mvn --% -Djetty.port=8081 jetty:run
```

**macOS / Linux**
```bash
mvn -Djetty.port=8081 jetty:run
```

### 4. Run the tests

```bash
mvn test
```

Tests are pure unit tests (JUnit 4 + Mockito) — no database required.

---

## Running the client and server on different machines

The browser makes AJAX calls to the API. By default those calls go to the same host that served the page. If you run the **JSP client** on one VM and the **API server** on another, tell the client where the API lives by setting `API_BASE_URL` when you start the **client** VM:

**Windows PowerShell**
```powershell
$env:API_BASE_URL="http://<api-server-ip>:8080"
mvn jetty:run
```

**macOS / Linux**
```bash
API_BASE_URL="http://<api-server-ip>:8080" mvn jetty:run
```

The API server has CORS enabled for all origins on `/api/**`, so cross-origin requests from the browser work without any extra configuration.

---

## REST API

### Submit a bug

```
POST /api/bugs
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

Valid `severity` values: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`  
Valid `status` values: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`

Returns `200 OK` with the created bug as JSON, or `400 Bad Request` with a `{"message": "..."}` body if validation fails.

### List all bugs

```
GET /api/bugs
```

### Filter by severity

```
GET /api/bugs?severity=HIGH
```

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

- **Submit** — `POST /api/bugs` with JSON body; on success the form is cleared and the table is refreshed.
- **Filter** — Changing the severity dropdown calls `GET /api/bugs?severity=<value>` and re-renders the table.
- `window.API_BASE_URL` is injected into the page by `PageController` from the `api.baseUrl` property (set via the `API_BASE_URL` environment variable).

### Database schema

```sql
CREATE TABLE bugs (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  bug_title   VARCHAR(255) NOT NULL,
  description TEXT         NOT NULL,
  severity    VARCHAR(32)  NOT NULL,
  status      VARCHAR(32)  NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
```

Hibernate is configured with `hbm2ddl.auto=update`, so schema changes to the entity are applied automatically on startup during development.

---

## Project structure

```
├── docker/
│   └── mysql/init/01_schema.sql   # DB init script (runs once on first container start)
├── src/
│   ├── main/
│   │   ├── java/com/bet99/bugtracker/
│   │   │   ├── controller/        # BugApiController, PageController, GlobalExceptionHandler
│   │   │   ├── service/           # BugService interface + BugServiceImpl
│   │   │   ├── repository/        # BugRepository interface + HibernateBugRepository
│   │   │   ├── model/             # Bug entity, Severity enum, BugStatus enum
│   │   │   └── dto/               # CreateBugRequest, BugResponse
│   │   ├── resources/
│   │   │   └── application.properties
│   │   └── webapp/
│   │       ├── assets/            # app.js, style.css
│   │       └── WEB-INF/
│   │           ├── jsp/bugs.jsp
│   │           └── spring/        # root-context.xml, servlet-context.xml
│   └── test/
│       └── java/com/bet99/bugtracker/service/
│           └── BugServiceImplTest.java
├── .env.example
├── docker-compose.yml
└── pom.xml
```
