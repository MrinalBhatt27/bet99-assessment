# bet99 assessment (Bug Tracker)

## Prerequisites
- Java 8+ (Java 21 is OK)
- Maven 3.8+
- Docker Desktop (for MySQL)

## MySQL (Docker)
1. Copy `.env.example` to `.env` and adjust values if needed.
2. Start MySQL:

```bash
docker compose up -d
```

3. Wait for it to become healthy:

```bash
docker ps
```

MySQL will be available at `localhost:${MYSQL_PORT}` (default `3306`).

## Build & Run (Server + JSP)
1. Start MySQL using Docker (above).
2. Run the web app:

```bash
mvn jetty:run
```

3. Open:
- `http://localhost:8080/`

If port `8080` is already in use, run Jetty on another port:

Windows PowerShell:

```powershell
mvn --% -Djetty.port=8081 jetty:run
```

macOS/Linux:

```bash
mvn -Djetty.port=8081 jetty:run
```

## API
- `POST /api/bugs` (JSON body)

```json
{
  "bugTitle": "Login button not working",
  "description": "Clicking login does nothing",
  "severity": "HIGH",
  "status": "OPEN"
}
```

- `GET /api/bugs`
- `GET /api/bugs?severity=HIGH`

## Client/Server on different machines (API base URL)
If the JSP is served from a different host than the API, set `API_BASE_URL` when starting the server, for example:

Windows PowerShell:

```powershell
$env:API_BASE_URL="http://<server-ip>:8080"
mvn jetty:run
```

macOS/Linux:

```bash
API_BASE_URL="http://<server-ip>:8080" mvn jetty:run
```

