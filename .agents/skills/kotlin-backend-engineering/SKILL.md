---
name: kotlin-backend-engineering
description: "Инженерная процедура реализации и проверки Kotlin/JVM backend: null-safety, Spring/JPA при наличии, транзакции, конкурентность, API, миграции и тесты."
---

# Kotlin Backend Engineering

1. Проверь `docs/PROJECT-PROFILE.md` и убедись, что задача относится к backend.
2. Исследуй фактические Kotlin/JDK/framework/Gradle версии, архитектуру, conventions и wrapper commands.
3. Сверь каждый change с REQ/AC. Не добавляй UI в backend-only проект.
4. Выбери минимально достаточный дизайн; обоснуй новые зависимости и абстракции.
5. Проверь nullability, validation, exception mapping, auth, transaction boundaries и concurrency в применимом объёме.
6. Для persistence проверь constraints, индексы, query shape, N+1, миграции и backward compatibility.
7. Для coroutines проверь structured concurrency, cancellation и отсутствие случайного blocking.
8. Добавь unit-тесты бизнес-логики и интеграционные тесты на реальные component boundaries, где это необходимо.
9. Запусти проверки существующим Gradle Wrapper/инструментами проекта. Не заявляй PASS без фактического результата.
10. Подготовь `DEVELOPER REPORT` с файлами, REQ/AC, решениями, командами/результатами и ограничениями.
