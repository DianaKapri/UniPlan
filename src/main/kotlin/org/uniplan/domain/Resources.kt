package org.uniplan.domain

import jakarta.persistence.*

/** Аудитория / лаборатория / клин. база / онлайн. INS-3: владелец + флаг shared. */
@Entity
class Location(
    @Enumerated(EnumType.STRING) var kind: LocationKind = LocationKind.ROOM,
    var name: String = "",
    var capacity: Int = 0,
    @ManyToOne(fetch = FetchType.LAZY) var ownerDepartment: Department? = null,
    var shared: Boolean = false,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

@Entity
class Instructor(
    var name: String = "",
    @ManyToOne(fetch = FetchType.LAZY) var ownerDepartment: Department? = null,
    var shared: Boolean = false,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
