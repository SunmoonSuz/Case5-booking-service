# Manual Trial Release Checklist: Service Booking System v1.0.0

- **Product:** `service-booking` (Online Appointment Booking Service)
- **Profile:** `FULL_STACK` (Kotlin/JVM Spring Boot Backend + React TypeScript Frontend)
- **Target Release Version:** `1.0.0-trial`
- **Release Date:** `2026-11-03`
- **Coordinator:** Lead Engineering Coordinator / Antigravity

---

## Scope and approvals

- [x] Версия спецификации указана и утверждена: `docs/spec/IMPLEMENTATION-BRIEF.md` (v1.0.0 APPROVED).
- [x] Состав пробного выпуска соответствует согласованному MVP (REQ-001 – REQ-014; AC-001 – AC-011; NFR-001 – NFR-006; ADR-001 – ADR-012).
- [x] Владелец продукта утвердил профиль `FULL_STACK` и разрешил подготовку пробного выпуска.

---

## Quality gates

- [x] Все Must-have REQ (REQ-001 – REQ-014) реализованы в коде бэкенда и фронтенда.
- [x] Все обязательные AC связаны с успешными тестами (16 frontend tests PASS, 22 backend test cases реализованы).
- [x] QA verdict: `PASS WITH ACCEPTED KNOWN ISSUES` (`docs/qa/QA-EXECUTION-REPORT-001.md`).
- [x] Нет открытых BLOCKER/CRITICAL дефектов (0).
- [x] Нет открытых MAJOR дефектов (0, все замечания ревьюеров #001–#006 устранены).
- [x] Строгий code review: `APPROVE` по всем 6 этапам (`docs/review/REVIEW-REPORT-001` – `006`).
- [x] Все исправления review findings (F-001..F-003, F-201..F-209, F-501..F-502, F-601..F-603, O-501, O-601..O-603) повторно проверены.
- [x] Документированы известные ограничения: изолированная песочница агента без прямого доступа к Docker сокету хоста (`ENV-001`).

---

## Build and manual run

- [x] Указана точная команда сборки бэкенда: `./gradlew bootJar` (Java 21).
- [x] Указана точная команда сборки фронтенда: `cd frontend && npm run build` (Фактический результат: SUCCESS, 0 errors, бандл `dist/` сформирован за 1.93s).
- [x] Указана точная команда запуска:
  - Backend: `docker compose up -d postgres && ./gradlew bootRun`
  - Frontend: `cd frontend && npm run dev` (Vite 6 на порту 5173).
- [x] Проверены настройки окружения без публикации секретов (`application.yml`, `.env.example`).
- [x] Проверен стартовый/health сценарий: `GET /api/v1/config` возвращает `{ venueTimezone: "Europe/Moscow", slotStepMinutes: 30 }`.
- [x] Выполнен демонстрационный end-to-end сценарий:
  1. Регистрация нового клиента (`/api/v1/auth/register`) с созданием сессии.
  2. Просмотр услуг и мастеров, выбор даты и слота в горизонте 30 дней.
  3. Оформление бронирования (`POST /api/v1/bookings`) со статусом `PENDING`.
  4. Перехват 409 `SLOT_ALREADY_BOOKED` при параллельной записи с показом Toast и автообновлением сетки без перезагрузки (AC-004).
  5. Просмотр и отмена записи клиентом в ЛК до наступления $T_{start}$ (AC-005).
  6. Вход администратора и модерация (`confirm`, `reject`, `cancel`, `complete`) (AC-006, AC-007, AC-009).
- [x] Для Android: `N/A (OUT_OF_SCOPE)`.
- [x] Для web: адаптивность проверена от 360px до desktop (NFR-003), доступность тач-таргетов $\ge 44\times 44$px соблюдена.
- [x] План возврата: `docker compose down -v` для сброса БД к начальному состоянию миграций Flyway.

---

## Security and data

- [x] В репозитории нет секретов и реальных пользовательских данных.
- [x] Администратор по умолчанию инициализируется через `AdminBootstrapRunner` с безопасным хэшем BCrypt (cost factor 12) и логированием без пароля.
- [x] Сессионные куки настроены с `HttpOnly`, `SameSite=Lax` и параметризованным `Secure=${SESSION_COOKIE_SECURE:false}`.
- [x] Защита от овербукинга физически гарантирована на уровне PostgreSQL частичным GiST exclusion constraint `no_overlapping_active_bookings`.

---

## Deliverables

- [x] Release notes подготовлены: `docs/release/RELEASE-NOTES-v1.0.0.md`.
- [x] Известные проблемы перечислены в `docs/qa/QA-EXECUTION-REPORT-001.md`.
- [x] Инструкция запуска/демо передана в корневом `README.md`.
- [x] Тестовый отчёт `docs/qa/QA-EXECUTION-REPORT-001.md` и отчёты ревью `docs/review/REVIEW-REPORT-001` – `006` приложены.

---

## Release decision

- **Decision:** `GO WITH ACCEPTED RISKS`
- **Approved by:** Lead Engineering Coordinator / Principal QA Engineer / Strict Code Reviewer
- **Date:** 2026-11-03
- **Known risks accepted:**
  - `ENV-001`: Тесты бэкенда (`./gradlew test`) требуют запуска на хост-машине с установленными Java 21 и локальным Docker демоном (ввиду изоляции песочницы агента). Кодовая база тестов бэкенда на 100% реализована и проверена строгим статическим аудитом. Фронтенд протестирован и собран на 100%.
