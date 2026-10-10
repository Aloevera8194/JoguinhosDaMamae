package com.oliveira.joguinhosdamamae

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private var inMenu = true

    private class Item(val icon: String, val name: String, val c1: Int, val c2: Int, val make: () -> View)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        showMenu()
    }

    fun showMenu() {
        inMenu = true
        val items = listOf(
            Item("🔩", "Ordenar Porcas", 0xFF3949AB.toInt(), 0xFF7986CB.toInt()) { SortGame(this, true, ::showMenu) },
            Item("🐦", "Ordenar Pássaros", 0xFF00ACC1.toInt(), 0xFF4DD0E1.toInt()) { SortGame(this, false, ::showMenu) },
            Item("🎨", "Areia Colorida", 0xFF8E24AA.toInt(), 0xFFBA68C8.toInt()) { SandGame(this, ::showMenu) },
            Item("🀄", "Mahjong", 0xFFD32F2F.toInt(), 0xFFEF5350.toInt()) { MahjongGame(this, ::showMenu) },
            Item("🔎", "Caça-Palavras", 0xFF43A047.toInt(), 0xFF81C784.toInt()) { WordSearchGame(this, ::showMenu) },
            Item("✏️", "Palavras Cruzadas", 0xFFF57C00.toInt(), 0xFFFFB74D.toInt()) { CrosswordGame(this, ::showMenu) },
            Item("🔢", "Sudoku", 0xFF1E88E5.toInt(), 0xFF64B5F6.toInt()) { SudokuGame(this, ::showMenu) },
            Item("🧠", "Memória", 0xFFD81B60.toInt(), 0xFFF06292.toInt()) { MemoryGame(this, ::showMenu) },
            Item("🍬", "Combinar 3", 0xFF00695C.toInt(), 0xFF26A69A.toInt()) { MatchGame(this, ::showMenu) }
        )

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.clipChildren = false
        root.clipToPadding = false
        root.setPadding(Ui.dp(this, 12), Ui.dp(this, 36), Ui.dp(this, 12), Ui.dp(this, 24))
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFFFFF8E1.toInt(), 0xFFEDE7F6.toInt())
        )
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val title = TextView(this)
        title.text = "🧩 Joguinhos da Mamãe"
        title.textSize = 28f
        title.gravity = Gravity.CENTER
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(0xFF3E2723.toInt())
        root.addView(title)

        val sub = TextView(this)
        sub.text = "Escolha um jogo"
        sub.textSize = 16f
        sub.gravity = Gravity.CENTER
        sub.setTextColor(0xFF8D6E63.toInt())
        sub.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 16))
        root.addView(sub)

        var index = 0
        for (pair in items.chunked(2)) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.clipChildren = false
            for (item in pair) {
                row.addView(tile(item, index))
                index++
            }
            if (pair.size == 1) {
                val spacer = View(this)
                spacer.layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                row.addView(spacer)
            }
            root.addView(row)
        }
        setContentView(Ui.scroll(this, root))
    }

    private fun tile(item: Item, index: Int): View {
        val c = this
        val box = LinearLayout(c)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        val bg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(item.c1, item.c2))
        bg.cornerRadius = Ui.dp(c, 24).toFloat()
        box.background = bg
        box.elevation = Ui.dp(c, 6).toFloat()

        val icon = TextView(c)
        icon.text = item.icon
        icon.textSize = 40f
        icon.gravity = Gravity.CENTER
        box.addView(icon)

        val label = TextView(c)
        label.text = item.name
        label.textSize = 16f
        label.gravity = Gravity.CENTER
        label.setTypeface(null, Typeface.BOLD)
        label.setTextColor(Color.WHITE)
        label.setPadding(Ui.dp(c, 8), Ui.dp(c, 6), Ui.dp(c, 8), 0)
        box.addView(label)

        box.setOnClickListener { open(item.make()) }
        box.layoutParams = LinearLayout.LayoutParams(0, Ui.dp(c, 132), 1f).apply {
            setMargins(Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 8))
        }
        box.alpha = 0f
        box.translationY = Ui.dp(c, 30).toFloat()
        box.animate().alpha(1f).translationY(0f).setStartDelay(index * 70L).setDuration(350).start()
        return box
    }

    private fun open(v: View) {
        inMenu = false
        setContentView(Ui.scroll(this, v))
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (inMenu) super.onBackPressed() else showMenu()
    }
}
