package org.uniplan.domain

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "academic_term")
class Term(
    var name: String = "",
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    @ManyToOne(fetch = FetchType.LAZY) var university: University? = null,
    @ElementCollection var holidays: MutableList<LocalDate> = mutableListOf(),
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

/** Блок цикловой системы: упорядоченный кусок терма со своей дневной сеткой (ось 1). */
@Entity
class Block(
    var sequence: Int = 0,
    var name: String? = null,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    var lengthDays: Int = 0,
    var weekdays: String = "MON,TUE,WED,THU,FRI",
    var workFrom: LocalTime = LocalTime.of(8, 0),
    var workTo: LocalTime = LocalTime.of(21, 0),
    @ManyToOne(fetch = FetchType.LAZY) var term: Term? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

/** Набор ДАТ, когда занятие реально идёт (числитель/знаменатель, вычет праздников). */
@Entity
class DatePattern(
    @Enumerated(EnumType.STRING) var type: DatePatternType = DatePatternType.STANDARD,
    var label: String? = null,
    @ElementCollection var dates: MutableList<LocalDate> = mutableListOf(),
    @ManyToOne(fetch = FetchType.LAZY) var term: Term? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
