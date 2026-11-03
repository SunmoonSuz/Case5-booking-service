# ADR-005: Аутентификация, сессии и ролевая модель доступа (RBAC)

- Status: `ACCEPTED`
- Date: `2026-11-03`
- Decision owner: `Security Architect / Coordinator`

## Context

Сервис обслуживает три категории пользователей:
1. `GUEST`: неавторизованный пользователь. Доступен публичный каталог, мастера, слоты.
2. `CLIENT`: зарегистрированный клиент. Имеет право оформлять заявки (`PENDING`), просматривать свои бронирования в ЛК, отменять собственные активные записи до наступления визита. Доступ к чужим бронированиям и админке строго запрещен (`AC-008`).
3. `ADMIN`: сотрудник с ролью `ADMIN`. Доступ к `/api/v1/admin/*` для модерации заявок, управления каталогом, мастерами и расписанием.

Требования безопасности:
- Хранение паролей в захэшированном виде с криптографической солью (Argon2id или BCrypt) (`NFR-004`, `REQ-001`);
- Безопасный Logout с обязательной инвалидацией сессии на сервере (`REQ-002`, `SCN-001`);
- Защита административных маршрутов RBAC с возвратом `401 Unauthorized` для неавторизованных и `403 Forbidden` для роли `CLIENT` (`AC-008`);
- Изоляция данных: клиент видит и отменяет только свои бронирования (`REQ-008`, `REQ-009`).

## Decision

Рекомендуется:

### 1. Механизм аутентификации и сессий:
- **Server-Side Session с Cookie `JSESSIONID` (или токеном сессии в HttpOnly Cookie)** под управлением Spring Security.
- Атрибуты Cookie:
  - `HttpOnly = true` (недоступно для JavaScript, полная защита от XSS-утечек);
  - `SameSite = Lax` (защита от CSRF-атак первого порядка при переходах со сторонних сайтов);
  - `Secure = false` в dev-профиле (для работы на `http://localhost`), `true` в production/HTTPS;
  - `Path = /`.
- **Конфигурация REST API без редиректов:** Для исключения перенаправления на HTML-форму входа (`302 Redirect to /login`) в Spring Security настраивается `exceptionHandling().authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))`, что гарантирует возврат строго `401 Unauthorized` при неавторизованном доступе (`AC-008`).
- **CSRF-политика:** Ввиду использования `SameSite = Lax` на сессионной cookie и работы клиентского SPA через обратный прокси `/api` в одной origin-зоне (без кросс-доменных вызовов), CSRF для stateless REST API маршрутов отключается (`csrf { it.disable() }`), либо настраивается `CookieCsrfTokenRepository.withHttpOnlyFalse()` при прямом кросс-доменном взаимодействии.
- **Инвалидация Logout:** Вызов `POST /api/v1/auth/logout` вызывает `session.invalidate()` и очищает `SecurityContextRepository` на сервере, а также обнуляет cookie в ответе. Повторное использование идентификатора сессии невозможно.

### 2. Хэширование паролей:
- Использование **BCryptPasswordEncoder** (cost factor 12) или **Argon2PasswordEncoder** (параметры: salt length 16, hash length 32, parallelism 1, memory 65536, iterations 3). BCrypt полностью поддерживается в стандартной сборке Spring Security без нативных C-библиотек.

### 3. Ролевая модель и авторизация (RBAC):
- Две хранимые роли: `ROLE_CLIENT` и `ROLE_ADMIN`.
- Публичные эндпоинты:
  - `POST /api/v1/auth/register` (всегда создает пользователя с ролью `CLIENT`);
  - `POST /api/v1/auth/login`;
  - `GET /api/v1/services/**`, `GET /api/v1/masters/**`, `GET /api/v1/availability/**`.
- Клиентские эндпоинты (`hasRole('CLIENT')` или `hasAnyRole('CLIENT', 'ADMIN')`):
  - `POST /api/v1/bookings` (создание заявки);
  - `GET /api/v1/client/bookings` (просмотр своих бронирований, фильтрация по `client_id = authenticatedUser.id`);
  - `POST /api/v1/client/bookings/{id}/cancel` (серверная проверка: `booking.client_id == authenticatedUser.id`, иначе `403 Forbidden`).
- Административные эндпоинты (`hasRole('ADMIN')`):
  - `/api/v1/admin/**` — все эндпоинты требуют `hasRole('ADMIN')`.
  - При отсутствии авторизации возвращается HTTP `401 Unauthorized`.
  - При авторизации с ролью `CLIENT` возвращается HTTP `403 Forbidden`.

### 4. Ответ текущего пользователя (`GET /api/v1/auth/me`):
- Возвращает `{ "id": "...", "email": "...", "name": "...", "phone": "...", "role": "CLIENT | ADMIN" }`. Если сессии нет — `401 Unauthorized`.

## Alternatives considered

| Option | Benefits | Costs/Risks |
|---|---|---|
| **Вариант 1: Stateful HttpOnly Cookie Session (Рекомендуется)** | 1. Простота и максимальная надежность.<br>2. Моментальная серверная инвалидация при Logout (`REQ-002`).<br>3. Токены не сохраняются в LocalStorage, полная защита от кражи токенов скриптами.<br>4. Встроенная поддержка в Spring Security. | Требует сохранения сессии в памяти сервера (для Single-tenant одного экземпляра это идеальное решение без накладных расходов). |
| **Вариант 2: Stateless JWT (Access + Refresh Token в LocalStorage)** | 1. Масштабируемость без сохранения состояния на сервере. | 1. Уязвимость LocalStorage к XSS.<br>2. Невозможность мгновенной инвалидации при Logout без ведения серверного черного списка (что превращает JWT обратно в stateful решение).<br>3. Избыточная сложность для Single-tenant MVP. |
| **Вариант 3: JWT в HttpOnly Cookie** | 1. Защищен от XSS. | Та же проблема с серверной инвалидацией Logout (`REQ-002` прямо требует аннулирования сессии на бэкенде). |

## Consequences

- Клиентское веб-приложение отправляет запросы с параметром `credentials: 'include'` (Axios: `withCredentials: true`).
- Вход в систему и регистрация автоматически устанавливают сессионную cookie.
- Попытки эскалации привилегий (передача чужого `client_id` или `role`) полностью блокируются на уровне серверных сервисов.

## Requirements affected

- `REQ-001`, `REQ-002`, `NFR-004`, `SCN-001`, `AC-001`, `AC-008`.

## Review date / conditions to revisit

- До старта реализации Identity & Access (Milestone 3).
