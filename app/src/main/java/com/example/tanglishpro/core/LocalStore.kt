package com.example.tanglishpro.core

import android.content.Context
import org.json.JSONArray

object LocalStore {
    private const val PREFS = "tanglish_pro_local"
    private const val KEY_THEME = "theme"
    private const val KEY_SHORTCUTS = "shortcuts"
    private const val KEY_CLIPBOARD = "clipboard_history"

    fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun theme(context: Context): String = prefs(context).getString(KEY_THEME, "light") ?: "light"

    fun setTheme(context: Context, theme: String) {
        prefs(context).edit().putString(KEY_THEME, theme).apply()
    }

    fun shortcuts(context: Context): Map<String, String> {
        val encoded = prefs(context).getStringSet(KEY_SHORTCUTS, null)
        if (encoded == null) {
            return mapOf("brb" to "be right back", "omw" to "on my way", "ty" to "thank you")
        }
        return encoded.mapNotNull { entry ->
            val split = entry.indexOf("::")
            if (split <= 0) null else entry.substring(0, split) to entry.substring(split + 2)
        }.toMap()
    }

    fun saveShortcut(context: Context, shortcut: String, expansion: String) {
        val current = shortcuts(context).toMutableMap()
        current[shortcut.lowercase()] = expansion
        val encoded = current.map { (key, value) -> "$key::$value" }.toMutableSet()
        prefs(context).edit().putStringSet(KEY_SHORTCUTS, encoded).apply()
    }

    fun clipboardHistory(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_CLIPBOARD, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addClipboardEntry(context: Context, text: String) {
        val value = text.trim()
        if (value.isEmpty()) return
        val history = (listOf(value) + clipboardHistory(context).filterNot { it == value }).take(30)
        val array = JSONArray()
        history.forEach(array::put)
        prefs(context).edit().putString(KEY_CLIPBOARD, array.toString()).apply()
    }
}
