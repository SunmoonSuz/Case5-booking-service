---
trigger: model_decision
---
# Kotlin/JVM Backend профиль

Применяй эту инструкцию, когда текущая задача относится к Kotlin/JVM backend или проект использует Kotlin.

- Приоритет технологий: фактический репозиторий и утверждённый brief выше любых типовых предпочтений.
- Сохраняй Kotlin null-safety, идиоматичный код и совместимость версий Kotlin/JDK/framework.
- Предпочитай constructor injection и явные зависимости.
- Проверяй транзакционные границы, конкурентность, cancellation для coroutines, обработку ошибок и целостность данных.
- Не вводи Spring Boot, JPA, Flyway, Kafka, coroutines или PostgreSQL без требований/обоснования.
- Изменения схемы БД сопровождай миграциями и проверкой совместимости; не правь production-данные.
- API-контракт и backend authorization являются серверной ответственностью.
- Не создавай frontend артефакты в `BACKEND_ONLY` проекте.
