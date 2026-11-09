package com.example.kalkuleitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kalkuleitor.ui.theme.KalkuleitorTheme

private val operators = setOf("+", "−", "×", "÷")

private val buttonRows = listOf(
    listOf("sin", "cos", "tan" , "C"),
    listOf("√", "x²", "π", "⌫"),
    listOf("7", "8", "9", "÷"),
    listOf("4", "5", "6", "×"),
    listOf("1", "2", "3", "−"),
    listOf("(", ")", ".", "+"),
    listOf("%", "0", "="),
)

/**
 * Основной экран калькулятора: дисплей и клавиатура.
 * Хранит текущее выражение, результат и флаг «уже вычислено».
 */
@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    // Текущее введённое выражение (то, что набирает пользователь).
    var expression by rememberSaveable { mutableStateOf("") }
    // Результат вычисления (показывается крупно, если evaluated = true).
    var result by rememberSaveable { mutableStateOf("") }
    // Флаг: последним действием было нажатие «=».
    var evaluated by rememberSaveable { mutableStateOf(false) }

    // Живой предпросмотр результата, пока пользователь вводит выражение.
    val preview = if (!evaluated && expression.isNotBlank()) {
        runCatching { CalculatorEngine.evaluate(expression) }
            .getOrNull()
            ?.let { "= " + CalculatorEngine.format(it) }
            ?: ""
    } else {
        ""
    }

    // Верхняя (мелкая) строка дисплея: либо выражение после «=», либо предпросмотр.
    val smallText = if (evaluated) expression else preview
    // Крупная строка дисплея: результат после «=» или текущее выражение.
    val bigText = if (evaluated) result else expression.ifBlank { "0" }

    /** Полностью очищает состояние калькулятора. */
    fun clearAll() {
        expression = ""
        result = ""
        evaluated = false
    }

    /** Если было вычисление — сбрасывает состояние перед новым вводом. */
    fun startFreshIfNeeded() {
        if (evaluated) {
            expression = ""
            result = ""
            evaluated = false
        }
    }

    /** Добавляет цифру (или π) к текущему выражению. */
    fun appendDigit(text: String) {
        startFreshIfNeeded()
        expression += text
    }

    /**
     * Добавляет оператор с учётом контекста:
     * — если только что был результат, продолжает от него;
     * — заменяет предыдущий оператор, если нужно;
     * — позволяет начать выражение со знака «−».
     */
    fun appendOperator(operator: String) {
        if (evaluated) {
            // Продолжаем считать от полученного результата.
            expression = if (result.isNotEmpty() && result.first().isDigit()) result else ""
            result = ""
            evaluated = false
        }
        if (expression.isEmpty()) {
            if (operator == "−") expression = "−" // разрешаем начать с минуса
            return
        }
        when {
            expression.last() in "+−×÷^" -> expression = expression.dropLast(1) + operator
            expression.last() == '(' -> if (operator == "−") expression += operator
            else -> expression += operator
        }
    }

    /**
     * Добавляет десятичную точку с защитой:
     * — в пустом выражении даёт «0.»;
     * — не даёт поставить две точки в одно число;
     * — не ставит точку после π, «)», «%».
     */
    fun appendDecimal() {
        startFreshIfNeeded()
        when {
            expression.isEmpty() -> expression = "0."
            expression.last() in "+−×÷^(" -> expression += "0."
            expression.last().isDigit() -> {
                val currentNumber = expression.takeLastWhile { it.isDigit() || it == '.' }
                if (!currentNumber.contains('.')) expression += "."
            }
            // после π, ), % дробная часть не нужна
        }
    }

    /** Добавляет функцию/токен (например, «sin(», «√(») в выражение. */
    fun appendFunction(token: String) {
        startFreshIfNeeded()
        expression += token
    }

    /**
     * Добавляет скобку:
     * — «(» ставит всегда;
     * — «)» ставит только если есть незакрытая «(» и последний символ не оператор.
     */
    fun appendParenthesis(parenthesis: String) {
        if (parenthesis == "(") {
            startFreshIfNeeded()
            expression += "("
            return
        }
        val opened = expression.count { it == '(' } - expression.count { it == ')' }
        if (opened > 0 && expression.isNotEmpty() &&
            expression.last() !in "+−×÷^("
        ) {
            expression += ")"
        }
    }

    /** Возводит в квадрат: дописывает «^2» к последнему операнду. */
    fun appendSquare() {
        if (evaluated) {
            expression = if (result.firstOrNull()?.isDigit() == true) result else ""
            result = ""
            evaluated = false
        }
        if (expression.isNotEmpty() && expression.last() !in "+−×÷^(") {
            expression += "^2"
        }
    }

    /** Добавляет процент «%» к последнему операнду. */
    fun appendPercent() {
        startFreshIfNeeded()
        if (expression.isNotEmpty() && expression.last() !in "+−×÷^(") {
            expression += "%"
        }
    }

    /**
     * Удаление последнего символа:
     * — после «=» стирает результат (возвращает к выражению);
     * — иначе убирает один символ в конце выражения.
     */
    fun backspace() {
        if (evaluated) {
            evaluated = false
            result = ""
        } else {
            expression = expression.dropLast(1)
        }
    }

    /**
     * Вычисляет текущее выражение и показывает результат.
     * При ошибке очищает выражение и выводит сообщение об ошибке.
     */
    fun evaluate() {
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

    /** Маршрутизирует нажатие кнопки к соответствующему обработчику по её подписи. */
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
            label == "sin" || label == "cos" || label == "tan" -> appendFunction("$label(")
            label == "π" -> appendDigit("π")
            label in operators -> appendOperator(label)
            else -> appendDigit(label) // цифры
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Дисплей
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = smallText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = bigText,
                fontSize = bigFontSize(bigText),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(16.dp))

        // Клавиатура
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            buttonRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { label ->
                        CalcButton(
                            label = label,
                            modifier = Modifier.weight(if (label == "=") 2f else 1f),
                            onClick = { press(label) }
                        )
                    }
                }
            }
        }
    }
}

/** Подбирает размер шрифта для большого текста дисплея в зависимости от длины строки. */
private fun bigFontSize(text: String): TextUnit = when {
    text.length <= 9 -> 54.sp
    text.length <= 13 -> 44.sp
    text.length <= 17 -> 34.sp
    else -> 26.sp
}

/**
 * Одна кнопка калькулятора с цветовой схемой, зависящей от её типа
 * (цифра, оператор, «=», «C/⌫», функция).
 */
@Composable
private fun CalcButton(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isDigit = label.length == 1 && label[0].isDigit()

    val container: Color
    val content: Color
    val fontSize: TextUnit

    when {
        label == "=" -> {
            container = scheme.primary
            content = scheme.onPrimary
            fontSize = 28.sp
        }
        isDigit || label == "." -> {
            container = scheme.surfaceVariant
            content = scheme.onSurfaceVariant
            fontSize = 24.sp
        }
        label in operators -> {
            container = scheme.primaryContainer
            content = scheme.onPrimaryContainer
            fontSize = 26.sp
        }
        label == "C" || label == "⌫" -> {
            container = scheme.errorContainer
            content = scheme.onErrorContainer
            fontSize = 18.sp
        }
        else -> {
            container = scheme.secondaryContainer
            content = scheme.onSecondaryContainer
            fontSize = 16.sp
        }
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** Превью экрана калькулятора для Android Studio. */
@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun CalculatorPreview() {
    KalkuleitorTheme {
        CalculatorScreen()
    }
}