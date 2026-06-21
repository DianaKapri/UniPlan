package org.uniplan.spike

import org.cpsolver.ifs.example.csp.CSPModel
import org.cpsolver.ifs.example.csp.CSPValue
import org.cpsolver.ifs.example.csp.CSPVariable
import org.cpsolver.ifs.solution.Solution
import org.cpsolver.ifs.solver.Solver
import org.cpsolver.ifs.util.DataProperties

/**
 * UniPlan · Спайк-0 (Неделя 1).
 *
 * Цель: подтвердить, что цепочка Gradle → зависимость CPSolver (1.4.91) → движок IFS
 * реально заводится и решает задачу из НАШЕГО Kotlin-кода. Это де-рискует Strategy A
 * (depend + extend CPSolver) на уровне toolchain + движок.
 *
 * Здесь используется готовая модель-пример CSP из CPSolver — наш код лишь конфигурирует
 * и драйвит решатель. Следующие шаги: (1) собственная мини-модель Layer 1
 * (курсы → блоки, не более одного курса в блоке), (2) coursett для Layer 2.
 */
fun main() {
    println("UniPlan · Спайк-0: запуск движка CPSolver (IFS) из нашего кода")

    val cfg = DataProperties().apply {
        setProperty("Termination.Class", "org.cpsolver.ifs.termination.GeneralTerminationCondition")
        setProperty("Termination.TimeOut", "10")
        setProperty("Termination.StopWhenComplete", "true") // остановиться, как только найдено полное решение
        setProperty("Comparator.Class", "org.cpsolver.ifs.solution.GeneralSolutionComparator")
        setProperty("Value.Class", "org.cpsolver.ifs.heuristics.GeneralValueSelection")
        setProperty("Variable.Class", "org.cpsolver.ifs.heuristics.GeneralVariableSelection")
        setProperty("Extensions.Classes", "org.cpsolver.ifs.extension.ConflictStatistics")
    }

    // Игрушечная, заведомо РЕШАЕМАЯ CSP: 10 переменных, домен 10, 8 ограничений,
    // 60 совместимых пар из 100 (свободная) — поиск должен найти полное решение.
    val model = CSPModel(10, 10, 8, 60, 1234L)

    val solver = Solver<CSPVariable, CSPValue>(cfg)
    solver.setInitalSolution(model) // (sic — опечатка в API библиотеки)
    solver.start()
    solver.solverThread.join()

    val solution: Solution<CSPVariable, CSPValue> = solver.lastSolution()
    solution.restoreBest()

    val assignment = solution.assignment
    val assigned = assignment.nrAssignedVariables()
    val total = model.variables().size
    println("Итераций: ${solution.iteration}, время: ${"%.2f".format(solution.time)} c")
    println("Назначено переменных: $assigned из $total")
    println("Значение целевой функции: ${model.getTotalValue(assignment)}")

    if (assigned == total) {
        println("✅ Движок IFS запустился и нашёл полное решение — toolchain + CPSolver работают.")
    } else {
        println("⚠️ Решение неполное ($assigned/$total) — движок работает, но задача недонапряжена/переопределена.")
    }
}
