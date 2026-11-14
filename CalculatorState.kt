package com.example.kalkuleitor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue

/**
 * Состояние калькулятора, отделённое от интерфейса.
 *
 * Хранит выражение, результат и признак «уже вычислено», а всю логику нажатий
 * реализует метод [press]. Поэтому UI-файл занимается только отрисовкой.
 */
class CalculatorState(
    initialExpression: String = "",
    initialResult: String = "",
    initialEvaluated: Boolean = false,
) {
    /** Текущее введённое выражение (то, что набирает пользователь). */
    var expression by mutableStateOf(initialExpression)
        private set

    /** Готовый результат (показывается крупно, когда [evaluated] = true). */
    var result by mutableStateOf(initialResult)
        private set

    /** Флаг: последним действием было нажатие «=». */
    var evaluated by mutableStateOf(initialEvaluated)
        private set

    /** Живой предпросмотр результата, пока пользователь вводит выражение. */
    private val preview: String
        get() = if (!evaluated && expression.isNotBlank()) {
            runCatching { CalculatorEngine.evaluate(expression) }
                .getOrNull()
                ?.let { "= " + CalculatorEngine.format(it) }
                ?: ""
        } else {
            ""
        }

    /** Верхняя (мелкая) строка дисплея. */
    val smallText: String
        get() = if (evaluated) expression else preview

    /** Нижняя (крупная) строка дисплея. */
    val bigText: String
        get() = if (evaluated) result else expression.ifBlank { "0" }

    /** Полный сброс: очищает выражение, результат и флаг вычисления. */
    fun clearAll() {
        expression = ""
        result = ""
        evaluated = false
    }

    /** Обрабатывает нажатие кнопки с подписью [label]. */
    fun press(label: String) {
        when {
            label == "C" -> clearAll()
            label == "⌫" -> backspace()
            label == "=" -> evaluate()
            label == "." -> appendDecimal()
            label == "(" || label == ")" -> appendParenthesis(label)
            label == "x²" -> appendSquare()
            label == "%" -> appendPercent()
            label == "√" -> appendFunction("√(")
            label == "sin" || label == "cos" || label == "tan" ||
                label == "ln" || label == "log" -> appendFunction("$label(")
            label == "π" -> appendDigit("π")
            label in operators -> appendOperator(label)
            else -> appendDigit(label) // цифры
        }
    }

    /** После «=» новое число должно начинать выражение с нуля. */
    private fun startFreshIfNeeded() {
        if (evaluated) {
            expression = ""
            result = ""
            evaluated = false
        }
    }

    /** Добавляет цифру или константу (π) в выражение. */
    private fun appendDigit(text: String) {
        startFreshIfNeeded()
        expression += text
    }

    /**
     * Добавляет оператор с защитой от лишних символов:
     * если последним уже введён оператор — он заменяется,
     * после «(» разрешён только минус, пустое выражение начинается с минуса.
     */
    private fun appendOperator(operator: String) {
        if (evaluated) {
            expression = if (result.isNotEmpty() && result.first().isDigit()) result else ""
            result = ""
            evaluated = false
        }
        if (expression.isEmpty()) {
            if (operator == "−") expression = "−"
            return
        }
        when {
            expression.last() in "+−×÷^" -> expression = expression.dropLast(1) + operator
            expression.last() == '(' -> if (operator == "−") expression += operator
            else -> expression += operator
        }
    }

    /** Добавляет десятичную точку, не допуская двух точек в одном числе. */
    private fun appendDecimal() {
        startFreshIfNeeded()
        when {
            expression.isEmpty() -> expression = "0."
            expression.last() in "+−×÷^(" -> expression += "0."
            expression.last().isDigit() -> {
                val currentNumber = expression.takeLastWhile { it.isDigit() || it == '.' }
                if (!currentNumber.contains('.')) expression += "."
            }
            // после «)», «%» точка не добавляется
        }
    }

    /** Добавляет функцию (например «sin(») в выражение. */
    private fun appendFunction(token: String) {
        startFreshIfNeeded()
        expression += token
    }

    /**
     * Добавляет скобку:
     * «(» ставится всегда, «)» — только если есть незакрытая «(» и выражение
     * не заканчивается оператором.
     */
    private fun appendParenthesis(parenthesis: String) {
        if (parenthesis == "(") {
            startFreshIfNeeded()
            expression += "("
            return
        }
        val opened = expression.count { it == '(' } - expression.count { it == ')' }
        if (opened > 0 && expression.isNotEmpty() && expression.last() !in "+−×÷^(") {
            expression += ")"
        }
    }

    /** Возведение в квадрат: дописывает «^2» к текущему числу или скобке. */
    private fun appendSquare() {
        if (evaluated) {
            expression = if (result.firstOrNull()?.isDigit() == true) result else ""
            result = ""
            evaluated = false
        }
        if (expression.isNotEmpty() && expression.last() !in "+−×÷^(") {
            expression += "^2"
        }
    }

    /** Добавляет процент «%» к текущему числу или скобке. */
    private fun appendPercent() {
        startFreshIfNeeded()
        if (expression.isNotEmpty() && expression.last() !in "+−×÷^(") {
            expression += "%"
        }
    }

    /** Удаляет последний символ (или снимает режим показа результата). */
    private fun backspace() {
        if (evaluated) {
            evaluated = false
            result = ""
        } else {
            expression = expression.dropLast(1)
        }
    }

    /**
     * Вычисляет выражение и показывает результат.
     * При ошибке дисплей очищается и показывается сообщение об ошибке.
     */
    private fun evaluate() {
        if (expression.isBlank()) return
        try {
            result = CalculatorEngine.format(CalculatorEngine.evaluate(expression))
        } catch (e: CalcException) {
            expression = ""
            result = e.message ?: "Ошибка"
        } catch (e: Exception) {
            expression = ""
            result = "Ошибка"
        }
        evaluated = true
    }

    companion object {
        /** Операторы, которые подсвечиваются на клавиатуре как математические. */
        val operators = setOf("+", "−", "×", "÷", "^")

        /** Saver, чтобы состояние переживало пересоздание Activity. */
        val Saver: Saver<CalculatorState, Any> = listSaver(
            save = { listOf(it.expression, it.result, it.evaluated) },
            restore = { saved ->
                CalculatorState(
                    initialExpression = saved[0] as String,
                    initialResult = saved[1] as String,
                    initialEvaluated = saved[2] as Boolean,
                )
            }
        )
    }
}
