# note

Kotlin + Spring Boot 4 backend with MongoDB Atlas.

## Stack

- Kotlin 2.2 · Spring Boot 4 · Java 17
- MongoDB (Atlas) via Spring Data
- JWT (JJWT) for auth
- Bean Validation + i18n (en / ar)

## Profiles

| File | When |
|---|---|
| `application.yaml` | shared base |
| `application-dev.yaml` | local dev (port 8181, DEBUG logs) |
| `application-prod.yaml` | production (port from `$PORT`, file logs, compressed responses) |

Switch via `SPRING_PROFILES_ACTIVE=dev|prod` (defaults to `dev`).

## Env vars

| Name | Required | Notes |
|---|---|---|
| `MONGODB_CONNECTION_STRING` | yes | full Atlas URI |
| `JWT_SECRET_BASE64` | yes | base64-encoded secret |
| `SPRING_PROFILES_ACTIVE` | no | `dev` (default) or `prod` |
| `PORT` | prod only | injected by host (Render / Railway / etc.) |
| `CORS_ALLOWED_ORIGINS` | prod only | comma-separated frontend URLs |

## Features

- **JWT auth** — access + refresh tokens, `jti` denylist for immediate revoke
- **Auth endpoints** — `POST /api/auth/register|login|refresh|logout`
- **Notes CRUD** — `GET|POST|PUT|DELETE /api/notes`, paginated list (`?page=&size=&sort=`)
- **i18n** — `Accept-Language: en | ar` on any response
- **CORS** — configured via `app.cors.allowed-origins` (dev list / prod env var)
- **Actuator** — `GET /actuator/health` (public) + `/actuator/info`
- **Custom Mongo health** — works on Atlas (built-in indicator can't query `local`)
- **TTL indexes** — refresh tokens and revoked tokens auto-expire
- **Index init** — `IndexInitializer` creates indexes idempotently on boot

## Run

```bash
# dev
./gradlew bootRun

# package
./gradlew bootJar

# run prod profile locally
SPRING_PROFILES_ACTIVE=prod java -jar build/libs/*.jar
```

## Project layout

```
auth/         register, login, refresh, logout, JWT, denylist
note/         CRUD + ownership checks
user/         User entity + repo
security/     JwtAuthFilter, 401/403 JSON handlers
common/       ApiResponse, ApiException, GlobalExceptionHandler
config/       Security, CORS, i18n, indexes, custom health
resources/    YAML configs, messages.properties (en + ar), logback
```

## Response envelope

```json
{ "status": true,  "data": { ... } }
{ "status": false, "error": { "code": "UNAUTHORIZED", "message": "..." } }
```
