package com.example.kalkuleitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.kalkuleitor.ui.theme.KalkuleitorTheme
import kotlinx.coroutines.launch

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
 * Корневой Composable приложения.
 *
 * Содержит выдвижное меню (три полоски в левом верхнем углу), из которого можно
 * переключаться между обычным и инженерным калькулятором, а также открывать историю.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalkuleitorApp() {
    val context = LocalContext.current
    val store = remember(context) { HistoryStore(context) }

    // История вычислений: загружается один раз и сохраняется при каждом изменении.
    var history by remember { mutableStateOf(store.load()) }
    // Текущий режим калькулятора (хранится как имя enum — так переживает поворот экрана).
    var modeName by rememberSaveable { mutableStateOf(CalculatorMode.NORMAL.name) }
    // Открыт ли экран истории вместо калькулятора.
    var showHistory by rememberSaveable { mutableStateOf(false) }

    val mode = CalculatorMode.valueOf(modeName)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Сохраняет состояние калькулятора при переключении между экранами меню.
    val stateHolder = rememberSaveableStateHolder()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Калькулятор",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                )
                HorizontalDivider()

                CalculatorMode.entries.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.title) },
                        selected = !showHistory && item == mode,
                        onClick = {
                            modeName = item.name
                            showHistory = false
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )

                NavigationDrawerItem(
                    label = { Text("История") },
                    selected = showHistory,
                    onClick = {
                        showHistory = true
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(if (showHistory) "История" else mode.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Меню")
                        }
                    }
                )
            }
        ) { innerPadding ->
            val screenKey = if (showHistory) "history" else "calculator-${mode.name}"
            stateHolder.SaveableStateProvider(screenKey) {
                if (showHistory) {
                    HistoryScreen(
                        entries = history,
                        onClear = {
                            history = emptyList()
                            store.save(emptyList())
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    CalculatorScreen(
                        mode = mode,
                        onEvaluated = { expression, result ->
                            val updated = (
                                listOf(HistoryEntry(expression, result, System.currentTimeMillis())) +
                                    history
                                ).take(HistoryStore.MAX_ENTRIES)
                            history = updated
                            store.save(updated)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
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