# Code & Architecture Review Report #001

- Base revision: `a2e3cef`
- Head revision: `working-tree (pre-kickoff architecture)`
- Specification version: `1.0.0 APPROVED`
- QA report: `NOT_RUN (Pre-implementation architectural phase)`
- Reviewer: `strict-code-reviewer` (Independent Senior Architect)
- Date: `2026-11-03`

## Verdict

`APPROVE WITH RECOMMENDATIONS (ALL PATCHES APPLIED)`

*(Первоначальный статус `CHANGES REQUESTED` переведён в `APPROVE` после применения исправлений синтаксиса GiST, пула соединений HikariCP, заполнения обязательных полей bootstrap-администратора, валидации мутаций расписания и добавления эндпоинта конфигурации).*

## Findings and Resolutions

| ID | Severity | File:line | REQ/AC | Evidence / failure scenario | Recommended correction | Status |
|---|---|---|---|---|---|---|
| **F-001** | **CRITICAL** | `docs/decisions/ADR-004.md:38`, `docs/architecture/SYSTEM-ARCHITECTURE.md:240` | `NFR-001`, `REQ-006`, `AC-003` | Синтаксическая ошибка PostgreSQL в DDL: выражение `tstzrange` в `EXCLUDE` не заключено в скобки. | Обернуть выражение в скобки `(tstzrange(...)) WITH &&`. | **RESOLVED** |
| **F-002** | **HIGH** | `docs/decisions/ADR-003.md:22`, `docs/decisions/ADR-004.md:61` | `NFR-001`, `AC-003` | Исчерпание пула соединений HikariCP (default 10) при конкурентном тесте 20 параллельных потоков. | Зафиксировать `maximum-pool-size = 30` и `connection-timeout = 20000`. | **RESOLVED** |
| **F-003** | **HIGH** | `docs/decisions/ADR-009.md:35`, `docs/architecture/SYSTEM-ARCHITECTURE.md:175` | `REQ-001`, `NFR-004`, `ASM-007` | Падение `NOT NULL` при старте приложения: `name` и `phone` в таблице `users` обязательны. | Добавить в bootstrap `name = "Administrator"` и `phone = "+70000000000"`. | **RESOLVED** |
| **F-004** | **MEDIUM** | `docs/decisions/ADR-010.md`, `docs/decisions/ADR-012.md` | `REQ-012`, `SCN-005` | Сокращение графика мастера может оставить существующие активные записи вне рабочих часов. | Добавить проверку и ошибку `400 SCHEDULE_CONFLICTS_WITH_ACTIVE_BOOKINGS`. | **RESOLVED** |
| **F-005** | **MEDIUM** | `docs/decisions/ADR-005.md:31` | `REQ-002`, `AC-008` | Неопределённость CSRF и 302 редирект вместо 401 в REST API по умолчанию Spring Security. | Настроить `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` и SameSite=Lax. | **RESOLVED** |
| **F-006** | **LOW** | `docs/decisions/ADR-007.md:29`, `docs/architecture/SYSTEM-ARCHITECTURE.md:300` | `NFR-005`, `AC-010` | Фронтенду требуется знать IANA-зону и валюту для корректного форматирования. | Добавить эндпоинт `GET /api/v1/config`. | **RESOLVED** |
| **F-007** | **LOW** | `docs/decisions/ADR-006.md:50` | `REQ-006`, `REQ-010` | Неполнота каталога доменных ошибок. | Добавить коды ошибок мутаций каталога и расписания в таблицу ADR-006. | **RESOLVED** |

## Areas reviewed

- Спецификация требований `03-requirements.md`, сценарии `04-scenarios.md`, матрица приёмки `05-acceptance-matrix.md`.
- Архитектурные решения `ADR-001` .. `ADR-012` в `docs/decisions/`.
- Системный архитектурный документ `docs/architecture/SYSTEM-ARCHITECTURE.md`.
- Инвариант защиты от овербукинга (`GiST exclusion constraint` + `saveAndFlush`).
- Сессионная модель безопасности и RBAC.
- Таймзоны и границы суток.

## Areas not reviewed / limitations

- Исходный код приложения (на данном этапе кодовая база отсутствует в соответствии с ограничениями задачи).
- Фактические запуски автотестов (будут выполняться в последующих фазах реализации).

## Required follow-up

- Согласование архитектурного пакета и ключевых ADR с владельцем продукта / пользователем.
- Переход к Milestone 2 (Backend Foundation) после утверждения.
