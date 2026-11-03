---
name: android-app-engineering
description: "Разработка и проверка native Android-приложений на Kotlin с учётом Android lifecycle, архитектуры, UI, безопасности и тестирования."
---

# Android App Engineering

Используй этот skill только для утверждённого Android scope.

1. Прочитай `docs/PROJECT-PROFILE.md`, спецификацию и Android-specific acceptance criteria.
2. Изучи существующие Gradle modules, Kotlin/AGP/Gradle versions, SDK levels, Compose или Views, навигацию, DI, local storage, networking и тестовую конфигурацию.
3. Не меняй версии инструментов и архитектурный стиль без необходимости и проверки совместимости.
4. Составь карту `REQ -> AC -> Android module/file -> test`.
5. Сначала опиши UI flows и состояния: loading, content, empty, error, validation, permission denied, offline/partial data — только применимые состояния.
6. Предложи минимальную архитектуру. Предпочитай чёткие UI/data boundaries, repositories, UDF, immutable UI state, coroutines/Flow; domain layer и дополнительные abstraction только при реальной сложности.
7. Для нового интерфейса рассматривай Jetpack Compose как основной вариант; для legacy-проекта сохраняй Views/XML, если миграция не входит в scope.
8. Учитывай lifecycle, process death, configuration changes, navigation, state restoration, background execution constraints и адаптивность заявленных устройств.
9. Проверь permissions, exported components, deep links, storage, secrets, network security и логирование чувствительных данных.
10. Добавляй только необходимые зависимости и проверяй актуальную совместимость с проектом.
11. Добавь применимые unit/UI/instrumented tests. Не считай один уровень тестов заменой остальных, если требования требуют device/UI verification.
12. Запусти фактические Gradle checks. Зафиксируй точные команды, окружение, итоги и `NOT RUN` для недоступных проверок.
13. Верни `ANDROID DEVELOPER REPORT`, привязанный к REQ/AC.

Не реализуй backend/API/server infrastructure и не меняй требования. При backend scope передавай работу `developer`.
