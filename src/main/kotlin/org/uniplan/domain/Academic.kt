package org.uniplan.domain

import jakarta.persistence.*

@Entity
class Course(
    var code: String = "",
    var title: String = "",
    var courseYear: Int = 1,   // 1..6 → 1–3 SPAN, 4–6 BLOCK, 3 переходный
    @ManyToOne(fetch = FetchType.LAZY) var institute: Institute? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

@Entity
class CourseOffering(
    @Enumerated(EnumType.STRING) var scope: CourseScope = CourseScope.SPAN,
    var durationDays: Int? = null,        // [L1] длина прохода (интенсив=1)
    var groupsParallel: Int? = null,      // [L1] сколько групп одновременно
    @Enumerated(EnumType.STRING) var delivery: Delivery = Delivery.IN_PERSON,
    @ManyToOne(fetch = FetchType.LAZY) var course: Course? = null,
    @ManyToOne(fetch = FetchType.LAZY) var term: Term? = null,
    @ManyToMany var allowedBlocks: MutableSet<Block> = mutableSetOf(),
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

/** Академическая ГРУППА — атомарная единица планирования (не студент!). */
@Entity
@Table(name = "academic_group")
class Group(
    var code: String = "",        // "1.4.01"
    var courseYear: Int = 1,
    var size: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY) var institute: Institute? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

/** [L2] Секция = то, что ставится в сетку (вход coursett). */
@Entity
class Section(
    @ManyToOne(fetch = FetchType.LAZY) var offering: CourseOffering? = null,
    @ManyToOne(fetch = FetchType.LAZY) var block: Block? = null,            // null для SPAN
    @ManyToOne(fetch = FetchType.LAZY) var datePattern: DatePattern? = null,
    var startSlot: Int? = null,        // свободное время, 5-мин слоты; null = решатель выберет
    var lengthSlots: Int? = null,
    var weekdays: String = "",
    var capacity: Int = 0,
    @ManyToMany var instructors: MutableSet<Instructor> = mutableSetOf(),
    @ManyToMany var locations: MutableSet<Location> = mutableSetOf(),       // [] если ONLINE
    @ManyToMany var groups: MutableSet<Group> = mutableSetOf(),
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
