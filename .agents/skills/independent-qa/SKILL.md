---
name: independent-qa
description: "Независимое тестирование по acceptance matrix: тест-план, запуск, исследовательские проверки, дефекты и регрессия."
---

# Independent QA

1. Прочитай `docs/PROJECT-PROFILE.md`, спецификацию и acceptance matrix.
2. Создай traceability map и test plan.
3. Подготовь независимые проверки на позитивные, негативные, граничные, permissions, ошибки и повторные действия.
4. Выбирай проверки по профилю: backend — unit/integration/API/contract; Android — unit/UI/instrumented/device/lifecycle/permissions/offline; web — component/UI/browser E2E. Для комбинированных профилей проверяй каждый включённый компонент и контракты между ними. Не требуй неприменимые уровни тестирования. Недоступные Android device checks фиксируй как `NOT RUN`. Не выполняй destructive actions на реальных данных.
5. Запускай тесты и записывай фактический результат. Отделяй продуктовые дефекты от проблем среды.
6. Оформляй дефекты с шагами воспроизведения и ссылками на AC.
7. После фикса проверяй defect retest и regression.
8. Выдай QA verdict с `PASS`, `PASS WITH KNOWN ISSUES`, `FAIL` или `BLOCKED`.
