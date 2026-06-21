package org.uniplan.spike

import org.cpsolver.coursett.constraint.InstructorConstraint
import org.cpsolver.coursett.constraint.RoomConstraint
import org.cpsolver.coursett.model.Lecture
import org.cpsolver.coursett.model.Placement
import org.cpsolver.coursett.model.RoomLocation
import org.cpsolver.coursett.model.TimeLocation
import org.cpsolver.coursett.model.TimetableModel
import org.cpsolver.ifs.assignment.Assignment
import org.cpsolver.ifs.solver.Solver
import org.cpsolver.ifs.util.DataProperties
import java.util.BitSet

/**
 * UniPlan · Спайк Layer 2 — адаптер «наш домен → coursett».
 *
 * Цель (главный отложенный де-риск Strategy A): доказать, что курсовой решатель CPSolver
 * (`coursett`) можно построить из наших данных и получить расписание «время + аудитория»
 * со СВОБОДНЫМ временем (нестандартный старт 8:30, переменная длительность).
 *
 * Кодировка (из org.cpsolver.coursett.Constants):
 *   дни: Mon=64, Tue=32, Wed=16, Thu=8, Fri=4;  слот = 5 мин от полуночи (8:30 → слот 102).
 */

private const val MON = 64
private const val TUE = 32
private const val WED = 16
private const val THU = 8
private const val FRI = 4

private fun slot(h: Int, m: Int) = (h * 60 + m) / 5

private fun fullWeeks(n: Int): BitSet = BitSet().apply { set(0, n) }
private fun someWeeks(indices: IntProgression): BitSet = BitSet().apply { indices.forEach { set(it) } }

private fun cfg(): DataProperties = DataProperties().apply {
    setProperty("Termination.Class", "org.cpsolver.ifs.termination.GeneralTerminationCondition")
    setProperty("Termination.TimeOut", "10")
    setProperty("Termination.StopWhenComplete", "true")
    setProperty("Comparator.Class", "org.cpsolver.ifs.solution.GeneralSolutionComparator")
    setProperty("Value.Class", "org.cpsolver.ifs.heuristics.GeneralValueSelection")
    setProperty("Variable.Class", "org.cpsolver.ifs.heuristics.GeneralVariableSelection")
    setProperty("Extensions.Classes", "org.cpsolver.ifs.extension.ConflictStatistics")
}

/** TimeLocation для свободного времени: произвольный старт + произвольная длительность в минутах. */
private fun time(dayCode: Int, h: Int, m: Int, lengthMin: Int, week: BitSet): TimeLocation =
    TimeLocation(dayCode, slot(h, m), lengthMin / 5, 0, 0.0, 0, 1L, "block", week, 0)

private fun decodeDays(dayCode: Int): String =
    listOf("Пн" to MON, "Вт" to TUE, "Ср" to WED, "Чт" to THU, "Пт" to FRI)
        .filter { (dayCode and it.second) != 0 }
        .joinToString("") { it.first }

/** При nrRooms=1 coursett хранит комнату в getRoomLocation(); множественный список — null. */
private fun roomsOf(p: Placement): String {
    val multi: List<RoomLocation>? = p.roomLocations
    if (!multi.isNullOrEmpty()) return multi.joinToString(", ") { it.name }
    return p.roomLocation?.name ?: "—"
}

private fun solve(props: DataProperties, model: TimetableModel): Assignment<Lecture, Placement> {
    val solver = Solver<Lecture, Placement>(props)
    solver.setInitalSolution(model)
    solver.start()
    solver.solverThread.join()
    val solution = solver.lastSolution()
    solution.restoreBest()
    return solution.assignment
}

private fun printResult(title: String, model: TimetableModel, a: Assignment<Lecture, Placement>) {
    println("— $title —")
    var assigned = 0
    for (lecture in model.variables()) {
        val p = a.getValue(lecture)
        if (p == null) {
            println("  ${lecture.name}: НЕ НАЗНАЧЕНО")
            continue
        }
        assigned++
        val t = p.timeLocation
        val startMin = t.startSlot * 5
        val endMin = startMin + t.length * 5
        val from = "%02d:%02d".format(startMin / 60, startMin % 60)
        val to = "%02d:%02d".format(endMin / 60, endMin % 60)
        val weeks = t.weekCode.cardinality()
        println("  ${lecture.name}: ${decodeDays(t.dayCode)} $from–$to, ауд. ${roomsOf(p)} (недель: $weeks)")
    }
    println("  → назначено ${assigned}/${model.variables().size}")
}

/** Сценарий 1: один преподаватель + одна аудитория делят 3 дисциплины. Свободное время. */
private fun scenarioInstructorRoom() {
    val props = cfg()
    val model = TimetableModel(props)
    val week = fullWeeks(28)

    val room = RoomConstraint(1L, "Ауд-301", null, 30, null, null, null, true, true)
    val instr = InstructorConstraint(1L, "i1", "Иванов", true)
    model.addConstraint(room)
    model.addConstraint(instr)
    val roomLoc = RoomLocation(1L, "Ауд-301", null, 0, 30, null, null, true, room)

    data class Spec(val id: Long, val name: String, val times: List<TimeLocation>)
    val specs = listOf(
        Spec(101, "Анатомия (90 мин)", listOf(time(MON, 8, 30, 90, week), time(WED, 8, 30, 90, week), time(MON, 10, 30, 90, week))),
        Spec(102, "Гистология (90 мин)", listOf(time(MON, 8, 30, 90, week), time(WED, 8, 30, 90, week), time(THU, 13, 0, 90, week))),
        Spec(103, "Практика в клинике (4 ч)", listOf(time(TUE, 8, 0, 240, week), time(THU, 8, 0, 240, week))),
    )
    for (s in specs) {
        val lecture = Lecture(s.id, 1L, s.id, s.name, s.times, listOf(roomLoc), 1, null, 25, 25, 1.0)
        model.addVariable(lecture)
        room.addVariable(lecture)
        instr.addVariable(lecture)
    }
    printResult("Сценарий 1: 1 преподаватель + 1 аудитория, свободное время", model, solve(props, model))
}

/** Сценарий 2: числитель/знаменатель — две секции в ОДНОМ слоте, но на комплементарных неделях. */
private fun scenarioAlternatingWeeks() {
    val props = cfg()
    val model = TimetableModel(props)
    val odd = someWeeks(1..27 step 2)   // нечётные недели
    val even = someWeeks(0..26 step 2)  // чётные недели

    val room = RoomConstraint(2L, "Ауд-204", null, 30, null, null, null, true, true)
    model.addConstraint(room)
    val roomLoc = RoomLocation(2L, "Ауд-204", null, 0, 30, null, null, true, room)

    // Обе — Пн 12:20, та же аудитория. Если конфликты week-aware → обе помещаются (не пересекаются по неделям).
    val a = Lecture(201, 1L, 201, "Экономика (нечёт. недели)", listOf(time(MON, 12, 20, 90, odd)), listOf(roomLoc), 1, null, 25, 25, 1.0)
    val b = Lecture(202, 1L, 202, "Экономика (чёт. недели)", listOf(time(MON, 12, 20, 90, even)), listOf(roomLoc), 1, null, 25, 25, 1.0)
    for (l in listOf(a, b)) {
        model.addVariable(l)
        room.addVariable(l)
    }
    printResult("Сценарий 2: числитель/знаменатель (одна аудитория, нечёт/чёт недели)", model, solve(props, model))
}

fun main() {
    println("UniPlan · Спайк Layer 2: адаптер домен → coursett\n")
    scenarioInstructorRoom()
    println()
    scenarioAlternatingWeeks()
}
