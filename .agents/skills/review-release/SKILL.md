---
name: review-release
description: "Строгое read-only code review, закрытие замечаний и ручная подготовка trial release без CI/CD."
---

# Review and Manual Trial Release

1. Зафиксируй базовую ревизию и scope diff.
2. Передай diff, спецификацию, QA report и тестовые evidence саб-агенту `strict-code-reviewer`.
3. Ревьюер работает read-only и возвращает вердикт.
4. BLOCKER/CRITICAL/MAJOR замечания передай владельцу компонента: `developer` (backend), `android-developer` (native Android) или `frontend-developer` (web UI). После исправлений повтори затронутые тесты и ревью.
5. Убедись, что DoD и release checklist выполнены.
6. Подготовь release notes, known issues, инструкции запуска и демонстрационный сценарий. Для Android отдельно укажи способ локальной установки тестовой сборки, поддерживаемые устройства/SDK и ограничения проверки.
7. Попроси явное разрешение пользователя перед пробным развёртыванием/публикацией.
8. Запусти сборку/демо вручную, зафиксируй команду и результат. Не создавай CI/CD pipeline или автоматический деплой.
