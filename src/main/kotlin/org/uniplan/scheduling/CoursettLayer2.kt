package org.uniplan.scheduling

import org.cpsolver.coursett.constraint.InstructorConstraint
import org.cpsolver.coursett.constraint.RoomConstraint
import org.cpsolver.coursett.model.Lecture
import org.cpsolver.coursett.model.Placement
import org.cpsolver.coursett.model.RoomLocation
import org.cpsolver.coursett.model.TimeLocation
import org.cpsolver.coursett.model.TimetableModel
import org.cpsolver.ifs.solver.Solver
import org.cpsolver.ifs.util.DataProperties
import org.uniplan.domain.Delivery
import org.uniplan.domain.Instructor
import org.uniplan.domain.Location
import org.uniplan.domain.Section
import org.uniplan.domain.Term
import org.uniplan.domain.DatePattern
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.BitSet

/**
 * Боевой адаптер Layer 2: строит модель coursett из доменных сущностей и решает
 * расписание «время + аудитория» внутри блока/недели.
 *
 * Чистый класс (без Spring) — тестируется на in-memory сущностях без БД. Обёртку-сервис,
 * читающую/пишущую БД, навесим отдельно (WBS 3.2/3.4).
 *
 * Кодировка coursett: дни Mon=64..Fri=4; слот = 5 мин от полуночи (12 слотов в часе).
 */
class CoursettLayer2 {

    data class Params(
        val windowFromSlot: Int = 8 * 12,   // 08:00
        val windowToSlot: Int = 21 * 12,     // 21:00
        val stepSlots: Int = 6,              // шаг 30 мин при свободном старте
        val timeoutSeconds: Int = 10,
    )

    /** Результат: размещение секции (день/старт/длительность/аудитории) или флаг «не назначено». */
    data class SectionPlacement(
        val section: Section,
        val assigned: Boolean,
        val dayCode: Int,
        val startSlot: Int,
        val lengthSlots: Int,
        val locations: List<String>,
    )

    private val dayBits = mapOf("MON" to 64, "TUE" to 32, "WED" to 16, "THU" to 8, "FRI" to 4, "SAT" to 2, "SUN" to 1)

    fun solve(term: Term, sections: List<Section>, params: Params = Params()): List<SectionPlacement> {
        val props = cfg(params.timeoutSeconds)
        val model = TimetableModel(props)
        val termStart = requireNotNull(term.startDate) { "Term.startDate обязателен" }

        // Проход 1: ресурсы → ограничения coursett.
        val roomConstraint = HashMap<Long, RoomConstraint>()
        val roomLocation = HashMap<Long, RoomLocation>()
        val instrConstraint = HashMap<Long, InstructorConstraint>()
        for (loc in sections.flatMap { it.locations }.distinct()) {
            val key = idOf(loc)
            val rc = RoomConstraint(key, loc.name, null, loc.capacity, null, null, null, true, true)
            model.addConstraint(rc)
            roomConstraint[key] = rc
            roomLocation[key] = RoomLocation(key, loc.name, null, 0, loc.capacity, null, null, true, rc)
        }
        for (ins in sections.flatMap { it.instructors }.distinct()) {
            val key = idOf(ins)
            val ic = InstructorConstraint(key, "i$key", ins.name, true)
            model.addConstraint(ic)
            instrConstraint[key] = ic
        }

        // Проход 2: секции → переменные (Lecture) с доменом «время × аудитория».
        val lectureToSection = HashMap<Lecture, Section>()
        var lid = 1L
        for (s in sections) {
            val dayCode = parseDays(s.weekdays)
            val length = requireNotNull(s.lengthSlots) { "Section.lengthSlots обязателен (секция id=${s.id})" }
            val week = weekCode(s.datePattern, termStart)
            val times = candidateTimes(s, dayCode, length, week, params)
            val online = s.locations.isEmpty() || s.offering?.delivery == Delivery.ONLINE
            val rooms = if (online) emptyList() else s.locations.map { roomLocation.getValue(idOf(it)) }
            val cap = if (s.capacity > 0) s.capacity else 1
            val lecture = Lecture(lid++, 1L, idOf(s), sectionName(s), times, rooms, if (online) 0 else 1, null, cap, cap, 1.0)
            model.addVariable(lecture)
            lectureToSection[lecture] = s
            if (!online) s.locations.forEach { roomConstraint.getValue(idOf(it)).addVariable(lecture) }
            s.instructors.forEach { instrConstraint.getValue(idOf(it)).addVariable(lecture) }
        }

        // Решение.
        val solver = Solver<Lecture, Placement>(props)
        solver.setInitalSolution(model)
        solver.start()
        solver.solverThread.join()
        val solution = solver.lastSolution()
        solution.restoreBest()
        val a = solution.assignment

        return model.variables().map { lecture ->
            val s = lectureToSection.getValue(lecture)
            when (val p = a.getValue(lecture)) {
                null -> SectionPlacement(s, false, 0, 0, 0, emptyList())
                else -> {
                    val t = p.timeLocation
                    SectionPlacement(s, true, t.dayCode, t.startSlot, t.length, roomNames(p))
                }
            }
        }
    }

    private fun candidateTimes(s: Section, dayCode: Int, length: Int, week: BitSet, p: Params): List<TimeLocation> {
        val starts = s.startSlot?.let { listOf(it) }
            ?: (p.windowFromSlot..(p.windowToSlot - length) step p.stepSlots).toList()
        return starts.map { TimeLocation(dayCode, it, length, 0, 0.0, 0, 1L, "dp", week, 0) }
    }

    /** weekCode: бит = день терма (от startDate). Непересекающиеся паттерны → нет конфликта (числ./знам.). */
    private fun weekCode(dp: DatePattern?, termStart: LocalDate): BitSet {
        val bs = BitSet()
        if (dp == null || dp.dates.isEmpty()) {
            bs.set(0, 200) // нет паттерна → «все недели» (секции пересекаются по неделям как обычно)
        } else {
            dp.dates.forEach { bs.set(ChronoUnit.DAYS.between(termStart, it).toInt()) }
        }
        return bs
    }

    private fun parseDays(weekdays: String): Int =
        weekdays.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
            .fold(0) { acc, d -> acc or (dayBits[d] ?: 0) }

    private fun roomNames(p: Placement): List<String> {
        val multi: List<RoomLocation>? = p.roomLocations
        if (!multi.isNullOrEmpty()) return multi.map { it.name }
        return p.roomLocation?.let { listOf(it.name) } ?: emptyList()
    }

    private fun sectionName(s: Section): String = s.offering?.course?.title ?: "Секция ${idOf(s)}"

    // Стабильный Long-id для coursett: реальный id сущности либо синтетический (для in-memory объектов).
    private val synth = HashMap<Any, Long>()
    private var synthSeq = 1_000_000L
    private fun idOf(x: Any): Long = when (x) {
        is Location -> x.id ?: synth.getOrPut(x) { synthSeq++ }
        is Instructor -> x.id ?: synth.getOrPut(x) { synthSeq++ }
        is Section -> x.id ?: synth.getOrPut(x) { synthSeq++ }
        else -> synth.getOrPut(x) { synthSeq++ }
    }

    private fun cfg(timeout: Int) = DataProperties().apply {
        setProperty("Termination.Class", "org.cpsolver.ifs.termination.GeneralTerminationCondition")
        setProperty("Termination.TimeOut", timeout.toString())
        setProperty("Termination.StopWhenComplete", "true")
        setProperty("Comparator.Class", "org.cpsolver.ifs.solution.GeneralSolutionComparator")
        setProperty("Value.Class", "org.cpsolver.ifs.heuristics.GeneralValueSelection")
        setProperty("Variable.Class", "org.cpsolver.ifs.heuristics.GeneralVariableSelection")
        setProperty("Extensions.Classes", "org.cpsolver.ifs.extension.ConflictStatistics")
    }
}
