# note — Kotlin + Spring Boot 4 Notes API

A production-style REST backend for a notes app, built with **Kotlin**, **Spring Boot 4**, and
**MongoDB**. It goes well beyond CRUD — full JWT authentication, email verification, password
reset, rate limiting, role-based admin access, file uploads, i18n, metrics, and an automated
test suite.

## Highlights

- 🔐 **JWT auth** — access + refresh tokens, rotation, instant revocation, BCrypt, role-based access
- 📧 **Email flows** — verification, resend, password reset, change-email (async, pluggable mailer)
- 🚦 **Rate limiting** — token-bucket per IP / per user, proper `429` responses
- 📝 **Notes CRUD** — paginated, with strict per-user ownership checks
- 🖼️ **Avatar upload** — multipart with magic-byte validation + path-traversal protection
- 🌍 **i18n** — English & Arabic via `Accept-Language`
- 📊 **Ops** — OpenAPI/Swagger, Prometheus metrics, health checks, managed TTL indexes
- 🧪 **Tested** — fast unit tests for all services + JWT, plus an embedded-Mongo context test

## Quick start

```bash
# run locally (dev profile)
./gradlew bootRun

# run the tests
./gradlew test

# build a jar
./gradlew bootJar
```

Requires `MONGODB_CONNECTION_STRING` and `JWT_SECRET_BASE64` — see the
[full setup & env vars](docs/README.md#-environment-variables).

## 📚 Documentation

| Doc | What's inside |
|---|---|
| **[docs/README.md](docs/README.md)** | Full feature list, API endpoint reference, profiles, env vars, run commands |
| **[docs/TESTING.md](docs/TESTING.md)** | Testing basics — stack, what to test vs skip, how to run and write tests |
| **[docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md)** | How the codebase is organized, package by package |
| **[docs/FUTURE-FEATURES.md](docs/FUTURE-FEATURES.md)** | Planned features and ideas |

---

## Reference documentation (Spring Boot)

- [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.6/gradle-plugin)
- [Create an OCI image](https://docs.spring.io/spring-boot/4.0.6/gradle-plugin/packaging-oci-image.html)
- [Spring Security](https://docs.spring.io/spring-boot/4.0.6/reference/web/spring-security.html)
- [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
- [Official Gradle documentation](https://docs.gradle.org)
