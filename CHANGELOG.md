# Журнал изменений UniPlan

Кратко — что добавляет каждый коммит (новые сверху).

## Боевой адаптер Layer 2 (домен → coursett) + декомпозиция работ
- `src/main/kotlin/org/uniplan/scheduling/CoursettLayer2.kt`: строит модель coursett **из доменных сущностей** (`Section`/`Location`/`Instructor`/`DatePattern`) и решает «время + аудитория». Свободный старт генерируется по рабочему окну (шаг 30 мин); week-aware конфликты — из `DatePattern.dates`.
- Тесты (`CoursettLayer2Test`, 2 шт., зелёные) на in-memory сущностях **без БД**: свободное время без наложений; числитель/знаменатель на комплементарных неделях.
- `docs/UniPlan-декомпозиция-работ.md` — WBS (эпики 0–6 + пост-MVP бэклог).

## Спайк Layer 2: адаптер домен → coursett (главный де-риск Strategy A закрыт)
- `src/main/kotlin/org/uniplan/spike/CoursettSpike.kt`: модель `coursett` строится **программно** из игрушечного блока, решатель выдаёт «время + аудитория». Запуск: `./gradlew runCoursett`.
- **Сценарий 1** (1 преподаватель + 1 аудитория, 3 дисциплины): свободное время (старт 08:30/10:30, длительность 90 мин и 4 ч) — все размещены без наложений.
- **Сценарий 2** (числитель/знаменатель): две секции в одном слоте/аудитории на нечёт/чёт недели → обе размещены, **week-aware конфликты работают** (требование R5).
- Подтверждает переиспользование coursett для Layer 2 — ключевая ставка Strategy A.

## Материализация доменной модели: Spring Boot + JPA
- Переход на **Spring Boot 3.3.5 + Spring Data JPA**; PostgreSQL (основной таргет) + H2 (профиль `dev`).
- **~17 JPA-сущностей** по `docs/UniPlan-доменная-модель.md` (`org.uniplan.domain`): оргструктура (University/Institute/Department), календарь (Term/Block/DatePattern), учебные (Course/CourseOffering/Group/Section), ресурсы (Location/Instructor), цикловая ротация (DisciplineAssignment/RotationAssignment), предпочтения преподавателей (InstructorAvailability), версионирование (ScheduleVersion/Assignment). Страховки INS-1..4 заложены.
- Spring Data репозитории; **context-load тест на H2 (зелёный)** — проверяет маппинг всех сущностей + сохранение.
- `application.yml` (профили: Postgres по умолчанию / H2 dev), `docker-compose.yml` (Postgres 16).
- Спайк-0 теперь запускается `./gradlew runSpike` (приложение — `./gradlew bootRun`).
- Follow-up: заменить Hibernate `ddl-auto` на Flyway-миграции.

## Каркас проекта + спайк CPSolver + проектные документы
- Gradle/Kotlin проект: wrapper 8.10.2, JDK 21, зависимость `org.unitime:cpsolver:1.4.91`.
- **Спайк-0** (`src/main/kotlin/org/uniplan/spike/BlockSpike.kt`): прогон движка IFS из нашего кода — зелёный, подтверждает цепочку Gradle → CPSolver → решатель.
- Проектные документы в `docs/`:
  - `UniTime-ограничения-и-критерии.md` — полный разбор ограничений/критериев UniTime (3 решателя).
  - `UniPlan-блочное-расписание-дизайн.md` — двухслойная модель блочности, трассировка реальных требований, будущие возможности.
  - `ADR-0001-форк-cpsolver.md` — решение: Strategy A (depend + extend CPSolver, LGPL-3.0).
  - `UniPlan-MVP-план.md` — план MVP на 5 недель.
  - `UniPlan-доменная-модель.md` — доменная модель Недели 2 (со страховками INS-1..4).
- Служебное: `.gitignore`, `log4j2.xml`.
