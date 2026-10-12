package com.example.tanglishpro.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.KeyEvent
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.tanglishpro.MainActivity
import com.example.tanglishpro.core.LocalStore
import com.example.tanglishpro.core.OfflineTranslator

class TanglishInputMethod : InputMethodService() {
    private enum class Mode { ENGLISH, TAMIL, TANGLISH }
    private enum class Panel { KEYS, EMOJI, CLIPBOARD }

    private var mode = Mode.TANGLISH
    private var panel = Panel.KEYS
    private var symbols = false
    private var shifted = false
    private var composition = StringBuilder()
    private lateinit var root: LinearLayout
    private lateinit var keysArea: LinearLayout
    private lateinit var theme: Palette

    private data class Palette(val background: Int, val key: Int, val ink: Int, val accent: Int)

    override fun onCreateInputView(): View {
        theme = palette(LocalStore.theme(this))
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(6))
            setBackgroundColor(theme.background)
        }
        root.addView(makeToolbar())
        keysArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(keysArea, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        showKeys()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        theme = palette(LocalStore.theme(this))
        if (::root.isInitialized) {
            root.setBackgroundColor(theme.background)
            refreshButtons(root)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        commitComposition()
        super.onFinishInputView(finishingInput)
    }

    private fun refreshButtons(view: View) {
        if (view is Button) {
            val translate = view.text.toString() == "Translate"
            view.setTextColor(if (translate) Color.WHITE else theme.ink)
            view.background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(if (translate) theme.accent else theme.key)
            }
        } else if (view is ViewGroup) {
            for (index in 0 until view.childCount) refreshButtons(view.getChildAt(index))
        }
    }

    private fun makeToolbar(): View {
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        scroll.addView(row)
        addToolbarButton(row, "EN", "English") { mode = Mode.ENGLISH; panel = Panel.KEYS; showKeys() }
        addToolbarButton(row, "அ", "Tamil") { mode = Mode.TAMIL; panel = Panel.KEYS; symbols = false; showKeys() }
        addToolbarButton(row, "Ta", "Tanglish") { mode = Mode.TANGLISH; panel = Panel.KEYS; symbols = false; showKeys() }
        addToolbarButton(row, "Translate", "Translate current Tanglish text") { translateCurrent() }
        addToolbarButton(row, "🙂", "Emoji") { panel = Panel.EMOJI; showPanel() }
        addToolbarButton(row, "▤", "Clipboard history") { openClipboardPanel() }
        addToolbarButton(row, "⚙", "Settings") {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        return scroll
    }

    private fun addToolbarButton(row: LinearLayout, label: String, description: String, action: () -> Unit) {
        val b = button(label, compact = true)
        b.contentDescription = description
        row.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)).apply {
            setMargins(dp(2), 0, dp(2), dp(4))
        })
        b.setOnClickListener { action() }
    }

    private fun showKeys() {
        if (!::keysArea.isInitialized) return
        keysArea.removeAllViews()
        if (mode == Mode.TAMIL) {
            listOf(
                listOf("அ", "ஆ", "இ", "ஈ", "உ", "ஊ", "எ", "ஏ", "ஐ", "ஒ", "ஓ", "ஔ"),
                listOf("க", "ச", "ட", "த", "ப", "ற", "ய", "ர", "ல", "வ"),
                listOf("ங", "ஞ", "ண", "ந", "ம", "ன", "ழ", "ள", "ஜ", "ஷ", "ஸ", "ஹ"),
                listOf("ா", "ி", "ீ", "ு", "ூ", "ெ", "ே", "ை", "ொ", "ோ", "ௌ", "்"),
                listOf("ஃ", "க்ஷ", "ஶ", "ஶ்ரீ", "ஃப", "ழ்", "ள்", "ண்", "ன்", "ம்")
            ).forEach { addKeyRow(it, tamil = true) }
        } else if (symbols) {
            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/"),
                listOf("=", "*", "\"", "'", ":", ";", "!", "?", "⌫")
            ).forEach { addKeyRow(it) }
        } else {
            listOf("qwertyuiop".map { it.toString() }, "asdfghjkl".map { it.toString() }).forEach { addKeyRow(it) }
            val third = listOf("⇧") + "zxcvbnm".map { it.toString() } + listOf("⌫")
            addKeyRow(third)
        }
        addBottomRow()
    }

    private fun addKeyRow(labels: List<String>, tamil: Boolean = false) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        labels.forEach { label ->
            val key = button(if (!tamil && shifted && label.length == 1 && label[0].isLetter()) label.uppercase() else label)
            key.textSize = if (tamil && label.length > 1) 16f else 20f
            row.addView(key, LinearLayout.LayoutParams(0, dp(if (tamil) 42 else 48), 1f).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
            key.setOnClickListener { onKey(label, tamil) }
        }
        keysArea.addView(row)
    }

    private fun addBottomRow() {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun add(label: String, weight: Float, action: () -> Unit) {
            val b = button(label)
            b.textSize = if (label == "space") 14f else 17f
            row.addView(b, LinearLayout.LayoutParams(0, dp(48), weight).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
            b.setOnClickListener { action() }
        }
        add("?123", 1.2f) { symbols = !symbols; showKeys() }
        add(",", 0.8f) { insertPunctuation(",") }
        add("space", 4.5f) { insertSpace() }
        add(".", 0.8f) { insertPunctuation(".") }
        add("↵", 1.2f) { enter() }
        keysArea.addView(row)
    }

    private fun onKey(key: String, tamil: Boolean) {
        if (key == "⇧") { shifted = !shifted; showKeys(); return }
        if (key == "⌫") { backspace(); return }
        if (tamil) {
            commitComposition()
            currentInputConnection?.commitText(key, 1)
            return
        }
        val text = if (shifted && key.length == 1 && key[0].isLetter()) key.uppercase() else key
        composition.append(text)
        currentInputConnection?.setComposingText(composition.toString(), 1)
        if (shifted) { shifted = false; showKeys() }
    }

    private fun insertSpace() {
        val ic = currentInputConnection ?: return
        if (composition.isNotEmpty()) {
            val raw = composition.toString()
            val lastWord = raw.trimEnd().substringAfterLast(' ')
            val expanded = if (lastWord.isNotEmpty()) LocalStore.shortcuts(this)[lastWord.lowercase()] else null
            val prefix = if (expanded != null) raw.dropLast(lastWord.length) else raw
            composition.clear()
            composition.append(prefix).append(expanded ?: "").append(' ')
            ic.setComposingText(composition.toString(), 1)
        } else ic.commitText(" ", 1)
    }

    private fun insertPunctuation(punctuation: String) {
        commitComposition()
        currentInputConnection?.commitText(punctuation, 1)
    }

    private fun backspace() {
        val ic = currentInputConnection ?: return
        if (composition.isNotEmpty()) {
            composition.deleteCharAt(composition.length - 1)
            if (composition.isEmpty()) ic.finishComposingText() else ic.setComposingText(composition.toString(), 1)
        } else {
            if (android.os.Build.VERSION.SDK_INT >= 24) ic.deleteSurroundingTextInCodePoints(1, 0)
            else ic.deleteSurroundingText(1, 0)
        }
    }

    private fun commitComposition() {
        if (composition.isNotEmpty()) {
            currentInputConnection?.commitText(composition.toString(), 1)
            composition.clear()
        }
    }

    private fun translateCurrent() {
        if (composition.isBlank()) {
            Toast.makeText(this, "Type a Tanglish phrase first.", Toast.LENGTH_SHORT).show()
            return
        }
        val translated = OfflineTranslator.translate(composition.toString())
        currentInputConnection?.commitText(translated, 1)
        composition.clear()
        panel = Panel.KEYS
    }

    private fun enter() {
        val ic = currentInputConnection ?: return
        commitComposition()
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun showPanel() {
        keysArea.removeAllViews()
        val values = if (panel == Panel.EMOJI) {
            listOf("😀", "😂", "🥰", "😍", "😊", "🙏", "👍", "❤️", "🎉", "😢", "🤔", "😎", "🔥", "✨", "💯", "🙌", "😅", "😭", "🙂", "😉", "🌸", "💐", "☀️", "🌈")
        } else LocalStore.clipboardHistory(this)
        val rows = values.chunked(6)
        rows.forEach { items ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            items.forEach { value ->
                val b = button(value)
                b.textSize = if (panel == Panel.EMOJI) 24f else 13f
                row.addView(b, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
                b.setOnClickListener {
                    commitComposition()
                    currentInputConnection?.commitText(value, 1)
                    panel = Panel.KEYS
                    showKeys()
                }
            }
            keysArea.addView(row)
        }
        if (values.isEmpty()) {
            val empty = TextView(this).apply {
                text = if (panel == Panel.CLIPBOARD) "No clipboard text yet. Copy text, then tap ▤ again." else "No items"
                setTextColor(theme.ink)
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
            keysArea.addView(empty)
        }
        val back = button("ABC  •  Back to keyboard")
        keysArea.addView(back, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        back.setOnClickListener { panel = Panel.KEYS; showKeys() }
    }

    private fun openClipboardPanel() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val clip: ClipData? = clipboard.primaryClip
            val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
            if (!text.isNullOrBlank()) LocalStore.addClipboardEntry(this, text)
        }
        panel = Panel.CLIPBOARD
        showPanel()
    }

    private fun button(label: String, compact: Boolean = false): Button = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(if (label == "Translate") Color.WHITE else theme.ink)
        textSize = if (compact) 13f else 19f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        minHeight = 0
        minimumHeight = 0
        minWidth = 0
        minimumWidth = 0
        setPadding(dp(if (compact) 10 else 2), 0, dp(if (compact) 10 else 2), 0)
        background = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(if (label == "Translate") theme.accent else theme.key)
        }
    }

    private fun palette(name: String): Palette = when (name) {
        "ocean" -> Palette(Color.rgb(218, 238, 248), Color.WHITE, Color.rgb(24, 54, 72), Color.rgb(22, 120, 158))
        "rose" -> Palette(Color.rgb(250, 229, 236), Color.WHITE, Color.rgb(77, 36, 53), Color.rgb(186, 67, 117))
        "dark" -> Palette(Color.rgb(34, 35, 43), Color.rgb(59, 60, 71), Color.WHITE, Color.rgb(132, 116, 236))
        else -> Palette(Color.rgb(244, 243, 249), Color.WHITE, Color.rgb(41, 38, 56), Color.rgb(104, 86, 201))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
