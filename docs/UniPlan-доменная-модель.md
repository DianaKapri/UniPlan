# UniPlan — доменная модель (Неделя 2)

> Консолидирует и уточняет набросок из `UniPlan-блочное-расписание-дизайн.md` (§2), вбирая: требования реальных документов (§0.1 там же), четыре «страховки» под будущие фичи (§7.1), цикловую ротацию и оргструктуру. Псевдо-схема (стек-агностично; читается как ER / Kotlin data classes / JPA-сущности).

Легенда тегов: **[L2]** — вход для coursett (расписание внутри блока/недели), **[L1]** — цикловая ротация (Layer 1), **[X]** — сквозное, **[INS-n]** — страховка под будущую фичу (см. §7.1 дизайна).

---

## 1. Оргструктура и тенант [X] [INS-2]

```
University {            // ★ ТЕНАНТ (SaaS). Один вуз = один тенант.
  id, name
}

Institute {             // институт/факультет ВНУТРИ вуза (ИКМ и т.д.)
  id, universityId
  name, code
}

Department {            // кафедра / клиническая база
  id, instituteId
  name, code
  kind: ACADEMIC | CLINICAL_BASE       // обычная кафедра или клиническая база
  capacityGroupsParallel: int?         // [L1] сколько групп берёт ОДНОВРЕМЕННО (ёмкость, R10)
}
```
> **[INS-2]**: все ключевые сущности несут FK вверх по иерархии `universityId`/`instituteId`, даже если MVP работает с одним институтом. Добавить сейчас дёшево, ретрофитить — больно.

---

## 2. Календарь и время [X]

```
Term {                  // учебный период (семестр)
  id, universityId
  name                  // "весна 2026"
  startDate             // 2026-02-09 (пн, неделя 1)
  endDate
}

Block {                 // [L1] блок цикловой системы: упорядоченный кусок терма
  id, termId
  sequence: int         // порядок 1,2,3...
  name?, startDate, endDate, lengthDays
  dayGrid: DayGrid
}

DayGrid {               // дневная сетка (ось 2). Для недельной системы — общая на терм.
  weekdays: [MON..SAT]
  // НЕТ фиксированных пар: время свободное, гранулярность 5 мин (как в CPSolver)
  workWindow: { from, to }   // напр. 08:00–21:00 (вмещает вечерние смены, R7)
}

HolidayCalendar {       // [X] праздники терма
  termId
  holidays: Date[]      // 23.02, 09.03, 19.03, 01.05, 11.05 ... (R4)
}

DatePattern {           // [X] НАБОР ДАТ, когда занятие реально идёт (а не «недель»)
  id, termId
  type: STANDARD | ALTERNATE_WEEKS | CUSTOM
  dates: Date[]         // конкретные даты — чтобы вычитать праздники по дню недели
  label?                // "(1,3,5,...16 н)" для отображения
}

// DatePatternGenerator (сервис, не сущность): из HolidayCalendar + правила
//   периодичности (каждая N-я неделя; нечёт/чёт; диапазон) → DatePattern в датах.
//   Покрывает R3 (раз в 3 нед.), R4 (вычет праздников), R5 (нечёт/чёт), R6 (сдвиг).
```

---

## 3. Учебные сущности

```
Course {                // дисциплина/предмет
  id, instituteId
  code, title
  courseYear: int       // курс 1..6 → определяет систему: 1–3 SPAN, 4–6 BLOCK, 3 переходный (R1)
  prerequisites: CourseId[]
}

CourseOffering {        // дисциплина, предлагаемая в терме
  id, courseId, termId
  scope: SPAN | BLOCK              // недельная (1–3) vs блочная (4–6) (R1)
  allowedBlocks: BlockId[]         // [L1] для BLOCK: в каких блоках может идти
  durationDays: int?               // [L1] длина одного прохода (R9): интенсив=1, длинные окна=N
  groupsParallel: int?             // [L1] сколько групп берёт одновременно (R9)
  delivery: IN_PERSON | ONLINE     // [INS] лекции дистанционно (R12)
}

Section {               // [L2] конкретная секция = то, что ставится в сетку
  id, offeringId
  blockId: BlockId?                // BLOCK: в каком блоке; null для SPAN (весь терм)
  datePatternId: DatePatternId     // на каких ДАТАХ идёт (числитель/знаменатель, R3/R5)
  dailyTime: { startSlot, lengthSlots }?  // свободное время 5-мин слотами; null = решатель выберет
  weekdays: [MON..FRI]
  capacity: int
  instructorIds: InstructorId[]
  locationIds: LocationId[]        // [] если ONLINE
  groupIds: GroupId[]              // какие группы посещают (единица — группа, R13)
}

Group {                 // ★ АТОМАРНАЯ ЕДИНИЦА планирования (не студент!) (R13)
  id, instituteId
  code                  // "1.4.01" (курс.поток.номер)
  courseYear: int
  size: int             // для проверки вместимости
}
```

---

## 4. Ресурсы [X] [INS-3]

```
Location {              // аудитория / лаборатория / клин. база / онлайн
  id
  kind: ROOM | LAB | CLINICAL_BASE | ONLINE
  name, capacity
  ownerDepartmentId     // [INS-3] кто владеет
  shared: boolean       // [INS-3] общий между институтами? (R: общие ресурсы)
  availability          // недоступные интервалы (по датам/времени)
}

Instructor {
  id
  name
  ownerDepartmentId     // [INS-3] основная кафедра
  shared: boolean       // [INS-3] читает в нескольких институтах?
}
```
> **[INS-3]**: `ownerDepartmentId` + `shared` делают межинститутный шаринг ресурсов выразимым позже (общий прогон или committed-ограничения), не ломая силосную организацию.

---

## 5. Преподавательские предпочтения [INS-4] (пост-MVP фича, поля сейчас)

```
InstructorAvailability {        // слоты «удобно/неудобно» + воркфлоу одобрения
  id, instructorId, termId
  weekday, fromSlot, toSlot     // интервал
  preference: REQUIRED | STRONGLY_PREFERRED | PREFERRED
            | NEUTRAL | DISCOURAGED | STRONGLY_DISCOURAGED | PROHIBITED  // шкала UniTime −4..+4/R/P
  status: PROPOSED | APPROVED | REJECTED    // [INS-4] заготовка под одобрение составителем
}
```
> Одобренные записи → в решатель: PROHIBITED = жёсткая недоступность, остальные = мягкие time-preferences. UI/воркфлоу — фаза 2+.

---

## 6. Цикловая ротация [L1] (R8–R11)

```
DisciplineAssignment {  // разбиение дисциплины по кафедрам с ДИАПАЗОНОМ групп (R11)
  id, offeringId
  departmentId
  groupRangeFrom, groupRangeTo   // непрерывный диапазон: "1.4.01"–"1.4.39"
  capacityGroupsParallel: int    // ёмкость именно этого назначения
}

RotationAssignment {    // КТО-КУДА-КОГДА: ядро ротации (вход в Режиме A / выход в Режиме B)
  id, termId
  blockId
  groupId               // или groupRange
  offeringId            // какую дисциплину проходит в этом блоке
  departmentId          // на какой кафедре/базе
}
```
> Эти две сущности **одинаковы** для Режима A (валидация ручной ротации) и Режима B (генерация). Выбор A/B меняет слой логики, не схему. Правила валидации (Режим A): ёмкость не превышена по блокам, группа не дублируется, покрытие всех дисциплин, диапазоны непрерывны.

---

## 7. Версионирование расписания [INS-1] (даёт «минимальные изменения» без PDF)

```
ScheduleVersion {       // версия расписания терма
  id, termId
  label                 // "изменения от 29.01.26"
  status: DRAFT | PUBLISHED
  createdAt, basedOnVersionId?   // от какой версии отталкивались (для MPP)
}

Assignment {            // результат решателя в рамках версии
  id, versionId
  sectionId
  // итог: блок/даты/время/комнаты/инструкторы (зафиксированное размещение)
}
```
> **[INS-1]**: версии + `basedOnVersionId` → «минимальные изменения» = MPP от предыдущей версии (`Perturbations`), **без PDF**. PDF-импорт — отдельный assisted-bootstrap легаси, не ядро.

---

## Карта «сущность → роль в решателе»
- **[L2] coursett-вход:** `Section`, `Location`, `Instructor`, `Group`, `DatePattern`, `InstructorAvailability(approved)`.
- **[L1] ротация:** `Block`, `Block`-`CourseOffering` параметры, `DisciplineAssignment`, `RotationAssignment`, ёмкости `Department`.
- **[X] сквозное:** оргструктура, `Term`/`HolidayCalendar`, `ScheduleVersion`.

## Что НЕ строим в MVP (поля есть, логики нет)
Воркфлоу одобрения преподавателей (UI), PDF-импорт, межинститутный общий прогон, авто-генерация ротации (Режим B). Поля/связи под них заложены — ретрофита не будет.
