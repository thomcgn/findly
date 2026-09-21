# Findly / ProductScout

Monorepo for the Findly product analysis workflow.

## Prerequisites

- Java 25
- Maven 3.9.9 or newer
- Node.js 20.19 or newer (Node 20 is used by the existing container/CI setup)
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
npm test
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
V3 allows unknown listing prices/currencies, V4 stores job URLs and deadlines and
migrates the old processing statuses, V5 adds persisted warnings, and V6 marks
historical completed results as unverified without deleting their audit data.

`./mvnw -B clean verify` runs PostgreSQL Testcontainers tests for a fresh schema,
the V1-to-current upgrade, persisted processing metadata, and optimistic locking.
No locally installed PostgreSQL is needed. Testcontainers 1.21.4 includes the Docker
API compatibility fix described in its
[release notes](https://github.com/testcontainers/testcontainers-java/releases/tag/1.21.4).

## Analysis API contract

`POST /api/analyses` accepts a JSON object with `url`, commits a job in `CREATED`,
and returns HTTP 202 after enqueueing it. Queue saturation returns HTTP 429 with
`ANALYSIS_QUEUE_FULL` and leaves a persisted failed job.
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
retain HTTP 405, 415, and 406. Listing failures retain specific codes for blocked
targets/access, invalid redirects, unsupported content, oversized responses,
and parsing errors. Unexpected processing failures use `ANALYSIS_FAILED` without
persisting internal exception messages.

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

## Listing client and asynchronous jobs

The listing client uses Apache HttpClient's connection-time
[`DnsResolver`](https://hc.apache.org/components/httpcomponents-client-5.5.x/5.5.2/httpclient5/apidocs/org/apache/hc/client5/http/class-use/DnsResolver.html); the
validated address objects are used directly by the socket connector. It rejects
private, loopback, link-local, multicast, unspecified and other non-public
IPv4/IPv6 targets, including mixed DNS answers. TLS certificate and hostname
verification use the client's defaults. Automatic redirects, retries, cookies,
compression and system proxies are not enabled. Each of at most three redirects
is revalidated and uses a new connection/resolution.

Only HTML/XHTML responses are accepted. Reads stop after the configured limit
plus one byte; the connection is aborted instead of draining untrusted content.
Each request also has an absolute deadline equal to connect plus read timeout.
Configure these validated Spring properties:

| Property | Default | Allowed range |
| --- | --- | --- |
| `app.listing.connect-timeout-millis` | 5000 | 1–30000 |
| `app.listing.read-timeout-millis` | 10000 | 1–60000 |
| `app.listing.max-response-bytes` | 10485760 | 1–10485760 |

Jsoup parses concrete listing elements and product JSON-LD/meta data. It does
not search arbitrary description text for money amounts. Absent, conflicting or
invalid price evidence stays `null`; missing prices are not interpreted as free.
CAPTCHA/login/access-block responses are not bypassed.

The workflow is `CREATED → FETCHING_LISTING → EXTRACTING_LISTING →
IDENTIFYING_PRODUCT → RESEARCHING_PRICES → COMPLETED`, with `FAILED` possible
from any nonterminal state. The initial MVP only lists the old coarse states;
these explicit intermediate states implement the refactoring plan's progress
requirements. `AnalysisStatusService` owns transitions in short `REQUIRES_NEW`
transactions. Row locks atomically claim jobs; terminal states cannot be changed
by duplicate workers or late provider replies. Network/provider work occurs
outside these transactions. Status responses include a `warnings` array.

The executor uses core 2/max 4 workers, queue 20 and rejection rather than running
work in the HTTP thread. The persisted deadline is five minutes from creation,
including queue time. A timer cancels overdue tasks; a periodic database sweep
also fails expired jobs after restart. Interrupted jobs are not automatically
resumed after a restart, and fail when their original deadline expires. Deadlines
are persisted even if cancellation cannot interrupt an OS-level DNS lookup.

Tests use local HTTP fixtures and PostgreSQL Testcontainers, never live listing
websites. The adapter is tested for bounded reads, timeouts and direct use of its
resolver's addresses; workflow tests cover early HTTP 202, transaction boundaries,
claims, queue rejection, deadlines and terminal-state protection.

## Provider availability and partial results

OCR, vision, product search and pricing have separate provider interfaces using
immutable domain DTOs instead of JPA entities. Configure `app.providers.ocr`,
`app.providers.vision`, `app.providers.search`, and `app.providers.pricing` through
Spring configuration. The supported modes are `DISABLED` (default) and
`LOCAL_STUB`; both return `UNAVAILABLE` with no business results. Unknown modes
fail startup. No API keys or external provider accounts are needed to start.

Without providers, a successfully fetched listing completes with a partial
result: `product` is null, market/deal values are null, and persisted warnings
include `OCR_UNAVAILABLE`, `VISION_UNAVAILABLE`, `SEARCH_UNAVAILABLE`,
`PRODUCT_NOT_IDENTIFIED`, and `PRICE_NOT_VERIFIED`. `COMPLETED` means processing
has finished; it does not assert a verified product or market price. Fetch and
parser failures still produce `FAILED`. Pricing is not called without a selected
product. Search hits are selected only when the evidence-based matcher passes
its confidence, identifier and runner-up checks.

Hardcoded model recognition, confidence values and synthetic market-price bands
have been removed from the backend processing and result paths. Historical
completed records are marked `LEGACY_RESULT_UNVERIFIED`; their legacy prices and
product claims are not returned as evidence. Provider quote DTOs require amount,
ISO currency, source name/URL, retrieval time, price kind and valid confidence;
accepted evidence and matching decisions are persisted by phase 7.

The frontend now uses phase 8's polling/result contract and explicit unknown
values; synthetic decision and transport calculations have been removed.
Live provider adapters are not implemented yet; the default configuration still
returns partial results without product or price claims.

## Candidate matching and price evidence (phase 7)

Migration V7 adds `product`, `extracted_attribute`, `product_match` and
`price_evidence`. Typed immutable evidence snapshots use PostgreSQL JSONB to
preserve identifiers, source URLs, retrieval times and component scores. Matches
also store their applied weights and rank. A partial unique index allows at most
one selected match per analysis. Historical synthetic entities remain separate.

Matching compares structured attributes from OCR/vision with corroborated search
candidate attributes. Attribute keys are `ean`, `sku`, `model`, `brand` and
`category`. Values use Unicode NFKC, case and whitespace normalization; SKUs
additionally require literal punctuation agreement. EAN-8/EAN-13 require valid
check digits. Missing features are excluded from the weight denominator;
contradictions score zero. Conflicting identifiers or brands prevent selection.
An exact model, valid EAN, or SKU with matching brand is required; brand/category
alone cannot select a product. Conflicting observations remain uncertain.
Confidence is the weighted evidence agreement, not a calibrated probability.

Spring properties under `app.matching`:

| Property | Default | Constraint |
| --- | --- | --- |
| `minimum-confidence` | `0.75` | 0.75–1 |
| `minimum-margin` | `0.10` | 0.10–1 |
| `ean-weight` | `5` | positive |
| `sku-weight` | `4` | positive |
| `model-weight` | `3` | positive |
| `brand-weight` | `1` | positive |
| `category-weight` | `1` | positive |

Threshold and runner-up margin are inclusive. Ties produce candidates without a
selected product. No brand/model catalog is inferred from keywords or image URLs.

Pricing adapters must supply an explicit `sourceType`: `MANUFACTURER`, `RETAILER`,
`MARKETPLACE` or `OTHER`. `OTHER` is not accepted as verified evidence. Acceptance
requires a positive amount (up to 18 digits, 4 decimal places), ISO currency,
HTTPS source, retrieval time within the last 30 days and not in the future, price
kind and confidence >= 0.75. The retrieval window also applies to historical
prices: the historical record must have been retrieved recently. Rejected quotes
produce `PRICE_EVIDENCE_REJECTED`; no accepted quotes produces `PRICE_NOT_VERIFIED`.
Source classification and confidence must come from actual adapter evidence.

Original/historical references prioritize manufacturer, retailer, marketplace;
current-used references prioritize marketplace, retailer, manufacturer. Only the
best available tier within the same kind and currency contributes to its median.
The result exposes `candidates`, `attributes`, `priceEvidence` and `comparisons`.
Each comparison includes kind, currency, reference price and its contributing
sources. Savings are reference minus listing price; negative values remain
negative and have outcome `SURCHARGE`. Missing listing prices leave savings null.
Other currencies remain visible as evidence but never enter that comparison.
`market` contains only compatible current-used values; legacy `deal` fields stay
null until the decision module is implemented. The frontend displays the separate
comparisons and evidence from this contract.

## Asynchronous frontend (phase 8)

The form starts an analysis using only the listing URL, then navigates to
`/results/{id}`. This URL supports direct visits and reloads. Status requests are
sequential: the first runs immediately, followed by 1/2/5-second waits (then 5
seconds). `COMPLETED` triggers `/result`; `FAILED` stops polling and displays the
stable error code as a localized message. A 429 read respects `Retry-After`, and
a 409 result returns to polling. Creating an analysis is never retried automatically.
Page changes/unmount cancel timers and requests through `AbortController`;
late replies cannot overwrite another analysis. Read errors offer a manual reload
of the existing ID without creating another job.

`frontend/src/types/analysis.ts` is the single contract module: Zod schemas
validate responses at runtime and derive the TypeScript types. Invalid/missing
values cause a response error, rather than conversion to zero. Problem Details
are parsed structurally; raw HTML, server exception messages and unknown error
text are never displayed. Trace IDs remain available for troubleshooting.

Results distinguish unknown prices, unresolved products, candidates, scores,
identifier conflicts, extraction evidence and price sources. Original, historical
original and current-used comparisons remain separate. Signed savings and
`SURCHARGE` come directly from the backend. Source links allow HTTPS only, include
retrieval times, and open without opener access. Candidate confidence below 75%
is explicitly marked uncertain. Transport and personal buying recommendations
are shown as not yet available; mock defaults and the former decision DTOs have
been removed.

`npm test` runs Vitest/React Testing Library tests with controlled fetch fixtures
and clocks, including navigation, polling intervals, terminal states, throttling,
abort/late replies, duplicate starts, error parsing, nulls and signed comparisons.
Frontend CI now runs these tests alongside lint, TypeScript and the production
build. No live listings or provider services are used in these tests.

Phase 8 verification: 19 tests, ESLint and TypeScript pass locally and in the
Node-20 container. The container production build succeeds; the production server
serves `/` and `/results/{id}` with HTTP 200. The local Turbopack build remains
blocked by this environment's port-binding restriction. Dependency installation
still reports the existing ESLint 9 deprecation and, locally, the unrs-resolver
install-script policy warning. The complete Compose stack and backend were not
retested for this frontend-only phase.
