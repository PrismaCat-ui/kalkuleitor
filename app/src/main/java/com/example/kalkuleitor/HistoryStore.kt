package com.example.kalkuleitor

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Одна запись в истории вычислений. */
data class HistoryEntry(
    val expression: String,
    val result: String,
    val timestamp: Long,
)

/**
 * Хранилище истории вычислений на SharedPreferences.
 * История сохраняется в JSON-строке и переживает перезапуск приложения.
 */
class HistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Загружает историю из хранилища. При повреждённых данных возвращает пустой список. */
    fun load(): List<HistoryEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val expression = item.optString("expression")
                val result = item.optString("result")
                if (expression.isBlank() || result.isBlank()) {
                    null
                } else {
                    HistoryEntry(expression, result, item.optLong("timestamp"))
                }
            }
        }.getOrDefault(emptyList())
    }

    /** Полностью перезаписывает историю. */
    fun save(entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("expression", entry.expression)
                    .put("result", entry.result)
                    .put("timestamp", entry.timestamp)
            )
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    companion object {
        /** Сколько последних вычислений храним. */
        const val MAX_ENTRIES = 100

        private const val PREFS_NAME = "calculator_history"
        private const val KEY_ENTRIES = "entries"
    }
}
