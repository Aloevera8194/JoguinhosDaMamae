package com.oliveira.joguinhosdamamae

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
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

    private var level = 1
    private var moves = 0
    private var tubes = mutableListOf<MutableList<Int>>()
    private val history = mutableListOf<List<List<Int>>>()
    private var sel = -1
    private var hintFrom = -1
    private var hintTo = -1
    private var msgView: TextView? = null
    private val stacks = mutableListOf<LinearLayout>()

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 12), Ui.dp(c, 44), Ui.dp(c, 12), Ui.dp(c, 24))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            if (nuts) intArrayOf(0xFF283593.toInt(), 0xFF0D1333.toInt())
            else intArrayOf(0xFF81D4FA.toInt(), 0xFF0288D1.toInt())
        )
        clipChildren = false
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
        moves = 0
        hintFrom = -1
        hintTo = -1
        draw()
    }

    private fun undo() {
        if (history.isNotEmpty()) {
            tubes = history.removeAt(history.lastIndex).map { it.toMutableList() }.toMutableList()
            sel = -1
            hintFrom = -1
            hintTo = -1
            if (moves > 0) moves--
            draw()
        }
    }

    private fun findMove(): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null
        for (a in tubes.indices) {
            val ta = tubes[a]
            if (ta.isEmpty()) continue
            val col = ta.last()
            if (ta.size == 4 && ta.all { it == col }) continue
            val uniform = ta.all { it == col }
            for (b in tubes.indices) {
                if (a == b) continue
                val tb = tubes[b]
                if (tb.isEmpty()) {
                    if (!uniform && best == null) best = Pair(a, b)
                } else if (tb.last() == col && tb.size < 4) {
                    return Pair(a, b)
                }
            }
        }
        return best
    }

    private fun hint() {
        if (solved()) return
        val m = findMove()
        if (m == null) {
            msgView?.text = "Sem jogadas. Desfaça ou reinicie"
            return
        }
        sel = -1
        hintFrom = m.first
        hintTo = m.second
        draw()
        postDelayed({
            if (hintFrom == m.first && hintTo == m.second) {
                hintFrom = -1
                hintTo = -1
                draw()
            }
        }, 2200)
    }

    private fun tap(i: Int) {
        if (solved()) return
        hintFrom = -1
        hintTo = -1
        var from = -1
        var moved = 0
        if (sel < 0) {
            if (tubes[i].isNotEmpty()) sel = i
        } else {
            val a = tubes[sel]
            val b = tubes[i]
            if (sel != i && a.isNotEmpty()) {
                val col = a.last()
                if (b.isEmpty() || (b.last() == col && b.size < 4)) {
                    history.add(tubes.map { it.toList() })
                    from = sel
                    while (a.isNotEmpty() && a.last() == col && b.size < 4) {
                        b.add(a.removeAt(a.lastIndex))
                        moved++
                    }
                }
            }
            sel = -1
        }
        if (moved > 0) moves++
        draw()
        if (from >= 0 && moved > 0) animateMove(from, i, moved)
        if (sel >= 0) liftTop(sel)
        if (solved()) celebrate()
    }

    private fun topChild(s: LinearLayout, j: Int): View? =
        if (nuts) s.getChildAt(j) else s.getChildAt(s.childCount - 1 - j)

    private fun liftTop(i: Int) {
        val v = topChild(stacks[i], 0) ?: return
        v.animate().translationY(-Ui.dp(context, 16).toFloat()).setDuration(160)
            .setInterpolator(OvershootInterpolator()).start()
    }

    private fun animateMove(from: Int, to: Int, count: Int) {
        val s = stacks[from]
        val d = stacks[to]
        post {
            val a = IntArray(2)
            val b = IntArray(2)
            s.getLocationOnScreen(a)
            d.getLocationOnScreen(b)
            val dx = (a[0] - b[0]).toFloat()
            val dy = (a[1] - b[1]).toFloat() - Ui.dp(context, 40)
            for (j in 0 until count) {
                val v = topChild(d, j) ?: continue
                v.translationX = dx
                v.translationY = dy
                v.alpha = 0.3f
                v.scaleX = 0.8f
                v.scaleY = 0.8f
                v.animate().translationX(0f).translationY(0f).alpha(1f).scaleX(1f).scaleY(1f)
                    .setStartDelay(j * 70L).setDuration(320)
                    .setInterpolator(OvershootInterpolator(1.4f)).start()
            }
        }
    }

    private fun celebrate() {
        var n = 0L
        for (s in stacks) for (k in 0 until s.childCount) {
            val v = s.getChildAt(k)
            v.animate().scaleX(1.25f).scaleY(1.25f).setStartDelay(n * 25).setDuration(180)
                .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(220).start() }
                .start()
            n++
        }
    }

    private fun pill(label: String, bg: Int, onClick: () -> Unit): Button {
        val c = context
        return Button(c).apply {
            text = label
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = Ui.shape(bg, Ui.dp(c, 14).toFloat())
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, Ui.dp(c, 46), 1f).apply {
                setMargins(Ui.dp(c, 4), Ui.dp(c, 6), Ui.dp(c, 4), Ui.dp(c, 4))
            }
        }
    }

    private fun chip(label: String): TextView {
        val c = context
        return TextView(c).apply {
            text = label
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(Ui.dp(c, 14), Ui.dp(c, 6), Ui.dp(c, 14), Ui.dp(c, 6))
            background = Ui.shape(0x33FFFFFF, Ui.dp(c, 16).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4)) }
        }
    }

    private fun tubeView(i: Int): FrameLayout {
        val c = context
        val d = c.resources.displayMetrics.density
        val t = tubes[i]
        val picked = sel == i
        val hinted = hintFrom == i || hintTo == i
        val fl = FrameLayout(c)
        fl.clipChildren = false
        fl.setOnClickListener { tap(i) }
        val strokeColor = when {
            picked -> 0xFF69F0AE.toInt()
            hinted -> 0xFFFFD54F.toInt()
            else -> 0x22FFFFFF
        }
        fl.background = Ui.shape(
            if (picked) 0x3300E676 else 0x14FFFFFF, Ui.dp(c, 18).toFloat(),
            Ui.dp(c, 3), strokeColor
        )
        val stack = LinearLayout(c)
        stack.clipChildren = false
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
            fl.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(c, 78)).apply {
                setMargins(0, Ui.dp(c, 4), 0, Ui.dp(c, 4))
            }
            fl.addView(BranchView(c, d))
            stack.orientation = HORIZONTAL
            stack.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            stack.setPadding(0, 0, 0, Ui.dp(c, 10))
            for (k in t) {
                stack.addView(BirdView(c, k), LinearLayout.LayoutParams(Ui.dp(c, 46), Ui.dp(c, 46)).apply {
                    setMargins(Ui.dp(c, 6), 0, Ui.dp(c, 6), 0)
                })
            }
        }
        fl.addView(stack, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        stacks.add(stack)
        return fl
    }

    private fun draw() {
        val c = context
        removeAllViews()
        stacks.clear()
        clipChildren = false

        val bar = Ui.row(c)
        bar.addView(Ui.smallBtn(c, "← Menu") { onBack() })
        bar.addView(Ui.text(c, if (nuts) "Porcas" else "Pássaros", 20f, true).apply {
            setTextColor(Color.WHITE)
            setPadding(Ui.dp(c, 12), 0, 0, 0)
        })
        addView(bar)

        val info = Ui.row(c)
        info.addView(chip("Nível $level"))
        info.addView(chip("Jogadas $moves"))
        addView(info)

        val msg = Ui.text(c, "", 18f, true).apply {
            setTextColor(0xFFFFEB3B.toInt())
            setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8))
        }
        msgView = msg
        addView(msg)

        if (nuts) {
            var rowView: LinearLayout? = null
            for (i in tubes.indices) {
                if (i % 5 == 0) {
                    val r = Ui.row(c)
                    r.clipChildren = false
                    addView(r)
                    rowView = r
                }
                rowView!!.addView(tubeView(i))
            }
        } else {
            for (i in tubes.indices) addView(tubeView(i))
        }

        val bottom = Ui.row(c)
        bottom.addView(pill("💡 Dica", 0xFFF9A825.toInt()) { hint() })
        bottom.addView(pill("↩ Desfazer", 0xFF5C6BC0.toInt()) { undo() })
        bottom.addView(pill("🔄 Reiniciar", 0xFF26A69A.toInt()) { start() })
        addView(bottom)

        if (solved()) {
            msg.text = "🎉 Parabéns!"
            addView(Ui.btn(c, "Próximo nível →") { level++; start() })
        }
    }
}
