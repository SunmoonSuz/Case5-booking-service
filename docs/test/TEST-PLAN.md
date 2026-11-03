# Test Plan — `service-booking`

- **Build/version/commit:** Milestone 2 (Architecture & Test Planning)
- **Environment:** Local / Testcontainers (Docker, PostgreSQL 16, Java 21 LTS, Node.js 20+)
- **Specification version:** `1.0.0` (APPROVED)
- **QA owner:** Independent QA Engineer
- **Date:** `2026-11-03`
- **Full Test Plan:** Полный утвержденный независимый тест-план со всеми детальными программами испытаний зафиксирован в [`docs/qa/TEST-PLAN-001.md`](file:///Users/sunmoonsuz/booking-service-realization/docs/qa/TEST-PLAN-001.md).

## Scope
- Backend: Kotlin/JVM + Spring Boot 3.3.x + PostgreSQL 16 (с `btree_gist` и exclusion constraint).
- REST API: v1 (`/api/v1/auth`, `/api/v1/services`, `/api/v1/masters`, `/api/v1/availability`, `/api/v1/bookings`, `/api/v1/client/*`, `/api/v1/admin/*`, `/api/v1/config`).
- Frontend: Web SPA (React 18/19, TypeScript, Vite, Tailwind CSS, TanStack Query v5).
- Безопасность: Cookie-based sessions (`JSESSIONID`, HttpOnly, SameSite=Lax), хэширование паролей BCrypt (cost factor 12), RBAC (`CLIENT`, `ADMIN`).
- Конкурентность: предотвращение овербукинга при 20 одновременных запросах (`NFR-001`, `AC-003`).
- Таймзоны: ISO-8601 UTC в БД и API, локальное отображение в `Europe/Moscow` (`NFR-005`, `AC-010`).
- Производительность: расчет сетки слотов p95 $\le 300$ мс при 50 параллельных пользователях (`NFR-006`, `AC-011`).
- Адаптивность: корректное отображение на экранах от 360px (`NFR-003`).

## Out of scope
- Native Android мобильное приложение (`OUT_OF_SCOPE` per `PROJECT-PROFILE.md`, `DEC-001`, `NFR-002`).
- Native iOS мобильное приложение.
- Онлайн-эквайринг, депозиты, фискализация (`DEC-008`).
- Интеграция с внешними CRM и складскими программами (`DEC-008`).
- Облачный CI/CD pipeline и multi-tenant кластер (`ASM-001`).

## Test strategy

| Level | What is tested | Tool | Why appropriate |
|---|---|---|---|
| Unit | Доменные правила полуинтервалов $[T_{start}, T_{end})$, валидации паролей, отмены до $T_{start}$, расчет слотов 30/60 мин. | JUnit 5, MockK, AssertJ | Быстрая верификация бизнес-правил без поднятия окружения. |
| Integration | Миграции Flyway, GiST exclusion constraint (`btree_gist`), JPA/JDBC репозитории, транзакции. | Testcontainers (PostgreSQL 16), Spring Data JPA | Гарантия инварианта овербукинга на уровне ядра СУБД. |
| API / Security | REST API контракты, RBAC (`GUEST`, `CLIENT`, `ADMIN`), сессии `JSESSIONID`, формат ошибок ADR-006. | `@SpringBootTest`, `MockMvc` / `TestRestTemplate` | Проверка авторизации и форматов ошибок без утечки SQL. |
| Concurrency | 20 параллельных запросов на один слот с дельтой $\le 50$ мс через `CyclicBarrier`. | `CyclicBarrier`, Java 21 Virtual Threads, Testcontainers | Прямая верификация `AC-003` / `NFR-001` (1x201, 19x409). |
| Timezone | Генерация сетки слотов, обеденные перерывы, отсечение прошедших слотов, конвертация в UTC. | JUnit 5, `Clock.fixed()`, `ZoneId` | Верификация `AC-010` и правила `BR-006`. |
| Performance | Бенчмарк генерации слотов (50 VU, 10 мастеров, 100 записей, 7 дней). | k6 / Gatling / Concurrency Benchmark Harness | Верификация критерия `AC-011` (p95 $\le 300$ мс). |
| UI Component & E2E | Реакция на 409 (Toast + автообновление без reload), адаптивность от 360px, отмена в ЛК, админка. | Vitest, React Testing Library, Playwright | Проверка интерфейса и сквозных пользовательских путей. |

## Traceability Summary

| REQ | AC | Test case IDs | Coverage status |
|---|---|---|---|
| `REQ-001`, `REQ-002`, `NFR-004` | `AC-001` | `TC-E2E-001` | PLANNED (`NOT RUN`) |
| `REQ-003`, `REQ-004`, `REQ-005` | `AC-002` | `TC-E2E-002` | PLANNED (`NOT RUN`) |
| `REQ-006`, `REQ-007`, `NFR-001` | `AC-003` | `TC-CONC-001` | PLANNED (`NOT RUN`) |
| `REQ-007`, `NFR-003` | `AC-004` | `TC-UI-001` | PLANNED (`NOT RUN`) |
| `REQ-008`, `REQ-009`, `BR-003` | `AC-005` | `TC-E2E-003` | PLANNED (`NOT RUN`) |
| `REQ-010`..`REQ-012` | `AC-006` | `TC-E2E-004` | PLANNED (`NOT RUN`) |
| `REQ-013`, `BR-002`, `BR-003` | `AC-007` | `TC-E2E-005` | PLANNED (`NOT RUN`) |
| `NFR-004` | `AC-008` | `TC-SEC-001` | PLANNED (`NOT RUN`) |
| `REQ-014` | `AC-009` | `TC-E2E-006` | PLANNED (`NOT RUN`) |
| `NFR-005` | `AC-010` | `TC-TIME-001` | PLANNED (`NOT RUN`) |
| `NFR-006` | `AC-011` | `TC-PERF-001` | PLANNED (`NOT RUN`) |
| `BR-001` | `AC-EDGE-001` | `TC-EDGE-001` | PLANNED (`NOT RUN`) |
| `BR-001` | `AC-EDGE-002` | `TC-EDGE-002` | PLANNED (`NOT RUN`) |
| `BR-005` | `AC-EDGE-003` | `TC-EDGE-003` | PLANNED (`NOT RUN`) |
| `BR-005` | `AC-EDGE-004` | `TC-EDGE-004` | PLANNED (`NOT RUN`) |
| `BR-004` | `AC-EDGE-005` | `TC-EDGE-005` | PLANNED (`NOT RUN`) |
| `REQ-004`, `REQ-012` | `AC-EDGE-006` | `TC-EDGE-006` | PLANNED (`NOT RUN`) |
| `REQ-013`, `REQ-014` | `AC-EDGE-007` | `TC-EDGE-007` | PLANNED (`NOT RUN`) |
| `BR-006` | `AC-EDGE-008` | `TC-EDGE-008` | PLANNED (`NOT RUN`) |
| `REQ-012` | `AC-EDGE-009` | `TC-EDGE-009` | PLANNED (`NOT RUN`) |

## Risks and constraints
1. **Пул соединений базы данных при конкурентном тесте:** Для выполнения теста `TC-CONC-001` (20 потоков) пул HikariCP должен иметь размер не менее 30 соединений во избежание ложных отказов по ConnectionTimeout.
2. **Зависимость от Testcontainers и Docker:** Требуется наличие активного Docker-демона для прогона интеграционных и конкурентных тестов PostgreSQL 16.
3. **Строгая политика Zero Fake Tests:** Все тесты до момента физического выполнения имеют статус `NOT RUN`. Запрещены пустые тесты и безусловные skip.
