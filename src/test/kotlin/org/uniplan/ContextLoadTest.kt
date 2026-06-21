package org.uniplan

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.uniplan.domain.University
import org.uniplan.domain.UniversityRepository

/**
 * Профиль dev → H2 (PostgreSQL mode). Поднимает весь контекст + JPA: проверяет, что
 * все сущности корректно мапятся и схема генерируется (без реального Postgres/Docker).
 */
@SpringBootTest
@ActiveProfiles("dev")
class ContextLoadTest {

    @Autowired
    lateinit var universities: UniversityRepository

    @Test
    fun contextLoadsAndPersists() {
        val saved = universities.save(University(name = "Тестовый вуз"))
        check(saved.id != null) { "Сущность должна получить id после save" }
        check(universities.count() == 1L) { "В таблице должна быть одна запись" }
    }
}
