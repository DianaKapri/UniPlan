package org.uniplan.domain

enum class DepartmentKind { ACADEMIC, CLINICAL_BASE }

enum class DatePatternType { STANDARD, ALTERNATE_WEEKS, CUSTOM }

/** Недельная (младшие курсы) vs блочная (старшие курсы) система. */
enum class CourseScope { SPAN, BLOCK }

enum class Delivery { IN_PERSON, ONLINE }

enum class LocationKind { ROOM, LAB, CLINICAL_BASE, ONLINE }

/** Шкала предпочтений UniTime (−4..+4 / Required / Prohibited). */
enum class Preference {
    REQUIRED, STRONGLY_PREFERRED, PREFERRED, NEUTRAL, DISCOURAGED, STRONGLY_DISCOURAGED, PROHIBITED
}

/** Статус заявки преподавателя на доступность (воркфлоу одобрения, INS-4). */
enum class AvailabilityStatus { PROPOSED, APPROVED, REJECTED }

enum class VersionStatus { DRAFT, PUBLISHED }
