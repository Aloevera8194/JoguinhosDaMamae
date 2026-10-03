package com.oliveira.joguinhosdamamae

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class MahjongGame(
    c: Context,
    private val onBack: () -> Unit
) : LinearLayout(c) {

    private class T(val l: Int, val r: Int, val c: Int, var s: String, var gone: Boolean = false)

    private val symbols = listOf("⚓", "🚢", "🗼", "🧭", "🗺️", "⛵", "🐚", "🦀", "🐙", "🌊", "🏝️")
    private val layerColors = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFF3D6.toInt(), 0xFFFFE08A.toInt())

    private var tiles = listOf<T>()
    private var sel: T? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 8), Ui.dp(c, 28), Ui.dp(c, 8), Ui.dp(c, 24))
        setBackgroundColor(Ui.BG)
        newGame()
    }

    private fun newGame() {
        val pos = mutableListOf<Triple<Int, Int, Int>>()
        for (r in 0..4) for (c in 0..5) pos.add(Triple(0, r, c))
        for (r in 1..3) for (c in 1..4) pos.add(Triple(1, r, c))
        for (c in 2..3) pos.add(Triple(2, 2, c))
        val syms = symbols.flatMap { listOf(it, it, it, it) }.shuffled()
        tiles = pos.mapIndexed { i, p -> T(p.first, p.second, p.third, syms[i]) }
        sel = null
        draw()
    }

    private fun shuffleRemaining() {
        val rem = tiles.filter { !it.gone }
        val s = rem.map { it.s }.shuffled()
        rem.forEachIndexed { i, t -> t.s = s[i] }
        sel = null
        draw()
    }

    private fun topAt(r: Int, c: Int): T? =
        tiles.filter { !it.gone && it.r == r && it.c == c }.maxByOrNull { it.l }

    private fun has(l: Int, r: Int, c: Int) =
        tiles.any { !it.gone && it.l == l && it.r == r && it.c == c }

    private fun free(t: T) =
        topAt(t.r, t.c) === t && (!has(t.l, t.r, t.c - 1) || !has(t.l, t.r, t.c + 1))

    private fun hasMoves(): Boolean =
        tiles.filter { !it.gone && free(it) }.groupBy { it.s }.any { it.value.size >= 2 }

    private fun tap(t: T) {
        if (!free(t)) return
        val cur = sel
        if (cur == null) {
            sel = t
        } else if (cur === t) {
            sel = null
        } else if (cur.s == t.s) {
            cur.gone = true
            t.gone = true
            sel = null
        } else {
            sel = t
        }
        draw()
    }

    private fun draw() {
        val c = context
        removeAllViews()
        val msg = Ui.header(c, this, "Mahjong") { onBack() }

        for (r in 0..4) {
            val row = Ui.row(c)
            for (col in 0..5) {
                val t = topAt(r, col)
                val w = Ui.dp(c, 54)
                val h = Ui.dp(c, 68)
                if (t == null) {
                    row.addView(View(c).apply {
                        layoutParams = LayoutParams(w, h).apply { setMargins(Ui.dp(c, 2), Ui.dp(c, 2), Ui.dp(c, 2), Ui.dp(c, 2)) }
                    })
                } else {
                    val isFree = free(t)
                    val stroke = when {
                        sel === t -> Color.parseColor("#E65100")
                        isFree -> Ui.GREEN
                        else -> 0xFFBDB6A5.toInt()
                    }
                    val bg = if (sel === t) 0xFFFFE27A.toInt() else layerColors[t.l]
                    row.addView(TextView(c).apply {
                        text = t.s
                        textSize = 26f
                        gravity = Gravity.CENTER
                        alpha = if (isFree) 1f else 0.6f
                        background = Ui.shape(bg, Ui.dp(c, 8).toFloat(), Ui.dp(c, 3), stroke)
                        layoutParams = LayoutParams(w, h).apply { setMargins(Ui.dp(c, 2), Ui.dp(c, 2), Ui.dp(c, 2), Ui.dp(c, 2)) }
                        setOnClickListener { tap(t) }
                    })
                }
            }
            addView(row)
        }

        val bar = Ui.row(c)
        bar.addView(Ui.smallBtn(c, "🔀 Embaralhar") { shuffleRemaining() })
        bar.addView(Ui.smallBtn(c, "Novo jogo") { newGame() })
        addView(bar)

        when {
            tiles.all { it.gone } -> {
                msg.text = "🎉 Parabéns, limpou tudo!"
            }
            !hasMoves() -> msg.text = "Sem jogadas. Toque em Embaralhar"
        }
    }
}
