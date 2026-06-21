package org.uniplan.domain

import jakarta.persistence.*

/** Вуз = ТЕНАНТ SaaS. INS-2: оргиерархия University → Institute → Department. */
@Entity
class University(
    var name: String = "",
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

/** Институт/факультет внутри вуза (напр. ИКМ). */
@Entity
class Institute(
    var name: String = "",
    var code: String = "",
    @ManyToOne(fetch = FetchType.LAZY) var university: University? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}

/** Кафедра или клиническая база. capacityGroupsParallel — ёмкость для цикловой ротации (R10). */
@Entity
class Department(
    var name: String = "",
    var code: String = "",
    @Enumerated(EnumType.STRING) var kind: DepartmentKind = DepartmentKind.ACADEMIC,
    var capacityGroupsParallel: Int? = null,
    @ManyToOne(fetch = FetchType.LAZY) var institute: Institute? = null,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
