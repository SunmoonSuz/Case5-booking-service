# Профиль инженерного проекта

Этот файл определяет состав реализации. Заполни его до технического дизайна. Он не заменяет спецификацию и не может расширять требования.

## Тип проекта

Выбери ровно один профиль:

- `BACKEND_ONLY` — API, сервис, worker, интеграция, обработка событий, CLI/server-side компонент без пользовательского клиента. **Профиль по умолчанию.**
- `ANDROID_ONLY` — native Android-приложение без разработки backend в этом репозитории; серверные API являются внешними/существующими.
- `FRONTEND_ONLY` — web-интерфейс, использующий существующий внешний API.
- `BACKEND_AND_ANDROID` — Kotlin/JVM backend и native Android-клиент.
- `FULL_STACK` — backend и web frontend.
- `BACKEND_WEB_AND_ANDROID` — backend, web frontend и native Android-клиент.
- `UNDECIDED` — профиль ещё не определён; это блокирующий вопрос, если от него зависит scope или архитектура.

```yaml
project_type: FULL_STACK
```

## Роли по профилю

| Профиль | Основные роли | Не подключать автоматически |
|---|---|---|
| `BACKEND_ONLY` | Главный координатор, `developer`, `tester`, `strict-code-reviewer` | `android-developer`, `frontend-developer` |
| `ANDROID_ONLY` | Главный координатор, `android-developer`, `tester`, `strict-code-reviewer` | `developer`, `frontend-developer` |
| `FRONTEND_ONLY` | Главный координатор, `frontend-developer`, `tester`, `strict-code-reviewer` | `developer`, `android-developer` |
| `BACKEND_AND_ANDROID` | Главный координатор, `developer`, `android-developer`, `tester`, `strict-code-reviewer` | `frontend-developer` |
| `FULL_STACK` | Главный координатор, `developer`, `frontend-developer`, `tester`, `strict-code-reviewer` | `android-developer` |
| `BACKEND_WEB_AND_ANDROID` | Главный координатор, `developer`, `frontend-developer`, `android-developer`, `tester`, `strict-code-reviewer` | — |

`developer` специализируется на Kotlin/JVM backend, `android-developer` — на native Android/Kotlin, `frontend-developer` — на web frontend. Это разные зоны ответственности, даже если несколько из них используют Kotlin или TypeScript.

## FULL_STACK

Заполняй, когда backend входит в scope (`BACKEND_ONLY`, `BACKEND_AND_ANDROID`, `FULL_STACK`, `BACKEND_WEB_AND_ANDROID`). Для нового backend-проекта, если технологии не заданы и владелец согласен с предложением, предпочтительный профиль:

- Язык/runtime: Kotlin/JVM.
- Framework: Spring Boot, если он оправдан типом сервиса.
- Build: Gradle; предпочитай существующий Gradle Wrapper и выбранный DSL.
- Database: определить по требованиям; PostgreSQL — кандидат, а не обязательное решение.
- Persistence/migrations: использовать выбранные проектом технологии; не добавлять JPA, Flyway или другие инструменты автоматически.
- Tests: существующий test stack; для нового Kotlin-проекта обычно JUnit 5, с MockK или Mockito только после проверки совместимости и обоснования.
- Async/messaging/cache: только если есть конкретные требования.

```yaml
backend:
  language: Kotlin
  runtime: JVM (Java 21 LTS)
  framework: Spring Boot 3.3.x (Spring Web, Spring Security, Spring Data JPA / JDBC, Validation)
  build_tool: Gradle (Kotlin DSL)
  database: PostgreSQL 16+
  persistence: Spring Data JPA + Hibernate 6 / JDBC
  migration_tool: Flyway
```

## Android stack

Заполняй, когда Android входит в scope (`ANDROID_ONLY`, `BACKEND_AND_ANDROID`, `BACKEND_WEB_AND_ANDROID`). Это профиль native Android на Kotlin, а не автоматическое требование Kotlin Multiplatform.

```yaml
android:
  required: false
  status: OUT_OF_SCOPE
  app_type: N/A
  kotlin_multiplatform: false
```

## Web frontend stack

Заполняется только для `FRONTEND_ONLY`, `FULL_STACK` или `BACKEND_WEB_AND_ANDROID`.

```yaml
frontend:
  required: true
  platform: WEB
  framework: React 18/19 (TypeScript)
  build_tool: Vite
  design_system: Tailwind CSS + Lucide Icons + TanStack Query v5
```

## Интеграции и контракты

- API style/versioning: RESTful JSON under `/api/v1` (`/auth`, `/services`, `/masters`, `/schedule`, `/slots`, `/bookings`).
- AuthN/AuthZ: State-managed cookie session (`JSESSIONID`, HttpOnly, SameSite=Lax, Secure in prod) + BCrypt (ADR-005). RBAC: `CLIENT`, `ADMIN`.
- External systems: Отсутствуют в рамках v1 (single-tenant монолит).
- Events/message broker: Не требуются (ACID транзакции в рамках единой PostgreSQL БД).
- Data ownership and transaction boundaries: PostgreSQL ACID transaction boundary. Предотвращение овербукинга через PostgreSQL GiST Exclusion Constraint на таблице `bookings` (ADR-004).
- API contract location (OpenAPI / other / not applicable): Задокументирован в `docs/architecture/SYSTEM-ARCHITECTURE.md#rest-api-contracts` и ADR-006.
- Backend ↔ Android contract (если применимо): N/A (Android не входит в scope).
- Backend ↔ Web contract (если применимо): `docs/architecture/SYSTEM-ARCHITECTURE.md` и ADR-006.

## Правило маршрутизации задач

- `BACKEND_ONLY`: `developer` реализует только backend. Не создавай клиентские backlog и не вызывай `android-developer`/`frontend-developer`.
- `ANDROID_ONLY`: `android-developer` реализует native Android. Backend-разработчик не вызывается для внешнего API, если изменение server side не входит в scope.
- `FRONTEND_ONLY`: `frontend-developer` реализует только web UI; `android-developer` не вызывается.
- `BACKEND_AND_ANDROID`: дели scope между `developer` и `android-developer`. До параллельной разработки согласуй API-контракт, auth, error model, pagination, versioning и ownership данных.
- `FULL_STACK`: дели scope между `developer` и `frontend-developer`; согласуй web API-контракт.
- `BACKEND_WEB_AND_ANDROID`: дели backend, web и Android scope между тремя профильными агентами; главный агент обеспечивает единые API-контракты и совместимость.
- Не делегируй компонент, которого нет в утверждённом scope. Не меняй профиль на основании предположений.
- При изменении профиля обнови этот файл, brief, задачи и оценку влияния.

## Подтверждение

- Профиль подтверждён владельцем: да
- Кто и когда: Product Owner в диалоге 2026-11-03 ("со всеми вопросами согласен по утвеждению")
- Связанные требования/решение: IMPLEMENTATION-BRIEF.md, ADR-001 - ADR-012, SYSTEM-ARCHITECTURE.md, review report #001
