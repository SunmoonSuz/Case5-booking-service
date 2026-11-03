# CODE REVIEW REPORT #004: Catalog, Masters & Schedule Engine (Stage 4)

- **Base revision:** `Stage 3 (Approved Identity & Access Module)`
- **Head revision:** `Stage 4 working-tree (Catalog, Masters & Schedule Engine)`
- **Specification version:** `1.0.0 APPROVED` (ADR-007, ADR-008, ADR-010, ADR-011, ADR-012, SYSTEM-ARCHITECTURE.md)
- **QA report:** `READY_FOR_EXECUTION` (Unit tests with MockK + Integration tests with Testcontainers PostgreSQL 16)
- **Reviewer:** `strict-code-reviewer` (Independent Principal Engineer)
- **Date:** `2026-11-03`

---

## Verdict

`APPROVE`

Реализация модуля каталога услуг, мастеров и алгоритма расчета доступных слотов (Этап 4) полностью соответствует утверждённой спецификации, функциональным требованиям (`REQ-003`, `REQ-004`, `REQ-010`, `REQ-011`, `REQ-012`), критериям приёмки (`AC-002`, `AC-006`, `AC-010`, `AC-EDGE-006`, `AC-EDGE-008`, `AC-EDGE-009`) и архитектурным инвариантам (`ADR-007`, `ADR-008`, `ADR-010`, `ADR-012`). Открытых дефектов уровня BLOCKER, CRITICAL или MAJOR не обнаружено.

---

## Findings Summary

| ID | Severity | File:line | REQ/AC / ADR | Description | Status |
|---|---|---|---|---|---|
| — | — | — | — | Замечаний уровней BLOCKER, CRITICAL, MAJOR не обнаружено. | **NONE** |
| **O-401** | **NIT** | [`MasterManagementService.kt:173`](file:///Users/sunmoonsuz/booking-service-realization/src/main/kotlin/com/booking/catalog/application/MasterManagementService.kt#L173) | `ADR-010` | Проверка активных бронирований при изменении расписания мастера ограничена горизонтом 60 дней (`now.plusDays(60)`). Для салона с горизонтом бронирования до 30 дней этого достаточно, но для абсолютной надежности рекомендуется убрать верхнюю границу (`endTime > now`). | **NOTED** |
| **O-402** | **NIT** | [`AvailabilityCalculatorService.kt:86`](file:///Users/sunmoonsuz/booking-service-realization/src/main/kotlin/com/booking/schedule/application/AvailabilityCalculatorService.kt#L86) | `KISS` | `currentSlotStart.plus(duration)` на `LocalTime` безопасен в рамках дневных смен (ограничение `start_time < end_time` в БД), но в качестве дополнительной защиты от гипотетического перехода через полночь можно проверять `currentSlotEnd.isAfter(currentSlotStart)`. | **NOTED** |

---

## Детальные результаты аудита по направлениям

### 1. Выполнение требований спецификации и приёмочных критериев
- **`REQ-003`, `AC-002` (Публичный каталог и слоты):**
  - Эндпоинты `GET /api/v1/services` и `GET /api/v1/masters` доступны без аутентификации и возвращают только активные сущности (`isActive = true`).
  - Фильтрация мастеров по услуге `GET /api/v1/masters?serviceId=...` использует оптимизированный JOIN-запрос без проблемы N+1.
- **`REQ-004`, `AC-002`, `ADR-010` (Расчет доступных слотов `GET /api/v1/availability`):**
  - Дискретная сетка: слоты формируются строго с шагом длительности выбранной услуги (30 или 60 минут).
  - Границы смены: цикл `while (!currentSlotStart.plus(duration).isAfter(shift.endTime))` гарантирует, что $Slot_{end} \le Shift_{end}$. Неполные остатки смены (хвосты меньше длительности) отсекаются.
  - Поддержка перерывов: мастер может иметь несколько интервалов в день (например, 10:00–14:00 и 15:00–19:00). Обеденный перерыв автоматически исключается из доступных слотов.
  - Коллизии: формула `startUtc.isBefore(booking.endTime) && endUtc.isAfter(booking.startTime)` математически точна для полуинтервалов $[T_{start}, T_{end})$. Смежные слоты не конфликтуют, частичные перекрытия исключаются.
- **`REQ-010`, `REQ-011`, `REQ-012`, `AC-006` (Администрирование каталога и расписания):**
  - Полный CRUD для услуг (`/api/v1/admin/services/**`), мастеров (`/api/v1/admin/masters/**`) и привязки услуг (`PUT /api/v1/admin/masters/{id}/services`).
  - Настройка расписания (`PUT /api/v1/admin/masters/{id}/schedule`) с атомарной заменой смен.
- **`AC-EDGE-008`, `BR-006` (Отсечение слотов в прошлом):**
  - Условие `val isPast = !startUtc.isAfter(now)` отсекает все слоты $Slot_{start} \le Now_{venue}$.
- **`AC-EDGE-009` (Валидация интервалов смен):**
  - При `endTime <= startTime` выбрасывается `InvalidScheduleIntervalException` с кодом ошибки `INVALID_SCHEDULE_INTERVAL` (HTTP 400).
  - Пересекающиеся смены одного мастера в один день также отклоняются с кодом `INVALID_SCHEDULE_INTERVAL` (HTTP 400).

### 2. Архитектурные инварианты
- **Таймзоны (`NFR-005`, `AC-010`, `ADR-007`):**
  - В БД и DTO `AvailableSlotDto` поля `startTime` и `endTime` передаются как `Instant` в ISO-8601 UTC.
  - Поле `formattedTime` («14:00») форматируется по часовому поясу заведения `Europe/Moscow` (UTC+3). В интеграционном тесте доказано соответствие `14:00 MSK` == `11:00 UTC` (`2026-11-10T11:00:00Z`).
- **Денежные суммы (`ADR-008`):**
  - Используются `BigDecimal` в коде и `NUMERIC(10, 2)` в СУБД. Валидация `@field:DecimalMin("0.00")`.
- **Жизненный цикл каталога (`ADR-012`):**
  - Мягкое удаление (`isActive = false`): сущности скрываются из публичного каталога, но остаются в админке и не ломают внешние ключи исторических бронирований.
  - Guard деактивации мастера: проверка `countFutureActiveByMasterId > 0` блокирует деактивацию и удаление мастера с ошибкой `400 MASTER_HAS_ACTIVE_BOOKINGS`.
  - Guard изменения длительности услуги: при наличии будущих активных записей смена длительности (30 $\leftrightarrow$ 60 мин) блокируется с ошибкой `400 CANNOT_MODIFY_DURATION_WITH_ACTIVE_BOOKINGS`.
  - Guard изменения расписания: если сокращение графика мастера оставляет хотя бы одно активное будущее бронирование вне рабочих часов, операция блокируется с ошибкой `400 SCHEDULE_CONFLICTS_WITH_ACTIVE_BOOKINGS`.

### 3. SOLID, ООП и Kotlin null-safety
- Четкое разделение ответственности: `ServiceCatalogService` (каталог услуг), `MasterManagementService` (мастера и смены), `AvailabilityCalculatorService` (движок слотов).
- Внедрение `Clock` во все сервисы времени обеспечивает 100% детерминированное тестирование без зависимости от системного времени ОС.
- Строгая null-safety: nullable только опциональные поля (`description: String?`), принудительный `.trim()` для строковых данных.

### 4. Качество тестирования
- `AvailabilityCalculatorServiceTest` (MockK + `Clock.fixed`): изолированно проверяет генерацию дискретной сетки 30 и 60 минут, исключение обеда, отсечение прошедших слотов и смежные интервалы.
- `CatalogAndAvailabilityIntegrationTest` (MockMvc + Testcontainers PostgreSQL 16): проверяет полный цикл административного создания каталога, публичной выдачи, расчета доступности, мягкого удаления и защитных guard-правил при наличии бронирований.

---

## Итоговый вердикт

**`APPROVE`** — Модуль каталога и расчета доступности (Этап 4) полностью верифицирован и одобрен. Рекомендуется переходить к Этапу 5 (Ядро бронирования и конкурентный стресс-тест).
