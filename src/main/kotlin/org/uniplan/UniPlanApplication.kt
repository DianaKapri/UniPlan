package org.uniplan

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UniPlanApplication

fun main(args: Array<String>) {
    runApplication<UniPlanApplication>(*args)
}
