package org.uniplan.scheduling

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.uniplan.domain.Course
import org.uniplan.domain.CourseOffering
import org.uniplan.domain.DatePattern
import org.uniplan.domain.DatePatternType
import org.uniplan.domain.Instructor
import org.uniplan.domain.Location
import org.uniplan.domain.LocationKind
import org.uniplan.domain.Section
import org.uniplan.domain.Term
import java.time.LocalDate

/**
 * Боевой адаптер Layer 2 на in-memory доменных сущностях (без БД).
 * Повторяет сценарии спайка, но через нашу доменную модель.
 */
class CoursettLayer2Test {

    private val termStart: LocalDate = LocalDate.of(2026, 2, 9) // пн, неделя 1
    private fun term() = Term(name = "весна 2026", startDate = termStart, endDate = termStart.plusWeeks(16))

    @Test
    fun `instructor and room share three disciplines with free time, no overlaps`() {
        val term = term()
        val room = Location(kind = LocationKind.ROOM, name = "Ауд-301", capacity = 30)
        val ivanov = Instructor(name = "Иванов")
        fun section(title: String, lenSlots: Int) = Section(
            offering = CourseOffering(course = Course(title = title)),
            datePattern = null,
            startSlot = null,           // свободный старт — решатель выбирает
            lengthSlots = lenSlots,
            weekdays = "MON",
            capacity = 25,
            instructors = mutableSetOf(ivanov),
            locations = mutableSetOf(room),
        )
        // 90 мин = 18 слотов, практика 4 ч = 48 слотов
        val sections = listOf(section("Анатомия", 18), section("Гистология", 18), section("Практика", 48))

        val result = CoursettLayer2().solve(term, sections)

        assertTrue(result.all { it.assigned }, "Все секции должны быть назначены: $result")
        val placed = result.filter { it.assigned }
        for (i in placed.indices) for (j in i + 1 until placed.size) {
            val a = placed[i]; val b = placed[j]
            val overlap = (a.dayCode and b.dayCode) != 0 &&
                a.startSlot < b.startSlot + b.lengthSlots && b.startSlot < a.startSlot + a.lengthSlots
            assertTrue(!overlap, "Общие препод./аудитория не должны пересекаться: $a vs $b")
        }
    }

    @Test
    fun `numerator denominator - two sections share one slot on complementary weeks`() {
        val term = term()
        val room = Location(kind = LocationKind.ROOM, name = "Ауд-204", capacity = 30)
        fun dp(label: String, weeks: List<Int>) = DatePattern(
            type = DatePatternType.ALTERNATE_WEEKS,
            label = label,
            dates = weeks.map { termStart.plusWeeks(it.toLong()) }.toMutableList(),
        )
        val odd = dp("нечёт", listOf(0, 2, 4, 6))
        val even = dp("чёт", listOf(1, 3, 5, 7))
        fun econ(name: String, pattern: DatePattern) = Section(
            offering = CourseOffering(course = Course(title = name)),
            datePattern = pattern,
            startSlot = 148,            // фикс. слот 12:20
            lengthSlots = 18,
            weekdays = "MON",
            capacity = 25,
            locations = mutableSetOf(room),
        )
        val sections = listOf(econ("Экономика нечёт", odd), econ("Экономика чёт", even))

        val result = CoursettLayer2().solve(term, sections)

        assertTrue(result.all { it.assigned }, "Обе секции должны разместиться (разные недели): $result")
        assertTrue(result.all { it.startSlot == 148 }, "Обе должны быть в 12:20 (слот 148): $result")
    }
}
