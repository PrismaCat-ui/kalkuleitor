package com.example.kalkuleitor

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** Исключение калькулятора — используется для сообщений об ошибках ввода/вычисления. */
class CalcException(message: String) : Exception(message)

object CalculatorEngine {

    /**
     * Вычисляет значение математического выражения.
     * Приводит символы «×», «÷», «−» к обычным «*», «/», «-» и передаёт строку парсеру.
     * Бросает [CalcException], если выражение некорректно или даёт NaN/бесконечность.
     */
    fun evaluate(expression: String): Double {
        if (expression.isBlank()) return 0.0
        val normalized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
        val value = Parser(normalized).parse()
        if (value.isNaN() || value.isInfinite()) throw CalcException("Ошибка")
        return value
    }

    /**
     * Красиво форматирует результат:
     * — убирает хвостовые нули и лишние дроби;
     * — для очень больших/маленьких чисел использует научную запись;
     * — возвращает «Ошибка» для NaN и бесконечностей.
     */
    fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Ошибка"
        if (value == 0.0) return "0"

        val abs = kotlin.math.abs(value)
        if (abs >= 1e12 || abs < 1e-9) {
            val text = String.format(Locale.US, "%.6e", value)
            val eIndex = text.indexOf('e')
            if (eIndex == -1) return text
            val mantissa = text.substring(0, eIndex).trimEnd('0').trimEnd('.')
            return mantissa + text.substring(eIndex)
        }

        return BigDecimal(value)
            .setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }
}


/**
 * Рекурсивный парсер выражений (метод рекурсивного спуска).
 * Поддерживает: +, -, *, /, ^, унарные +/-, проценты, скобки,
 * константу π, корень √, тригонометрию sin/cos/tan (в градусах), неявное умножение.
 */
private class Parser(private val input: String) {

    private var position = 0

    /** Точка входа: разбирает всё выражение и проверяет, что вход дочитан до конца. */
    fun parse(): Double {
        val value = expression()
        skipSpaces()
        if (position < input.length) throw error()
        return value
    }

    /** Разбирает сумму/разность: term (('+'|'-') term)*. */
    private fun expression(): Double {
        var value = term()
        while (true) {
            skipSpaces()
            when (peek()) {
                '+' -> { position++; value += term() }
                '-' -> { position++; value -= term() }
                else -> return value
            }
        }
    }

    /**
     * Разбирает произведение/деление (а также неявное умножение: 2π, 3(4+5), 2sin(30)).
     * Проверяет деление на ноль.
     */
    private fun term(): Double {
        var value = unary()
        while (true) {
            skipSpaces()
            val current = peek()
            value = when {
                current == '*' -> { position++; value * unary() }
                current == '/' -> {
                    position++
                    val divisor = unary()
                    if (divisor == 0.0) throw CalcException("Деление на 0")
                    value / divisor
                }
                // Неявное умножение: 2π, 3(4+5), 2sin(30)
                startsPrimary(current) -> value * unary()
                else -> return value
            }
        }
    }

    /** Разбирает унарные плюс/минус перед операндом (например, −5, +x). */
    private fun unary(): Double {
        skipSpaces()
        return when (peek()) {
            '-' -> { position++; -unary() }
            '+' -> { position++; unary() }
            else -> power()
        }
    }

    /** Разбирает возведение в степень (правoассоциативно: 2^3^2 = 2^(3^2)). */
    private fun power(): Double {
        val base = postfix()
        skipSpaces()
        if (peek() == '^') {
            position++
            return Math.pow(base, unary())
        }
        return base
    }

    /** Разбирает постфиксные проценты: число может иметь несколько «%» подряд. */
    private fun postfix(): Double {
        var value = primary()
        while (peek() == '%') {
            position++
            value /= 100.0
        }
        return value
    }

    /**
     * Разбирает «первичное» значение: число, скобочное выражение, π, √(...),
     * либо вызов функции (sin/cos/tan/sqrt).
     */
    private fun primary(): Double {
        skipSpaces()
        val current = peek()
        return when {
            current == '(' -> {
                position++
                val value = expression()
                skipSpaces()
                expect(')')
                value
            }
            current == 'π' -> { position++; Math.PI }
            current == '√' -> {
                position++
                skipSpaces()
                expect('(')
                val value = expression()
                skipSpaces()
                expect(')')
                Math.sqrt(value)
            }
            current.isDigit() || current == '.' -> number()
            current.isLetter() -> function()
            else -> throw error()
        }
    }

    /** Читает числовой литерал (целое/дробное, с экспонентой вида 1e10). */
    private fun number(): Double {
        val start = position
        while (position < input.length) {
            val current = input[position]
            if (current.isDigit() || current == '.') {
                position++
            } else if ((current == 'e' || current == 'E') &&
                position + 1 < input.length &&
                (input[position + 1].isDigit() || input[position + 1] == '+' || input[position + 1] == '-')
            ) {
                position += 2
            } else {
                break
            }
        }
        val text = input.substring(start, position)
        return text.toDoubleOrNull() ?: throw error()
    }

    /**
     * Разбирает вызов функции: имя, затем обязательные скобки с аргументом.
     * Тригонометрия считает в градусах. Неизвестное имя → ошибка.
     */
    private fun function(): Double {
        val start = position
        while (position < input.length && input[position].isLetter()) position++
        val name = input.substring(start, position).lowercase(Locale.US)

        skipSpaces()
        expect('(')
        val argument = expression()
        skipSpaces()
        expect(')')

        return when (name) {
            "sin" -> Math.sin(Math.toRadians(argument))
            "cos" -> Math.cos(Math.toRadians(argument))
            "tan" -> Math.tan(Math.toRadians(argument))
            "sqrt" -> Math.sqrt(argument)
            else -> throw CalcException("Ошибка")
        }
    }

    /** Проверяет и «съедает» ожидаемый символ; иначе — ошибка. */
    private fun expect(char: Char) {
        skipSpaces()
        if (peek() != char) throw error()
        position++
    }

    /** Возвращает текущий символ без сдвига; в конце строки — нулевой символ. */
    private fun peek(): Char = if (position < input.length) input[position] else '\u0000'

    /** Пропускает пробелы. */
    private fun skipSpaces() {
        while (position < input.length && input[position] == ' ') position++
    }

    /** Проверяет, может ли символ начинать первичное значение (для неявного умножения). */
    private fun startsPrimary(char: Char): Boolean =
        char == '(' || char == 'π' || char == '√' || char == '.' ||
                char.isDigit() || char.isLetter()

    /** Возвращает исключение-ошибку калькулятора. */
    private fun error() = CalcException("Ошибка")
}