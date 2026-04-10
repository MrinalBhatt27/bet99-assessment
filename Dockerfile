FROM maven:3.9.6-eclipse-temurin-21-alpine
WORKDIR /app

# ── Pre-fetch all Maven dependencies ──────────────────────────────────────────
# This layer is cached and only re-runs when pom.xml changes,
# so subsequent builds (code-only changes) are fast.
COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins --no-transfer-progress -q

# ── Copy source and expose port ────────────────────────────────────────────────
COPY src ./src
EXPOSE 8080

# ── Start the embedded Jetty server ───────────────────────────────────────────
CMD ["mvn", "jetty:run", "--no-transfer-progress"]
