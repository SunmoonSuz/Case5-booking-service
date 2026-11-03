# Сервис онлайн-записи на услуги (Service Booking)

Полнофункциональная система онлайн-записи клиентов на услуги для предприятий сферы услуг (салоны красоты, барбершопы, автосервисы, студии).

- **Архитектурный профиль:** `FULL_STACK` (Kotlin 2.0 / Java 21 / Spring Boot 3.3.5 Backend + React 18 / TypeScript / Vite 6 Frontend).
- **СУБД:** PostgreSQL 16+ с аппаратным предотвращением овербукинга через GiST Exclusion Constraint.
- **Статус реализации:** `TRIAL_RELEASED` (Релиз v1.0.0-trial).

---

## 🚀 Ключевые возможности

1. **Гарантия отсутствия овербукинга (NFR-001, AC-003):**
   Физическая защита от наложения записей на уровне СУБД через расширение `btree_gist` и частичный констрейнт `EXCLUDE USING gist (master_id WITH =, (tstzrange(start_time, end_time, '[)')) WITH &&) WHERE (status IN ('PENDING', 'CONFIRMED'))`. Выдерживает параллельные гонки 20+ запросов на один слот с миллисекундной дельтой.
2. **Мгновенная реакция UI на конфликты (AC-004):**
   При перехвате HTTP 409 `SLOT_ALREADY_BOOKED` клиент получает информативный Toast, а TanStack Query мгновенно инвалидирует кэш слотов, обновляя доступность без перезагрузки всей страницы.
3. **Единый часовой пояс (Europe/Moscow, ADR-007, AC-010):**
   Сервер хранит данные строго в UTC (`TIMESTAMPTZ`), а клиентский веб-интерфейс принудительно форматирует время по таймзоне заведения `Europe/Moscow` вне зависимости от часового пояса устройства клиента.
4. **Умный расчет доступных слотов (AC-002):**
   Генерация дискретной сетки (шаг 30/60 мин) на 30 календарных дней вперёд с учётом смен мастера, обеденных перерывов и динамическим отсечением времени в прошлом ($T_{slot} \le T_{now}$).
5. **Личный кабинет клиента (AC-005):**
   Просмотр активных записей и истории визитов, отмена бронирования до наступления времени начала визита ($T_{start} > now$).
6. **Административная панель (AC-006, AC-007, AC-009):**
   Модерация входящих заявок (`confirm`, `reject` с причиной, `cancel`, `complete`), управление каталогом услуг (цены в RUB, снимки цен), мастерами и гибким графиком смен.
7. **Мобильная адаптивность и доступность (NFR-003):**
   Адаптивный интерфейс от 360px, размеры тач-таргетов $\ge 44 \times 44$px, клавиатурный Focus Trap в модальных окнах.

---

## 🛠 Технологический стек

- **Backend:**
  - Язык: Kotlin 2.0.21 (JVM 21 LTS)
  - Фреймворк: Spring Boot 3.3.5 (Spring Web, Spring Security, Spring Data JPA, Validation)
  - База данных: PostgreSQL 16+
  - Миграции: Flyway
  - Безопасность: BCrypt (cost factor 12), Cookie Session (`JSESSIONID`, HttpOnly, SameSite=Lax)
  - Тестирование: JUnit 5, MockK, Testcontainers PostgreSQL 16, AssertJ
- **Frontend:**
  - Язык: TypeScript 5.6 (strict mode)
  - Фреймворк: React 18, Vite 6
  - Стилизация: Tailwind CSS 3.4
  - Управление состоянием и кэшированием: TanStack Query v5 (React Query)
  - Роутинг и сеть: React Router v6, Axios
  - Тестирование: Vitest, React Testing Library, jsdom

---

## 🏃 Быстрый старт

### 1. Запуск PostgreSQL
```bash
docker run --name booking-postgres -d \
  -e POSTGRES_DB=booking_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Запуск сервера (Backend)
```bash
./gradlew bootRun
```
Сервер запустится на `http://localhost:8080`. При первом запуске Flyway создаст таблицы и индексы, а `AdminBootstrapRunner` автоматически инициализирует учетную запись администратора:
- **Email:** `admin@booking.local`
- **Пароль:** `admin12345`

### 3. Запуск веб-клиента (Frontend)
```bash
cd frontend
npm install
npm run dev
```
Интерфейс откроется по адресу `http://localhost:5173`.

---

## 🧪 Запуск тестов

### Тесты бэкенда (Unit, Concurrency, Integration):
```bash
./gradlew test
```
*Включает стресс-тест овербукинга `OverbookingConcurrencyIntegrationTest` с 20 параллельными потоками через `CyclicBarrier`.*

### Тесты фронтенда (Component, State, Flow):
```bash
cd frontend
npm test
```
*Запускает 16 тестов в Vitest, покрывающих регистрацию, бронирование, отмену, модерацию и перехват 409 ошибки.*

### Сборка production-бандла фронтенда:
```bash
cd frontend
npm run build
```

---

## 📚 Архитектурная и проектная документация

- [SYSTEM-ARCHITECTURE.md](docs/architecture/SYSTEM-ARCHITECTURE.md) — Системная архитектура и API-контракты.
- [PROJECT-PROFILE.md](docs/PROJECT-PROFILE.md) — Профиль инженерного проекта (`FULL_STACK`).
- [ADR-001 — ADR-012](docs/decisions/) — Архитектурные решения (PostgreSQL GiST, Cookie Session, Таймзоны, Снимки цен).
- [TEST-PLAN-001.md](docs/qa/TEST-PLAN-001.md) — Независимый QA тест-план со 100% матрицей трассируемости.
- [QA-EXECUTION-REPORT-001.md](docs/qa/QA-EXECUTION-REPORT-001.md) — Отчёт независимого тестирования.
- [REVIEW-REPORT-001 — 006](docs/review/) — Отчёты строгого независимого аудита кода (все в статусе `APPROVE`).
- [RELEASE-CHECKLIST.md](docs/release/RELEASE-CHECKLIST.md) — Чеклист ручного пробного выпуска.
- [RELEASE-NOTES-v1.0.0.md](docs/release/RELEASE-NOTES-v1.0.0.md) — Примечания к релизу v1.0.0.
