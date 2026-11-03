---
trigger: always_on
---
# Android engineering rules

Эти правила применяются только к Android scope; они не требуют Android-компонентов в backend-only или web-only проектах.

- Для Android задач сначала прочитай `docs/PROJECT-PROFILE.md` и убедись, что Android входит в утверждённый scope.
- Android production code реализует `android-developer`; серверную часть — `developer`; web UI — `frontend-developer`.
- Сохраняй фактические версии Kotlin, Android Gradle Plugin, Gradle и SDK, если изменение не требуется и не согласовано.
- Не предполагай Compose, Hilt, Room, Retrofit, Ktor, WorkManager или конкретные архитектурные слои без изучения проекта.
- Не встраивай секреты в приложение; не ослабляй TLS; минимизируй permissions и exported components.
- Учитывай Android lifecycle, process death, configuration changes, main-thread restrictions и background execution limits.
- Проверяй accessibility, adaptive layout, offline/error states в пределах требований.
- Учитывай, что UI/device tests требуют отдельной тестовой среды; не отмечай недоступные device tests как PASS.
- Не публикуй приложения и не выполняй release signing/deployment без отдельного разрешения владельца.
