# CODE REVIEW REPORT #005 (FINAL): Booking Core & Overbooking Prevention (Stage 5)

- **Base revision:** `Stage 4 (Approved Catalog, Masters & Schedule Engine)`
- **Head revision:** `Stage 5 working-tree (Booking Core & Overbooking Prevention with fixes F-501, F-502, O-501)`
- **Specification version:** `1.0.0 APPROVED` (REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-013, REQ-014; AC-003, AC-005, AC-007, AC-009, AC-EDGE-001, AC-EDGE-002, AC-EDGE-005, AC-EDGE-007, NFR-001; ADR-004, ADR-007, ADR-011, ADR-012)
- **Reviewer:** `strict-code-reviewer` (Independent Principal Engineer)
- **Date:** `2026-11-03`

---

## Verdict

`APPROVE`

Все ранее выявленные замечания (`F-501`, `F-502`, `O-501`) полностью и качественно устранены разработчиком.
Инвариант предотвращения овербукинга (`ADR-004`, `AC-003`, `NFR-001`) доказан конкурентным стресс-тестом на 20 параллельных потоков.
Эндпоинты чтения клиентских и административных бронирований покрыты исчерпывающими интеграционными тестами с проверкой сортировок, фильтрации и защиты от N+1 через `@EntityGraph`.
Открытых дефектов уровня BLOCKER, CRITICAL или MAJOR не осталось. Этап 5 одобрен к приёмке.

---

## Статус устранения замечаний

| ID | Severity | File:line | REQ/AC / ADR | Описание и проверка решения | Статус |
|---|---|---|---|---|---|
| **F-501** | **MAJOR** | [`BookingService.kt:72-77`](file:///Users/sunmoonsuz/booking-service-realization/src/main/kotlin/com/booking/booking/application/BookingService.kt#L72-L77) | `BR-001`, `AC-EDGE-006` | Добавлена проверка `startVenue.toLocalDate() == endVenue.toLocalDate() && startVenue.toLocalTime().isBefore(endVenue.toLocalTime())`. Добавлен регрессионный тест в `BookingLifecycleIntegrationTest.kt` на отказ в бронировании 23:30–00:30 с кодом 400 `OUTSIDE_WORKING_HOURS`. | **RESOLVED** |
| **F-502** | **MAJOR** | [`BookingLifecycleIntegrationTest.kt:356-526`](file:///Users/sunmoonsuz/booking-service-realization/src/test/kotlin/com/booking/booking/presentation/BookingLifecycleIntegrationTest.kt#L356-L526) | `REQ-006`, `REQ-013`, `AC-007`, `AC-009` | Добавлены 2 интеграционных теста: 1) `GET /api/v1/client/bookings` с верификацией разделения `active` (сортировка ASC по времени визита) и `archive` (сортировка DESC), а также изоляции между клиентами; 2) `GET /api/v1/admin/bookings` с проверкой работы динамического JPQL-запроса без параметров, с фильтрами по статусу, мастеру, дате, комбинации фильтров и проверкой 403 Forbidden для клиентов. | **RESOLVED** |
| **O-501** | **MINOR** | [`BookingRepository.kt:17, 72`](file:///Users/sunmoonsuz/booking-service-realization/src/main/kotlin/com/booking/booking/infrastructure/BookingRepository.kt#L17) | Performance, Clean Arch | В `BookingRepository.kt` для методов `findAllByClientIdOrderByStartTimeDesc` и `findAdminBookings` добавлена аннотация `@EntityGraph(attributePaths = ["client", "master", "service"])`, предотвращающая N+1 lazy loading при маппинге в `BookingResponse`. | **RESOLVED** |

---

## Детальные результаты аудита по направлениям

### 1. Архитектурный инвариант защиты от овербукинга (`ADR-004`, `REQ-006`, `AC-003`, `NFR-001`)
- **База данных:** В `V1__init_schema.sql` действует PostgreSQL GiST exclusion constraint `no_overlapping_active_bookings` на диапазоны `(tstzrange(start_time, end_time, '[)')) WITH &&` для статусов `PENDING` и `CONFIRMED`.
- **Уровень сервиса:** Метод `BookingService.createBooking` выполняет `bookingRepository.saveAndFlush(booking)`, что форсирует оценку констрейнта транзакцией внутри метода до коммита.
- **Маппинг ошибок:** `GlobalExceptionHandler` перехватывает PostgreSQL SQLSTATE `23P01` (`exclusion_violation`) и возвращает HTTP `409 Conflict` с телом `ApiErrorResponse(error="SLOT_ALREADY_BOOKED")`.
- **Конкурентный стресс-тест (`OverbookingConcurrencyIntegrationTest.kt`):** 20 параллельных потоков через `CyclicBarrier` отправляют одновременные запросы на бронирование одного и того же слота. Доказано: ровно 1 запрос завершается успехом `201 Created`, ровно 19 запросов получают отказ `409 Conflict` со структурой `SLOT_ALREADY_BOOKED`. В базе данных создана ровно 1 запись.

### 2. Смежные и перекрывающиеся интервалы (`AC-EDGE-001`, `AC-EDGE-002`)
- Полуинтервалы $[10:00, 11:00)$ и $[11:00, 12:00)$ успешно создаются без коллизий благодаря правосторонней открытости `[)`.
- Частично пересекающиеся интервалы $[10:00, 11:00)$ и $[10:30, 11:00)$ гарантированно отклоняются базой данных с HTTP 409 `SLOT_ALREADY_BOOKED`.

### 3. Исторические снапшоты (`ADR-011`)
- При создании записи сохраняются `serviceNameSnapshot` и `servicePriceSnapshot` на момент визита. Последующие мутации названия или цены услуги в каталоге не искажают исторические данные.

### 4. Жизненный цикл, отмена и защита от IDOR (`BR-004`, `AC-005`, `AC-EDGE-005`)
- Клиент B не может отменить бронирование клиента A (HTTP 403 `ACCESS_DENIED`).
- Попытка отмены бронирования в прошлом ($T_{start} \le now$) отклоняется с HTTP 400 `CANCELLATION_DEADLINE_EXCEEDED`.
- При успешной отмене статус меняется на `CANCELLED`. Так как статус исключён из предиката констрейнта, слот моментально освобождается для повторного бронирования другим клиентом.

### 5. Административная модерация (`AC-007`, `AC-009`, `AC-EDGE-007`)
- Конечный автомат статусов `BookingStatus.canTransitionTo` валидирует допустимость переходов:
  - `PENDING` $\to$ `CONFIRMED`, `REJECTED`, `CANCELLED`;
  - `CONFIRMED` $\to$ `COMPLETED`, `CANCELLED`;
  - Терминальные статусы (`COMPLETED`, `CANCELLED`, `REJECTED`) не допускают дальнейших мутаций (возврат `400 INVALID_STATUS_TRANSITION`).
- Фильтрация в панели администратора (`GET /api/v1/admin/bookings`) поддерживает параметры `status`, `masterId`, `date` (с автоматическим преобразованием дня в границы UTC по таймзоне заведения).

### 6. Личный кабинет клиента (`REQ-008`, `GET /api/v1/client/bookings`)
- Записи клиента четко разделяются на два списка:
  - `active` (`PENDING`, `CONFIRMED`) — отсортированы по возрастанию `startTime ASC` (ближайшие визиты первыми);
  - `archive` (`COMPLETED`, `CANCELLED`, `REJECTED`) — отсортированы по убыванию `startTime DESC` (последние завершённые визиты первыми).

---

## Итоговый вердикт

**`APPROVE`** — Ядро бронирования и защита от овербукинга (Этап 5) полностью верифицированы, соответствуют всем требованиям и критериям приёмки. Рекомендуется переходить к Этапу 6 (Web Frontend).
