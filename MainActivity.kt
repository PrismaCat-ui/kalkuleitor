package com.example.kalkuleitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.kalkuleitor.ui.theme.KalkuleitorTheme

/** Главная Activity: включает edge-to-edge и запускает Compose-контент приложения. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KalkuleitorTheme {
                KalkuleitorApp()
            }
        }
    }
}

/**
 * Корневой Composable приложения: оборачивает калькулятор в Scaffold,
 * чтобы учесть системные отступы (innerPadding).
 */
@Composable
fun KalkuleitorApp() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        CalculatorScreen(modifier = Modifier.padding(innerPadding))
    }
}

/** Превью всего приложения для Android Studio. */
@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun KalkuleitorAppPreview() {
    KalkuleitorTheme {
        KalkuleitorApp()
    }
}