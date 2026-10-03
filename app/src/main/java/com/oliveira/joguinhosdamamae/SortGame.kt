package com.oliveira.joguinhosdamamae

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class NutView(c: Context, private val col: Int) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val path = Path().apply {
            moveTo(w * 0.22f, 0f); lineTo(w * 0.78f, 0f); lineTo(w, h * 0.5f)
            lineTo(w * 0.78f, h); lineTo(w * 0.22f, h); lineTo(0f, h * 0.5f); close()
        }
        p.style = Paint.Style.FILL
        p.color = col
        cv.drawPath(path, p)
        cv.save()
        cv.clipPath(path)
        p.color = 0x33000000
        cv.drawRect(0f, h * 0.65f, w, h, p)
        p.color = 0x55FFFFFF
        cv.drawRect(0f, 0f, w, h * 0.28f, p)
        cv.restore()
        p.color = 0xFFECEFF1.toInt()
        cv.drawOval(RectF(w * 0.34f, h * 0.26f, w * 0.66f, h * 0.64f), p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = 0x66000000
        cv.drawPath(path, p)
    }
}

class BoltView(c: Context, private val d: Float) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.color = 0xFFB0BEC5.toInt()
        cv.drawRoundRect(RectF(w / 2 - 7 * d, 0f, w / 2 + 7 * d, h - 8 * d), 6 * d, 6 * d, p)
        p.color = 0x33000000
        var y = 6 * d
        while (y < h - 14 * d) {
            cv.drawRect(w / 2 - 7 * d, y, w / 2 + 7 * d, y + 1.5f * d, p)
            y += 8 * d
        }
        p.color = 0xFFCFD8DC.toInt()
        cv.drawRoundRect(RectF(w * 0.04f, h - 14 * d, w * 0.96f, h), 7 * d, 7 * d, p)
    }
}

class BranchView(c: Context, private val d: Float) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.color = 0xFF6D4C41.toInt()
        cv.drawRoundRect(RectF(0f, h - 11 * d, w, h - 3 * d), 4 * d, 4 * d, p)
        p.color = 0xFF8D6E63.toInt()
        cv.drawRoundRect(RectF(0f, h - 11 * d, w, h - 8 * d), 3 * d, 3 * d, p)
    }
}

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
        setPadding(Ui.dp(c, 12), Ui.dp(c, 28), Ui.dp(c, 12), Ui.dp(c, 24))
        setBackgroundColor(if (nuts) 0xFF1B2250.toInt() else 0xFF7CC6D4.toInt())
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

    private fun tubeView(i: Int): FrameLayout {
        val c = context
        val d = c.resources.displayMetrics.density
        val t = tubes[i]
        val selected = sel == i
        val fl = FrameLayout(c)
        fl.setOnClickListener { tap(i) }
        val hl = Ui.shape(
            if (selected) 0x2200E676 else 0x00000000, Ui.dp(c, 14).toFloat(),
            Ui.dp(c, 3), if (selected) 0xFF69F0AE.toInt() else 0x00000000
        )
        fl.background = hl
        val stack = LinearLayout(c)
        if (nuts) {
            fl.layoutParams = LinearLayout.LayoutParams(Ui.dp(c, 56), Ui.dp(c, 190)).apply {
                setMargins(Ui.dp(c, 3), Ui.dp(c, 6), Ui.dp(c, 3), Ui.dp(c, 6))
            }
            fl.addView(BoltView(c, d))
            stack.orientation = VERTICAL
            stack.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            stack.setPadding(0, 0, 0, Ui.dp(c, 12))
            for (k in t.indices.reversed()) {
                stack.addView(NutView(c, nutColors[t[k]]), LinearLayout.LayoutParams(Ui.dp(c, 48), Ui.dp(c, 30)).apply {
                    setMargins(0, Ui.dp(c, 1), 0, Ui.dp(c, 1))
                })
            }
        } else {
            fl.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(c, 74)).apply {
                setMargins(0, Ui.dp(c, 4), 0, Ui.dp(c, 4))
            }
            fl.addView(BranchView(c, d))
            stack.orientation = HORIZONTAL
            stack.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            stack.setPadding(0, 0, 0, Ui.dp(c, 8))
            for (k in t) {
                stack.addView(TextView(c).apply {
                    text = birds[k]
                    textSize = 36f
                    setPadding(Ui.dp(c, 6), 0, Ui.dp(c, 6), 0)
                })
            }
        }
        fl.addView(stack, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        return fl
    }

    private fun draw() {
        val c = context
        removeAllViews()

        val bar = Ui.row(c)
        bar.addView(Ui.smallBtn(c, "← Menu") { onBack() })
        bar.addView(Ui.text(c, if (nuts) "Porcas · Nível $level" else "Pássaros · Nível $level", 18f, true).apply {
            setTextColor(Color.WHITE)
            setPadding(Ui.dp(c, 12), 0, 0, 0)
        })
        addView(bar)
        val msg = Ui.text(c, "", 18f, true).apply {
            setTextColor(0xFFFFEB3B.toInt())
            setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8))
        }
        addView(msg)

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

        val bottom = Ui.row(c)
        bottom.addView(Ui.smallBtn(c, "↩ Desfazer") { undo() })
        bottom.addView(Ui.smallBtn(c, "🔄 Reiniciar") { start() })
        addView(bottom)

        if (solved()) {
            msg.text = "🎉 Parabéns!"
            addView(Ui.btn(c, "Próximo nível →") { level++; start() })
        }
    }
}
