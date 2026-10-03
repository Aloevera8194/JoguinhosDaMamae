package com.oliveira.joguinhosdamamae

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class SortGame(
    c: Context,
    private val nuts: Boolean,
    private val onBack: () -> Unit
) : LinearLayout(c) {

    private val nutColors = intArrayOf(
        0xFFE53935.toInt(), 0xFF9E9E9E.toInt(), 0xFFFFAB91.toInt(),
        0xFF26C6DA.toInt(), 0xFFFBC02D.toInt(), 0xFF8E24AA.toInt(),
        0xFFEC407A.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt()
    )
    private val birds = listOf("🦜", "🦉", "🐤", "🐦", "🦆", "🦅", "🐧", "🦩", "🐓")

    private var level = 1
    private var tubes = mutableListOf<MutableList<Int>>()
    private val history = mutableListOf<List<List<Int>>>()
    private var sel = -1

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 14), Ui.dp(c, 28), Ui.dp(c, 14), Ui.dp(c, 24))
        setBackgroundColor(Ui.BG)
        start()
    }

    private fun solved() = tubes.all { t -> t.isEmpty() || (t.size == 4 && t.all { it == t[0] }) }

    private fun start() {
        val n = minOf(3 + level, 9)
        do {
            val all = (0 until n).flatMap { listOf(it, it, it, it) }.shuffled()
            tubes = MutableList(n) { i -> all.subList(i * 4, i * 4 + 4).toMutableList() }
            tubes.add(mutableListOf())
            tubes.add(mutableListOf())
        } while (solved())
        history.clear()
        sel = -1
        draw()
    }

    private fun undo() {
        if (history.isNotEmpty()) {
            tubes = history.removeAt(history.lastIndex).map { it.toMutableList() }.toMutableList()
            sel = -1
            draw()
        }
    }

    private fun tap(i: Int) {
        if (solved()) return
        if (sel < 0) {
            if (tubes[i].isNotEmpty()) sel = i
        } else {
            val a = tubes[sel]
            val b = tubes[i]
            if (sel != i && a.isNotEmpty()) {
                val col = a.last()
                if (b.isEmpty() || (b.last() == col && b.size < 4)) {
                    history.add(tubes.map { it.toList() })
                    while (a.isNotEmpty() && a.last() == col && b.size < 4) {
                        b.add(a.removeAt(a.lastIndex))
                    }
                }
            }
            sel = -1
        }
        draw()
    }

    private fun tubeView(i: Int): LinearLayout {
        val c = context
        val t = tubes[i]
        val selected = sel == i
        val v = LinearLayout(c)
        v.background = Ui.shape(
            Ui.MUTED, Ui.dp(c, 16).toFloat(),
            Ui.dp(c, 3), if (selected) Ui.GREEN else Ui.MUTED
        )
        v.setPadding(Ui.dp(c, 4), Ui.dp(c, 6), Ui.dp(c, 4), Ui.dp(c, 6))
        v.setOnClickListener { tap(i) }
        if (nuts) {
            v.orientation = VERTICAL
            v.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            v.layoutParams = LayoutParams(Ui.dp(c, 58), Ui.dp(c, 170)).apply {
                setMargins(Ui.dp(c, 4), Ui.dp(c, 6), Ui.dp(c, 4), Ui.dp(c, 6))
            }
            for (k in t.indices.reversed()) {
                val nut = android.view.View(c)
                nut.background = Ui.shape(nutColors[t[k]], Ui.dp(c, 10).toFloat(), Ui.dp(c, 2), 0x55000000)
                nut.layoutParams = LayoutParams(Ui.dp(c, 46), Ui.dp(c, 32)).apply {
                    setMargins(0, Ui.dp(c, 2), 0, Ui.dp(c, 2))
                }
                v.addView(nut)
            }
        } else {
            v.orientation = HORIZONTAL
            v.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
            v.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 66)).apply {
                setMargins(0, Ui.dp(c, 6), 0, Ui.dp(c, 6))
            }
            for (k in t) {
                val tv = TextView(c)
                tv.text = birds[k]
                tv.textSize = 32f
                tv.setPadding(Ui.dp(c, 6), 0, Ui.dp(c, 6), 0)
                v.addView(tv)
            }
        }
        return v
    }

    private fun draw() {
        val c = context
        removeAllViews()
        val msg = Ui.header(c, this, if (nuts) "Porcas · Nível $level" else "Pássaros · Nível $level") { onBack() }

        if (nuts) {
            var rowView: LinearLayout? = null
            for (i in tubes.indices) {
                if (i % 5 == 0) {
                    rowView = Ui.row(c)
                    addView(rowView)
                }
                rowView!!.addView(tubeView(i))
            }
        } else {
            for (i in tubes.indices) addView(tubeView(i))
        }

        val bar = Ui.row(c)
        bar.addView(Ui.smallBtn(c, "↩ Desfazer") { undo() })
        bar.addView(Ui.smallBtn(c, "🔄 Reiniciar") { start() })
        addView(bar)

        if (solved()) {
            msg.text = "🎉 Parabéns!"
            addView(Ui.btn(c, "Próximo nível →") { level++; start() })
        }
    }
}
