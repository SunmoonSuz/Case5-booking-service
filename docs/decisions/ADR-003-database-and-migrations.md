# ADR-003: Выбор СУБД и инструмента миграций

- Status: `ACCEPTED`
- Date: `2026-11-03`
- Decision owner: `Lead Backend Engineer / Database Architect`

## Context

Сервис онлайн-записи на услуги требует реляционной базы данных с поддержкой строгих транзакций ACID и возможностью обеспечения целостности данных при конкурентных запросах.
В `docs/spec/IMPLEMENTATION-BRIEF.md` (раздел 2.1, 5.3) и handoff рекомендована СУБД **PostgreSQL**, в частности из-за наличия мощного механизма исключающих ограничений (GiST exclusion constraints с диапазонами `tstzrange`), гарантирующих отсутствие овербукинга на уровне ядра базы данных.
Также требуется версионное управление схемой БД для предсказуемого применения миграций на локальных стендах и в тестах (`ASM-007`).

## Decision

Рекомендуется:
1. **СУБД:** **PostgreSQL 16+**.
   - Активация расширения `btree_gist` для поддержки составных ограничений с проверкой равенства скалярных полей (`master_id`) и пересечения временных диапазонов.
   - Использование типа данных `TIMESTAMPTZ` для всех временных меток с хранением строго в UTC (`NFR-005`).
2. **Инструмент миграций схемы:** **Flyway**.
   - Версионные SQL-скрипты в `src/main/resources/db/migration/` вида `V1__init_schema.sql`, `V2__seed_admin.sql`.
   - Прозрачный, явный SQL без скрытой магии генерации схемы через ORM (`hibernate.ddl-auto=validate`).
3. **Пул соединений:** **HikariCP** (встроен в Spring Boot).
   - Настройка `spring.datasource.hikari.maximum-pool-size = 30` и `connection-timeout = 20000` для обеспечения бесперебойного выполнения конкурентного стресс-теста на 20 параллельных потоков (`NFR-001`, `AC-003`) без риска отказа по пулу соединений.
4. **Тестовое окружение:** **Testcontainers PostgreSQL** для интеграционных и нагрузочных проверок (`AC-003`, `AC-010`, `AC-011`).

## Alternatives considered

| Option | Benefits | Costs/Risks |
|---|---|---|
| **Вариант 1: PostgreSQL 16+ + Flyway (Рекомендуется)** | 1. Нативная поддержка диапазонных типов (`tstzrange`) и GiST exclusion constraints (`ADR-004`).<br>2. 100% совместимость с рекомендацией Product Owner в brief.<br>3. Flyway прост, надежен, прозрачен и широко поддерживается Spring Boot.<br>4. Testcontainers обеспечивает полное соответствие dev-окружения и тестов. | Требует установки PostgreSQL или Docker локально (уже установлен в системе). |
| **Вариант 2: PostgreSQL + Liquibase** | 1. Поддержка XML/YAML/JSON форматов и отката миграций (rollback). | Избыточная сложность DSL для MVP; SQL-миграции Flyway гораздо нагляднее для ревью и работы с PostgreSQL-специфичными типами (`EXCLUDE USING gist`). |
| **Вариант 3: H2 Database / SQLite** | 1. Работа в оперативной памяти без внешних демонов. | Отсутствие диапазонных типов `tstzrange` и GiST exclusion constraints делает невозможным нативную проверку `AC-003` и `AC-EDGE-002` на уровне СУБД. |

## Consequences

- Все разработчики используют PostgreSQL (локально или через docker-compose).
- Структура таблиц определяется исключительно SQL-файлами миграций Flyway.
- Сущности JPA помечаются аннотациями для маппинга, но схема БД никогда не генерируется автоматически Hibernate.

## Requirements affected

- `REQ-006`, `NFR-001`, `NFR-005`, `ASM-007`, `AC-003`, `AC-010`.

## Review date / conditions to revisit

- До старта Milestone 2 (Backend Foundation).
