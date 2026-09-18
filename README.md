# Findly / ProductScout

Monorepo for the Findly product analysis workflow.

## Prerequisites

- Java 25
- Maven 3.9.9 or newer
- Node.js 20 LTS or newer
- npm 10 or newer

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
