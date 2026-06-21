# UniTime — полный справочник ограничений и критериев построения расписаний

> Сводка по open-source системе **UniTime** и её решателю **CPSolver**. Источники: документация `help.unitime.org`, исходный код CPSolver (`github.com/UniTime/cpsolver`), научные статьи Tomáš Müller (PhD'05, PATAT'05/'08/'24, MISTA'13). Числовые веса по умолчанию взяты из исходного кода и базовых конфигов решателя — на высокоуровневых страницах документации их нет.

UniTime — это **три отдельных решателя**, у каждого свой набор ограничений и критериев:

1. **Course Timetabling** — расписание занятий (время + комната + инструктор для каждого класса).
2. **Examination Timetabling** — расписание экзаменов (период + комната(ы) для каждого экзамена).
3. **Student Sectioning / Scheduling** — распределение студентов по секциям при уже готовом расписании.

Общий принцип всех решателей: **жёсткие ограничения (hard) не нарушаются никогда** — решатель скорее оставит переменную неназначенной, чем нарушит hard-ограничение. **Мягкие критерии (soft)** образуют взвешенную целевую функцию, которая минимизируется (или максимизируется — у студентов).

Шкала мягких предпочтений везде одинаковая: `Required (R)` / `Strongly Preferred (−2/−4)` / `Preferred (−1)` / `Neutral (0)` / `Discouraged (+1)` / `Strongly Discouraged (+2/+4)` / `Prohibited (P)`.

---

# ЧАСТЬ 1. COURSE TIMETABLING (расписание занятий)

## 1.1. Жёсткие ограничения (Hard Constraints)

| # | Ограничение | Что запрещает |
|---|---|---|
| 1 | **Комната ≠ два класса одновременно** (`RoomConstraint`) | Два класса в одной комнате не пересекаются по времени. Исключение — `Can Share Room`, если суммарная вместимость хватает. |
| 2 | **Вместимость комнаты ≥ числа студентов** | Класс нельзя поставить в комнату меньшего размера — такие комнаты исключаются из домена. |
| 3 | **Инструктор ≠ два класса одновременно** (`InstructorConstraint`) | Классы одного инструктора не пересекаются. Back-to-back в слишком удалённых корпусах (> «prohibited»-порога) тоже запрещён жёстко. |
| 4 | **Required/Prohibited по ВРЕМЕНИ** | Required-время — единственно допустимое; Prohibited-время удаляется из домена класса. |
| 5 | **Required/Prohibited по КОМНАТЕ** | Аналогично для комнат. |
| 6 | **Обязательные distribution-ограничения** (`GroupConstraint`, уровень R/P) | Связи между классами, заданные как Required/Prohibited (см. §1.3). |
| 7 | **Доступность комнаты** | Недоступные интервалы комнаты моделируются как занятые → класс туда не встанет. |
| 8 | **Лимит набора класса/подчасти** (`ClassLimitConstraint`) | Запрещает размещения, при которых доступный лимит падает ниже требуемого. |
| 9 | **Flexible-ограничения в режиме Required** | `MaxDays`, `MaxHoles`, `MaxBreaks`, `MaxBlock`, `MaxConsecutiveDays`, `MaxHalfDays`, `MaxWeeks` — жёсткие, только если preference = Required. |

## 1.2. Мягкие критерии / целевая функция (Optimization Objectives)

Целевая функция = взвешенная сумма. Веса по умолчанию (ключи `Comparator.*Weight` из CPSolver):

### Основные критерии (активны по умолчанию)

| Критерий | Вес по умолч. | Что измеряет |
|---|---|---|
| **Student conflicts** (`StudentOverlapConflict`) | **1.0** | Конфликты студентов из-за пересечения классов по времени (базовый «вес студ. конфликта»). |
| **Hard student conflicts** (`StudentHardConflict`) | **5.0** | Студ. конфликты между классами без альтернатив секционирования. |
| **Committed student conflicts** | **1.0** | Конфликт с уже зафиксированным (committed) классом из другой задачи. |
| **Distance student conflicts** | **0.2** | Back-to-back классы в слишком удалённых комнатах (студент не успевает дойти). |
| **Work-day student conflicts** | **0.2** | Классы студента растягивают учебный день сверх нормы. |
| **Time preferences** | **1.0** | Нарушение мягких предпочтений по времени. |
| **Room preferences** | **1.0** | Нарушение мягких предпочтений по комнате. |
| **Distribution preferences** | **1.0** | Нарушение мягких distribution-ограничений. |
| **Too big rooms** | **0.1** | Комната существенно больше необходимой. |
| **Useless half-hours** | **0.1** | Пустые получасовые «дыры» в использовании комнаты. |
| **Broken time patterns** | **0.1** | Слоты, ломающие стандартные шаблоны MWF/TTh. |
| **Same-subpart balancing** | **1.0** (эфф. ×12) | Равномерное распределение классов одной подчасти по времени. |
| **Department balancing** | **1.0** (эфф. ×12) | Балансировка классов кафедры по непопулярным слотам. |
| **Back-to-back instructor prefs** | **1.0** | Штраф инструктору за смежные классы в удалённых корпусах (ниже prohibited-порога). |
| **Perturbations** (MPP) | **1.0** | Отличия текущего решения от исходного (только в режиме MPP). |

### Дополнительные критерии (opt-in, включаются в конфиге)

`ImportantStudentConflict` (×3), `InstructorConflict` (100), `InstructorStudentConflict` (×10), `InstructorFairness`, `InstructorLunchBreak` (0.3), `QuadraticStudentConflict` (совместная запись в квадрате), `RoomSizePenalty` (0.001), `StudentLunchBreak`, `StudentMinimizeDaysOfWeek` (0.05), `StudentMinimizeScheduleHoles` (0.05), `StudentOnlineConflict` (0.5), `StudentOverLunchConflict` (0.1).

## 1.3. Distribution Constraints — полный список (~50 типов)

Любой тип может быть **hard** (Required/Prohibited) или **soft** (Preferred/Discouraged). Полное перечисление — в `GroupConstraint.java`.

### По времени дня
- **Same Time** — то же время суток (независимо от дней).
- **Same Start** — тот же получасовой интервал начала.
- **Different Time** — не пересекаться по времени.
- **Different Time + Ignore Student Conflicts** — то же, но студ. конфликты игнорируются.
- **Spread In Time** — максимально разнести во времени (оптимизация).
- **N Hours Between** (`NHB`) — точный промежуток в один день: 90 мин, 1, 2, 3, 4, 4.5, 5, 6, 7, 8 часов.
- **At Least 1 Hour Between** (`NHB_GTE`) — минимум 1 час.
- **Less Than 6 Hours Between** (`NHB_LT`) — менее 6 часов.
- **Minimal Gap Between Classes** (`MIN_GAP(G)`) — минимум G минут между любыми двумя занятиями.

### Последовательность (Back-To-Back)
- **Back-To-Back Time** — соседние слоты, комнаты могут отличаться.
- **Back-To-Back & Same Room** — соседние слоты + та же комната.
- **Back-To-Back (min,max)** — кастомный зазор в слотах (с/без той же комнаты).
- **Precedence** — заданный порядок (первое заканчивается до начала следующего).
- **Back-To-Back Precedence** — порядок + соседние слоты в один день.

### По дням недели
- **Same Days** — одни и те же дни.
- **Back-To-Back Day** — соседние дни.
- **Next Day** (`FOLLOWING_DAY`) — на следующий день.
- **Two Days After** (`EVERY_OTHER_DAY`) — через день (Пн→Ср).
- **More Than 1 Day Between** — минимум 2 дня между.

### По комнатам
- **Same Room** — одна и та же комната.
- **Can Share Room** — можно делить комнату при достаточном размере.
- **Minimize Number Of Rooms Used** — минимизировать число комнат (оптимизация).
- **Online/Offline Room** — все занятия дня либо онлайн, либо офлайн.

### По неделям/датам
- **Same Weeks** — одинаковые недели (date pattern).
- **Back-To-Back Weeks** / **Following Weeks** — последовательные недели (с/без порядка).
- **Same Dates** / **Following Dates** — одинаковые/последовательные даты встреч.

### Комбинированные
- **Meet Together** — Can Share Room + Same Room + Same Time + Same Days.
- **Meet Together & Same Weeks**.
- **Same Days-Time**, **Same Days-Room-Time**, **Same Days-Room-Start**, **Same Days-Time-Weeks**.

### Ресурсные (студенты/преподаватели)
- **Same Instructor** — считаются как у одного препода (не пересекаются + учёт расстояния).
- **Same Students** / **Same Students No Distance**.
- **Linked Classes** — связанные секции разных курсов с одними студентами.
- **Children Cannot Overlap** — дочерние классы не пересекаются, если не пересекаются родительские.

### Нагрузка преподавателя за день/неделю (Additional Distribution Constraints)
- **At Most N Hours A Day** (`MAX_HRS_DAY`) — N = 3…10 часов.
- **Work Day** (`WORKDAY`) — промежуток первое–последнее занятие, N = 4…12 ч.
- **Max Block** (`MaxBlock:M:S`) — макс. длина блока подряд (M мин) с минимальным перерывом S.
- **Max Breaks** (`MaxBreaks:M:S`) — макс. число перерывов в день.
- **Max Holes** (`MaxHoles:H`) — макс. суммарное свободное время (дыры) за день.
- **Max Days** (`MaxDays:N`), **Max Half-Days** (`MaxHalfDays:N`), **Max Consecutive Days** (`MaxConsDays:N`), **Max Weeks** (`MaxWeeks:W:D`).
- **Max Days Range** / **Max Workdays Range** — в пределах N подряд (рабочих) дней.
- **Daybreak** (`DAYBREAK(H,D)`) — вечернее → утреннее на след. день.

### Оптимизационные (только preference)
- **Minimize Use Of 1h/2h/3h/5h Groups** — минимизировать число используемых временны́х групп.

---

# ЧАСТЬ 2. EXAMINATION TIMETABLING (расписание экзаменов)

## 2.1. Жёсткие ограничения (Hard Constraints)

| # | Ограничение | Описание |
|---|---|---|
| 1 | **One At A Time** | В одной комнате в один период — только один экзамен. |
| 2 | **Room Availability** | Комнату нельзя использовать в недоступные/prohibited периоды. |
| 3 | **Exam Availability** | Экзамен нельзя ставить в период/комнату, помеченные prohibited для него. |
| 4 | **Exam Size** | Σ вместимостей назначенных комнат ≥ числа студентов. |
| 5 | **Max Rooms** | Нельзя превысить макс. число комнат на экзамен (обычно 1–4). |
| 6 | **Hard Distributions** | Все required distribution-ограничения выполнены. |
| 7 | **Длина периода** | Период не короче длительности экзамена. |

> **Важно:** в модели UniTime **прямой конфликт студента (два экзамена в один период) — НЕ жёсткий**, а мягкий (в отличие от ITC2007). Для реального вуза полностью бесконфликтное расписание часто невозможно.

## 2.2. Мягкие критерии / целевая функция

Веса по умолчанию из базового конфига `exam-base` (в скобках — production-веса Purdue из статьи MISTA'13):

### Конфликты студентов
| Критерий | Вес | Описание |
|---|---|---|
| **Direct Conflict** | 1 000 000 (1000) | Студент: 2 экзамена в одном периоде. 3 экзамена = 3 конфликта. |
| **More Than Two A Day** | 10 000 (100) | Студент: ≥3 экзамена в один день. |
| **Back-To-Back Conflict** | 100 (10) | Студент: экзамены в двух последовательных периодах. |
| **Distance Back-To-Back** | 250 | Back-to-back + комнаты слишком далеко (порог ~670 м; по умолч. отключён `-1`). |

### Конфликты преподавателей (аналоги, по умолчанию почти нулевые)
`Instructor Direct` = 10, `Instructor More Than Two A Day` = 0, `Instructor Back-To-Back` = 0, `Instructor Distance B2B` = 0.

### Предпочтения и штрафы размещения
| Критерий | Вес | Формула / описание |
|---|---|---|
| **Period Penalty** | 1.0 | `2 × penalty_ep(e,p) + penalty_p(p)`. Штраф непопулярных периодов (поздний вечер, суббота). |
| **Room Penalty** | 1.0 | `Σ (2 × penalty_er + penalty_rp)`. Предпочтения по комнатам/зданиям/features. |
| **Distribution Penalty** | 1.0 | Нарушение мягкого distribution (1 = preferred, 4 = strongly). |
| **Room Split** | 10.0 | `(n−1)²`, n = число комнат: 1→0, 2→1, 3→4, 4→9. |
| **Room Split Distance** | 0.01 | Средняя попарная дистанция между комнатами разбитого экзамена. |
| **Room Size** | 0.001 | `(Σ вместимость − студенты)^f` — мелкие экзамены в мелкие комнаты. |
| **Rotation Penalty** | 0.0001 | `√(index(p) × average(e))` — «вращает» по времени по истории прошлых семестров. |
| **Large Exams Penalty** | 2 500 000 | Крупные экзамены (≥`LargeSize`=600 студ.) — в первые ~86% (`LargePeriod`=0.86) периодов. |
| **Room Distance** | 0.0001 | Тяга к «родной» комнате класса (−4 на неё, −1 на здание). |
| **Perturbation Penalty** | 0.001 | Минимум изменений при модификации готового расписания. |

**Иерархия важности:** Large Exams ≫ Direct Conflict ≫ More-Than-2-A-Day ≫ Distance B2B / B2B ≫ Room Split ≫ Period/Room preference ≫ Room Size ≫ Rotation / Perturbation.

## 2.3. Distribution Constraints для экзаменов
- **Same Period** / **Different Period** — в одних / разных периодах.
- **Same Room** / **Different Room** — в одних / разных комнатах.
- **Precedence** — экзамены в заданном порядке периодов.

> Связь по преподавателю/студентам — через конфликты (§2.2), а не отдельным типом. В новых версиях добавлен **room sharing** (несколько экзаменов в одной комнате по матрице совместимости).

---

# ЧАСТЬ 3. STUDENT SECTIONING (распределение студентов)

Распределение студентов по секциям при **уже готовом** расписании. Иерархия: **course → configuration → subpart → class**. Студент берёт по одному классу из каждой subpart **одной** выбранной конфигурации.

## 3.1. Жёсткие ограничения (Hard Constraints)

### Структура и время
- **Структура course→config→subpart→class** — нельзя смешивать классы из разных конфигураций; parent-child связи классов обязательны; классы внутри зачисления не пересекаются.
- **`StudentConflict`** — нельзя записать на две пересекающиеся секции (если overlap не разрешён явно).
- **`StudentNotAvailable`** — нельзя пересекаться с недоступностью студента (в т.ч. его TA-назначениями).
- **`FreeTimeConflicts`** — нельзя пересекаться с free-time более высокого приоритета.

### Лимиты
- **`SectionLimit`** / **`ConfigLimit`** / **`CourseLimit`** — суммарный вес зачислений не превышает лимит секции / конфигурации / курса (отрицательный лимит = безлимитно).

### Reservations (резервирования) и Restrictions (ограничения)
> **Reservation** резервирует *места* для группы. **Restriction** не резервирует места, но студент *обязан* следовать (напр., онлайн-студент → только онлайн-конфигурация).

- **`ReservationLimit`** — вместимость резервирования не превышается, места защищены.
- **`RequiredReservation`** — резервирования `mustBeUsed()` обязаны быть использованы.
- **`RequiredRestrictions`** / **`RequiredSections`** — зачисление обязано соответствовать restriction / включать обязательную секцию.

**Типы резервирований:** Individual (конкретный студент), Student Group (когорта/программа), Curriculum (учебный план), Course (места в offering), Override (индивидуальное исключение — поверх лимита/конфликта/закрытого класса). Параметры: Reserved Space, Expiration Date, Start Date, Restrictions, приоритет.

### Прочие hard
- **`LinkedSections`** — секции разных offerings, берущиеся только вместе.
- **`CancelledSections`** / **`DisabledSections`** — нельзя записать на отменённую/отключённую секцию.
- **Альтернативы** — alternative назначается только при неназначенном основном; нельзя быть записанным и на курс, и на его альтернативу.
- **`DependentCourses`** — пререквизиты/зависимости между курсами.
- **`HardDistanceConflicts`** — запрет back-to-back, если дистанция > `DistanceHardLimitInMinutes` (60) И время в пути > `AllowedDistanceInMinutes` (30).
- **`FixInitialAssignments`** / **`FixedAssignments`** — фиксация исходных назначений (MPP).

## 3.2. Мягкие критерии / целевая функция

Решатель **максимизирует суммарный вес студентов** (полное расписание = вес в [0,1], 1 = идеал). Default-реализация — **`PriorityStudentWeights`**.

- **Приоритет запросов** — `PriorityFactor ≈ 0.501`: каждый следующий запрос получает ~50% остатка бюджета. **Гарантия:** вес высокого приоритета > суммы всех низких → высоким приоритетом никогда не жертвуют.
- **Vital / Priority students** — vital-запросы назначаются первыми (до студ. приоритетов); затем спортсмены/оркестр, near-graduation (≥100 кр.), senior (≥60 кр.), остальные.
- **Штраф за альтернативность** — `weight ≈ 0.5^alt`: 1-я альтернатива ×0.501, 2-я ×0.251 и т.д. → брать первый выбор.
- **Distance conflicts** (`DistanceConflict.Factor` = 0.01; short-distance accommodation = 0.10) — back-to-back в зданиях, куда не успеть дойти.
- **Time overlaps** (`TimeOverlapFactor` = 0.5, лимит 0.5) — разрешённые пересечения секция-секция / секция-freetime / секция-unavailability.
- **Section balancing** (0.005) — равномерная загрузка параллельных секций (tie-breaker).
- **No-time sections** (0.01), **Selection/preferences** (0.10 — online/f2f, предпочтённые секции), **Reservation not followed** (0.10), **Student grouping** (когорты вместе), **MPP perturbation**, **`StudentQuality`** (обед, дорога, длина дня, ранние/поздние классы).

> **PriorityStudentWeights** (default) — вес по приоритету (первый запрос важнее). **EqualStudentWeights** — все primary-запросы весят одинаково.

## 3.3. Batch (offline) vs Online (real-time)

| | **Batch (пакетное)** | **Online (по одному)** |
|---|---|---|
| Когда | один раз в конце pre-registration, все студенты сразу | по одному, в порядке прихода (open registration) |
| Цель | глобальный оптимум, минимум неназначенных | лучшее для текущего студента, прошлых не трогаем |
| Алгоритм | iterative forward search + hill climbing / great deluge | branch-and-bound по запросам студента (~1 c) |
| Данные | + projected (last-like) спрос будущих студентов | предвычисленные Expected/Held Space из batch |
| Ключевой критерий | вычисляет **Held Space** и **Expected Space** для секций | **over-expected penalty** — защита мест под будущих студентов |

**Over-expected penalty** (online): `penalty = (Expected − Available) / limit`. Предпочитаются секции с **отрицательной** penalty (мест больше ожидаемого спроса). **Гарантия:** eligible-студента никогда не блокируют от курса из-за будущего спроса — penalty лишь направляет выбор секций. Реализация — `OverExpectedCriterion` (default `PercentageOverExpected`).

---

# Источники

**Документация UniTime:**
- https://help.unitime.org/types-of-distribution-preferences
- https://help.unitime.org/additional-distribution-constraints
- https://help.unitime.org/examination-timetabling , https://help.unitime.org/examination-solver
- https://help.unitime.org/student-scheduling , https://help.unitime.org/reservations
- https://help.unitime.org/solver , https://help.unitime.org/solver-parameters

**Исходный код (точные веса и классы):**
- https://github.com/UniTime/cpsolver — `coursett/constraint/GroupConstraint.java`, `coursett/criteria/*`, `studentsct/constraint/*`, `studentsct/weights/*`
- https://www.unitime.org/text.php?file=exam-base — конфиг экзаменационного решателя

**Научные статьи (Tomáš Müller):**
- PhD'05 — https://www.unitime.org/papers/phd05.pdf (полная модель, MPP)
- PATAT'05 — https://www.unitime.org/papers/patat05.pdf (Minimal Perturbation Problem)
- PATAT'08 — https://www.unitime.org/papers/patat08.pdf (student sectioning, batch vs online, over-expected)
- PATAT'24 — https://www.unitime.org/papers/patat24.pdf (современная реализация в Purdue)
- MISTA'13 — https://www.unitime.org/papers/mista13.pdf (экзамены: формулы, веса)
