package com.example.kalkuleitor

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kalkuleitor.CalculatorState.Companion.operators
import com.example.kalkuleitor.ui.theme.CalcBackground
import com.example.kalkuleitor.ui.theme.CalcKeyDark
import com.example.kalkuleitor.ui.theme.CalcKeyLight
import com.example.kalkuleitor.ui.theme.CalcKeyOrange
import com.example.kalkuleitor.ui.theme.CalcTextMain
import com.example.kalkuleitor.ui.theme.CalcTextOnLight
import com.example.kalkuleitor.ui.theme.CalcTextSub
import com.example.kalkuleitor.ui.theme.KalkuleitorTheme

// Раскладка кнопок — это визуальное решение, поэтому остаётся в UI-файле.
private val buttonRows = listOf(
    listOf("sin", "cos", "tan", "C"),
    listOf("√", "x²", "π", "⌫"),
    listOf("7", "8", "9", "÷"),
    listOf("4", "5", "6", "×"),
    listOf("1", "2", "3", "−"),
    listOf("(", ")", ".", "+"),
    listOf("%", "0", "="),
)

/** Зазор между кнопками. */
private val Gap = 6.dp

/** Высота прямоугольной кнопки — ширина задаётся весом внутри Row. */
private val ButtonHeight = 62.dp

/**
 * Визуальная часть калькулятора.
 * Ничего не считает сам — только рисует дисплей и кнопки,
 * а нажатия отдаёт в [CalculatorState.press].
 */
@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    val state = rememberSaveable(saver = CalculatorState.Saver) { CalculatorState() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CalcBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Дисплей
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = state.smallText,
                fontSize = 18.sp,
                color = CalcTextSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.bigText,
                fontSize = bigFontSize(state.bigText),
                fontWeight = FontWeight.Light,
                color = CalcTextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(10.dp))

        // Клавиатура: прямоугольные кнопки — фиксированная высота, ширина через weight.
        Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
            buttonRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Gap)
                ) {
                    row.forEach { label ->
                        CalcButton(
                            label = label,
                            modifier = Modifier
                                .weight(if (label == "=") 2f else 1f)
                                .height(ButtonHeight),
                            onClick = { state.press(label) }
                        )
                    }
                }
            }
        }
    }
}

/** Размер шрифта крупного дисплея подбирается под длину текста. */
private fun bigFontSize(text: String): TextUnit = when {
    text.length <= 9 -> 54.sp
    text.length <= 13 -> 44.sp
    text.length <= 17 -> 34.sp
    else -> 26.sp
}

/** Прямоугольная кнопка калькулятора в стиле iPhone. */
@Composable
private fun CalcButton(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val isDigit = label.length == 1 && label[0].isDigit()

    val container: Color
    val content: Color
    val fontSize: TextUnit

    when {
        // Оранжевые: операторы и «=» — как правая колонка в iPhone.
        label == "=" || label in operators -> {
            container = CalcKeyOrange
            content = CalcTextMain
            fontSize = if (label == "=") 30.sp else 28.sp
        }
        // Светло-серые: сброс и удаление — как AC в iPhone.
        label == "C" || label == "⌫" -> {
            container = CalcKeyLight
            content = CalcTextOnLight
            fontSize = 18.sp
        }
        // Тёмно-серые: цифры, точка, функции, скобки, % — как цифровая клавиатура.
        else -> {
            container = CalcKeyDark
            content = CalcTextMain
            fontSize = if (isDigit || label == ".") 26.sp else 16.sp
        }
    }

    Button(
        onClick = onClick,
        modifier = modifier,
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