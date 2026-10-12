package com.example.tanglishpro

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.tanglishpro.core.LocalStore

class MainActivity : Activity() {
    private lateinit var shortcutList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.enableKeyboardButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        findViewById<Button>(R.id.chooseKeyboardButton).setOnClickListener {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }

        listOf(
            R.id.themeLight to "light",
            R.id.themeOcean to "ocean",
            R.id.themeRose to "rose",
            R.id.themeDark to "dark"
        ).forEach { (viewId, theme) ->
            findViewById<Button>(viewId).setOnClickListener {
                LocalStore.setTheme(this, theme)
                Toast.makeText(this, "Theme saved. Reopen the keyboard to see it.", Toast.LENGTH_SHORT).show()
            }
        }

        shortcutList = findViewById(R.id.shortcutsList)
        updateShortcutList()
        findViewById<Button>(R.id.addShortcutButton).setOnClickListener {
            val key = findViewById<EditText>(R.id.shortcutInput).text.toString().trim()
            val expansion = findViewById<EditText>(R.id.expansionInput).text.toString().trim()
            if (key.isEmpty() || expansion.isEmpty() || key.contains(' ')) {
                Toast.makeText(this, "Enter a one-word shortcut and its expansion.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            LocalStore.saveShortcut(this, key, expansion)
            findViewById<EditText>(R.id.shortcutInput).text.clear()
            findViewById<EditText>(R.id.expansionInput).text.clear()
            updateShortcutList()
            Toast.makeText(this, "Shortcut saved on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateShortcutList() {
        shortcutList.text = LocalStore.shortcuts(this).toSortedMap().entries.joinToString("\n") { "${it.key}  →  ${it.value}" }
    }
}
