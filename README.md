# Findly / ProductScout

Monorepo for the Findly product analysis workflow.

## Prerequisites

- Java 25
- Maven 3.9.9 or newer
- Node.js 20 LTS or newer
- npm 10 or newer
- Docker with a running daemon (required for PostgreSQL integration tests)

## Local development

```bash
# Backend quality gates
./mvnw -B clean verify

# Frontend setup and checks
cd frontend
npm ci
npm run lint -- --max-warnings=0
npm run typecheck
npm run build
```

The backend uses Spring Boot 4.1.1 and the frontend uses Next.js 16.3.5, React 19 and TypeScript 5.

## Development with Docker Compose

```bash
cp .env.example .env
docker compose up --build --wait
```

The frontend is available at http://localhost:3000 and the backend health check at
http://localhost:8080/actuator/health. Compose runs the development servers.
`POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` configure both the database
and the backend connection. Changing these values does not reconfigure an existing
PostgreSQL data volume.

`NEXT_PUBLIC_API_URL` must be reachable from the browser; the Compose hostname
`backend` is only reachable inside Docker. For a separately started frontend, set
this variable in `frontend/.env.local`. Next.js embeds public variables at build
time, so production builds need the intended URL when running `npm run build`.

## Database migrations

Flyway owns the schema; Hibernate only validates it. Add a new versioned migration
for schema changes instead of editing an applied migration. V2 adds the processing
metadata already used by the analysis entity and preserves existing analyses.

`./mvnw -B clean verify` runs PostgreSQL Testcontainers tests for a fresh schema,
the V1-to-current upgrade, persisted processing metadata, and optimistic locking.
No locally installed PostgreSQL is needed. Testcontainers 1.21.4 includes the Docker
API compatibility fix described in its
[release notes](https://github.com/testcontainers/testcontainers-java/releases/tag/1.21.4).

## Analysis API contract

`POST /api/analyses` accepts a JSON object with `url` and returns HTTP 202.
URLs are limited to 2048 characters, HTTPS, the exact hosts `kleinanzeigen.de`
and `www.kleinanzeigen.de`, and ports 80/443 (or no explicit port). Userinfo and
fragments are rejected. Validation happens before persistence and scheduling;
this syntactic check does not replace DNS and connection-level SSRF protection.

`GET /api/analyses/{id}` returns status and progress. Failed analyses expose a
stable error code and a public message, never the stored exception text.
`GET /api/analyses/{id}/result` returns the result only for completed analyses.

| HTTP status | Code | Meaning |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` / `INVALID_URL` | Invalid body, identifier, or listing URL |
| 404 | `ANALYSIS_NOT_FOUND` | No analysis with this ID |
| 409 | `RESULT_NOT_READY` | Analysis still pending or processing |
| 422 | `ANALYSIS_FAILED` | Analysis failed without a more specific public code |
| 429 | `RATE_LIMIT_EXCEEDED` | Request budget exhausted; observe `Retry-After` |
| 502 | `LISTING_FETCH_FAILED` | Analysis has a stored listing-provider failure |
| 504 | `ANALYSIS_TIMEOUT` | Analysis has a stored timeout failure |
| 500 | `INTERNAL_ERROR` | Unexpected failure or completed analysis without a result |

Errors use `application/problem+json` with `type`, `title`, `status`, `detail`,
`instance`, `code`, `traceId`, and UTC `timestamp`. `X-Trace-ID` matches the body.
Framework errors for unsupported methods, content types, and response formats
retain HTTP 405, 415, and 406. The current processing pipeline still records most
failures as `ANALYSIS_FAILED`; provider-specific classification and enforcement
of analysis timeouts belong to the subsequent client/orchestrator phases.

Rate limits use independent, fixed 60-second windows per remote IP: 20 creates
and 120 combined status/result reads by default. They are per application
instance and reset on restart. `X-Forwarded-For` is not trusted by the limiter.
For multiple instances or deployment behind a proxy, configure a trusted proxy
boundary and shared limiting separately. The store is capped at 10000 active
IP/operation windows; new windows receive 429 while it is full, and expired
windows are reclaimed.

Configure the limits through `ANALYSIS_CREATE_REQUESTS_PER_MINUTE`,
`ANALYSIS_READ_REQUESTS_PER_MINUTE`, and `ANALYSIS_RATE_LIMIT_MAX_CLIENTS`.
`CORS_ALLOWED_ORIGINS` is a comma-separated list of explicit HTTP(S) origins
without paths or wildcards. The default is `http://localhost:3000`. CORS permits
GET/POST/OPTIONS and exposes `X-Trace-ID` and `Retry-After` to the frontend;
preflight requests do not consume the analysis budgets.
