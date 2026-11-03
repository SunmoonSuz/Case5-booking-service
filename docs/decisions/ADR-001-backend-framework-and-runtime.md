# ADR-001: Выбор бэкенд-фреймворка и среды выполнения

- Status: `ACCEPTED`
- Date: `2026-11-03`
- Decision owner: `Lead Backend Engineer / Coordinator`

## Context

Проект «Сервис онлайн-записи на услуги» (`service-booking`) реализуется по профилю `FULL_STACK`. Handoff и Implementation Brief (`docs/spec/IMPLEMENTATION-BRIEF.md`, раздел 2.1) определяют, что серверная часть разрабатывается на языке Kotlin/JVM и оставляет выбор между Spring Boot 3.x и Ktor, а также между Java 17 и Java 21 на усмотрение инженерной команды.

Ключевые требования к бэкенду:
- Строгая транзакционная изоляция и исключение овербукинга при одновременных запросах (`NFR-001`, `REQ-006`, `AC-003`);
- Надежная работа с реляционной базой данных (PostgreSQL) и расширениями (`btree_gist`);
- Разграничение доступа на основе ролей (`CLIENT`, `ADMIN`, `GUEST`) с защитой административных эндпоинтов (`NFR-004`, `REQ-002`, `AC-008`);
- Валидация входных данных, форматирование ошибок и надежное управление сессиями.

## Decision

Рекомендуется выбрать:
1. **Язык и среда выполнения:** **Kotlin 2.0+** на платформе **Java 21 LTS**.
2. **Фреймворк:** **Spring Boot 3.3.x** со следующими стартерами:
   - `spring-boot-starter-web` (REST API, контроллеры, обработка ошибок);
   - `spring-boot-starter-data-jpa` (или `spring-boot-starter-jdbc` с транзакционным менеджером);
   - `spring-boot-starter-security` (RBAC, безопасные сессии, солевое хэширование паролей BCrypt/Argon2id);
   - `spring-boot-starter-validation` (Jakarta Validation / Bean Validation);
   - `flyway-core` + `flyway-database-postgresql` (управление версионными миграциями БД);
   - `jackson-module-kotlin` (десериализация Kotlin data-классов);
   - `spring-boot-starter-test` + `testcontainers-postgresql` + `mockk` (тестирование транзакций и параллелизма).

## Alternatives considered

| Option | Benefits | Costs/Risks |
|---|---|---|
| **Вариант 1: Spring Boot 3.3.x + Java 21 LTS (Рекомендуется)** | 1. Зрелая экосистема транзакций (`@Transactional`), декларативное управление изоляцией.<br>2. Готовая интеграция с Spring Security (сессии, cookie, Argon2/BCrypt, CSRF).<br>3. Отличная поддержка Testcontainers для многопоточных тестов (`AC-003`).<br>4. Virtual Threads (Java 21) из коробки для высококонкурентных I/O операций.<br>5. Минимальный риск архитектурных ошибок при интеграции компонентов. | Чуть больший размер артефакта и время холодного старта по сравнению с микрофреймворками (не критично для серверного web-приложения). |
| **Вариант 2: Ktor + Exposed/Ktorm + Java 21** | 1. Идиоматичный асинхронный корутинный Kotlin-фреймворк.<br>2. Низкое потребление памяти и быстрый старт.<br>3. Легковесная модульная структура. | 1. Необходимость ручной сборки и конфигурации транзакций, пула соединений, механизмов безопасности и валидации.<br>2. Меньшая зрелость решений «из коробки» для RBAC сессий и перехвата DB constraint exceptions.<br>3. Повышенные трудозатраты на инфраструктурный код вместо бизнес-логики. |

## Consequences

- Сборка осуществляется с помощью Gradle (`build.gradle.kts`) с плагинами `org.springframework.boot`, `io.spring.dependency-management`, `org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.plugin.spring`.
- Доступна поддержка виртуальных потоков Java 21 (`spring.threads.virtual.enabled=true`).
- Разработчик получает стандартизированную структуру слоев (Controllers, Services, Repositories, Domain Entities) и готовую интеграцию с PostgreSQL.

## Requirements affected

- `REQ-001`, `REQ-002`, `REQ-006`, `NFR-001`, `NFR-004`, `AC-001`, `AC-003`, `AC-008`.

## Review date / conditions to revisit

- До старта Milestone 2 (Backend Foundation). Если заказчик потребует микросервисную архитектуру или бессерверное развертывание (serverless/graalvm native), выбор может быть пересмотрен.
