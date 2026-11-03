# Первый прогон на небольшом Android-проекте

Этот сценарий проверяет работу аналитики, Android-разработчика, независимого QA и code review без обязательной серверной разработки.

## 1. Создай аналитический кейс

В `analysis-project` создай папку кейса из `_TEMPLATE` и явно укажи:
- тип приложения: native Android;
- профиль `ANDROID_ONLY`, если backend/API уже существует или не входит в этот репозиторий;
- профиль `BACKEND_AND_ANDROID`, если нужно разрабатывать и сервер, и приложение;
- нужные устройства, минимальную версию Android, offline-сценарии, permissions, способ авторизации и требования к данным.

Попроси аналитика описать пользовательские сценарии, состояния экранов, системные взаимодействия, ограничения устройств, безопасность и критерии приёмки. Не утверждай Compose/Room/Retrofit/Hilt до технического обследования — это решения реализации, если владелец не задал их явно.

## 2. Подготовь Android-репозиторий

Скопируй `implementation-project-template/` в корень отдельного репозитория приложения. Перенеси утверждённый handoff в `docs/spec/`.

В `docs/PROJECT-PROFILE.md` установи `project_type: ANDROID_ONLY` и заполни Android stack: Kotlin/AGP/Gradle, minSdk/compileSdk/targetSdk, UI toolkit, navigation, persistence, networking, DI и test stack. Неизвестное оставь `OPEN QUESTION`.

## 3. Запусти цикл

Используй этот запрос:

```text
Это native Android приложение на Kotlin. Прочитай AGENTS.md, docs/PROJECT-PROFILE.md, docs/spec/IMPLEMENTATION-BRIEF.md и утверждённые требования.
Подтверди профиль ANDROID_ONLY. Обследуй репозиторий: Kotlin, Gradle Wrapper, Android Gradle Plugin, SDK levels, модули, Compose/Views, navigation, DI, persistence, networking и тестовую инфраструктуру.
Не создавай backend и web frontend. Используй skill android-app-engineering. До реализации проверь DoR, предложи минимальный технический план и обоснуй зависимости/архитектуру. Не меняй версии инструментов без необходимости и согласования.
После моего подтверждения делегируй реализацию android-developer, независимый тест-план и QA — tester, read-only review — strict-code-reviewer. Запускай доступные unit/UI/instrumented checks фактически; если эмулятор или SDK недоступны, укажи NOT RUN. Не публикуй приложение и не подписывай production release.
```

## 4. Проверяй доказательства

- Unit-тесты не заменяют Compose UI или instrumented tests, если требования требуют проверки реального Android поведения.
- Отсутствие Android SDK/эмулятора должно быть указано как ограничение среды, а device checks — `NOT RUN`.
- Проверяй не только happy path, но и permissions, поворот/изменение конфигурации, восстановление состояния, offline/error states и accessibility — если эти сценарии входят в scope.
- Не разрешай агентам публиковать APK/AAB или менять Play Console/production signing без отдельного разрешения.

## 5. Границы ответственности

- `android-developer`: native Android/Kotlin app.
- `developer`: Kotlin/JVM backend и server API.
- `frontend-developer`: web UI.
- `tester`: независимый QA соответствующих компонентов.
- `strict-code-reviewer`: общий read-only review.
