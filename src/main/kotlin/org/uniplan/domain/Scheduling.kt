package org.uniplan.domain

import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.Instant

/** Слот «удобно/неудобно» преподавателя + воркфлоу одобрения (INS-4). */
@Entity
class InstructorAvailability(
    @ManyToOne(fetch = FetchType.LAZY) var instructor: Instructor? = null,
    @ManyToOne(fetch = FetchType.LAZY) var term: Term? = null,
    @Enumerated(EnumType.STRING) var weekday: DayOfWeek = DayOfWeek.MONDAY,
    var fromSlot: Int = 0,
    var toSlot: Int = 0,
    @Enumerated(EnumType.STRING) var preference: Preference = Preference.NEUTRAL,
    @Enumerated(EnumType.STRING) var status: AvailabilityStatus = AvailabilityStatus.PROPOSED,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

/** [L1] Разбиение дисциплины по кафедрам с диапазоном номеров групп (R11). */
@Entity
class DisciplineAssignment(
    @ManyToOne(fetch = FetchType.LAZY) var offering: CourseOffering? = null,
    @ManyToOne(fetch = FetchType.LAZY) var department: Department? = null,
    var groupRangeFrom: String = "",     // "1.4.01"
    var groupRangeTo: String = "",       // "1.4.39"
    var capacityGroupsParallel: Int = 0,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

/** [L1] Ядро ротации: кто-куда-когда. Вход в Режиме A / выход в Режиме B. */
@Entity
class RotationAssignment(
    @ManyToOne(fetch = FetchType.LAZY) var term: Term? = null,
    @ManyToOne(fetch = FetchType.LAZY) var block: Block? = null,
    @ManyToOne(fetch = FetchType.LAZY) var group: Group? = null,
    @ManyToOne(fetch = FetchType.LAZY) var offering: CourseOffering? = null,
    @ManyToOne(fetch = FetchType.LAZY) var department: Department? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

/** Версия расписания терма. basedOnVersion → MPP «минимальные изменения» (INS-1). */
@Entity
class ScheduleVersion(
    @ManyToOne(fetch = FetchType.LAZY) var term: Term? = null,
    var label: String = "",
    @Enumerated(EnumType.STRING) var status: VersionStatus = VersionStatus.DRAFT,
    var createdAt: Instant = Instant.EPOCH,
    @ManyToOne(fetch = FetchType.LAZY) var basedOnVersion: ScheduleVersion? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

/** Результат решателя в рамках версии: зафиксированное размещение секции. */
@Entity
@Table(name = "schedule_assignment")
class Assignment(
    @ManyToOne(fetch = FetchType.LAZY) var version: ScheduleVersion? = null,
    @ManyToOne(fetch = FetchType.LAZY) var section: Section? = null,
    var startSlot: Int? = null,
    var lengthSlots: Int? = null,
    var weekdays: String? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
