package com.oliveira.joguinhosdamamae

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class WordSearchGame(
    c: Context,
    private val onBack: () -> Unit
) : LinearLayout(c) {

    private val bank = listOf(
        "GATO", "FLOR", "CASA", "PRAIA", "SOL", "LUA", "AMOR", "BOLO", "FESTA", "PAZ",
        "CHUVA", "VENTO", "MESA", "LIVRO", "FAMILIA", "NETO", "AVO", "BANANA", "UVA", "PERA",
        "LIMAO", "CAFE", "PAO", "QUEIJO", "HORTA", "JARDIM", "ESTRELA", "NUVEM", "MAR", "RIO",
        "PEIXE", "PASSARO", "ABELHA", "BORBOLETA", "TEMPO", "RISADA", "ABRACO", "SAUDADE",
        "CARINHO", "BENCAO", "ORACAO", "COZINHA", "TRICO", "CROCHE", "NOVELA", "BINGO"
    )

    private var level = 1
    private var size = 8
    private var grid = Array(0) { CharArray(0) }
    private var placed = listOf<String>()
    private val found = mutableSetOf<String>()
    private val foundCells = mutableSetOf<Int>()
    private var selR = -1
    private var selC = -1

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 14), Ui.dp(c, 28), Ui.dp(c, 14), Ui.dp(c, 24))
        setBackgroundColor(Ui.BG)
        start()
    }

    private fun start() {
        size = minOf(7 + level, 12)
        grid = Array(size) { CharArray(size) { '\u0000' } }
        val dirs = listOf(
            0 to 1, 1 to 0, 1 to 1, 0 to -1, -1 to 0, -1 to -1, 1 to -1, -1 to 1
        ).take(if (level < 3) 3 else if (level < 5) 5 else 8)
        val chosen = bank.filter { it.length <= size }.shuffled().take(minOf(4 + level, 10))
        val ok = mutableListOf<String>()

        for (w in chosen) {
            var done = false
            var tries = 0
            while (!done && tries < 300) {
                tries++
                val d = dirs.random()
                val dr = d.first
                val dc = d.second
                val r = (0 until size).random()
                val col = (0 until size).random()
                val er = r + dr * (w.length - 1)
                val ec = col + dc * (w.length - 1)
                if (er !in 0 until size || ec !in 0 until size) continue
                var fits = true
                for (i in w.indices) {
                    val ch = grid[r + dr * i][col + dc * i]
                    if (ch != '\u0000' && ch != w[i]) {
                        fits = false
                        break
                    }
                }
                if (!fits) continue
                for (i in w.indices) grid[r + dr * i][col + dc * i] = w[i]
                ok.add(w)
                done = true
            }
        }
        placed = ok

        for (r in 0 until size) for (col in 0 until size) {
            if (grid[r][col] == '\u0000') grid[r][col] = ('A'..'Z').random()
        }
        found.clear()
        foundCells.clear()
        selR = -1
        selC = -1
        draw()
    }

    private fun won() = placed.isNotEmpty() && found.size == placed.size

    private fun tap(r: Int, col: Int) {
        if (won()) return
        if (selR < 0) {
            selR = r
            selC = col
            draw()
            return
        }
        if (selR == r && selC == col) {
            selR = -1
            selC = -1
            draw()
            return
        }
        val dr = r - selR
        val dc = col - selC
        if (dr == 0 || dc == 0 || abs(dr) == abs(dc)) {
            val len = maxOf(abs(dr), abs(dc))
            val sr = Integer.signum(dr)
            val sc = Integer.signum(dc)
            val sb = StringBuilder()
            val cells = mutableListOf<Int>()
            for (k in 0..len) {
                val rr = selR + sr * k
                val cc = selC + sc * k
                sb.append(grid[rr][cc])
                cells.add(rr * size + cc)
            }
            val s = sb.toString()
            val w = placed.firstOrNull { it == s || it == s.reversed() }
            if (w != null && w !in found) {
                found.add(w)
                foundCells.addAll(cells)
            }
        }
        selR = -1
        selC = -1
        draw()
    }

    private fun draw() {
        val c = context
        removeAllViews()
        val msg = Ui.header(c, this, "Caça-Palavras · Nível $level") { onBack() }

        val screenW = c.resources.displayMetrics.widthPixels
        val cell = minOf((screenW - Ui.dp(c, 28)) / size - Ui.dp(c, 2), Ui.dp(c, 42))

        for (r in 0 until size) {
            val row = Ui.row(c)
            for (col in 0 until size) {
                val isFound = foundCells.contains(r * size + col)
                val isSel = r == selR && col == selC
                row.addView(TextView(c).apply {
                    text = grid[r][col].toString()
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(if (isFound) Color.WHITE else Ui.TEXT)
                    background = Ui.shape(
                        when {
                            isFound -> Ui.GREEN
                            isSel -> 0xFFFFE27A.toInt()
                            else -> Ui.CARD
                        },
                        Ui.dp(c, 6).toFloat()
                    )
                    layoutParams = LayoutParams(cell, cell).apply {
                        setMargins(Ui.dp(c, 1), Ui.dp(c, 1), Ui.dp(c, 1), Ui.dp(c, 1))
                    }
                    setOnClickListener { tap(r, col) }
                })
            }
            addView(row)
        }

        addView(Ui.text(c, placed.joinToString("    ") { if (it in found) "✔$it" else it }, 16f, true).apply {
            setPadding(0, Ui.dp(c, 14), 0, Ui.dp(c, 10))
        })

        val bar = Ui.row(c)
        bar.addView(Ui.smallBtn(c, "Novo jogo") { start() })
        addView(bar)

        if (won()) {
            msg.text = "🎉 Achou todas!"
            addView(Ui.btn(c, "Próximo nível →") { level++; start() })
        }
    }
}
