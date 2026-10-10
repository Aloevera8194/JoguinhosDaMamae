package com.oliveira.joguinhosdamamae

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import kotlin.random.Random

val SAND_COLORS = intArrayOf(
    0xFFEF5350.toInt(), 0xFFFFA726.toInt(), 0xFFFFEE58.toInt(),
    0xFF66BB6A.toInt(), 0xFF26C6DA.toInt(), 0xFF42A5F5.toInt(),
    0xFFAB47BC.toInt(), 0xFFEC407A.toInt(), 0xFFD7B38A.toInt()
)

class SandPotView(c: Context) : View(c) {
    var layers: List<Int> = emptyList()
    var picked = false
    var popCount = 0
    var drain: List<Int> = emptyList()
    var anim = 1f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = c.resources.displayMetrics.density
    private val glass = Path()

    private fun drawLayer(cv: Canvas, ci: Int, l: Float, r: Float, yb: Float, hgt: Float, seed: Int): Float {
        if (hgt < 0.5f) return yb
        val col = SAND_COLORS[ci % SAND_COLORS.size]
        val yt = yb - hgt
        p.style = Paint.Style.FILL
        p.color = col
        cv.drawRect(l, yt, r, yb, p)
        val rnd = Random(ci * 131 + seed * 17)
        for (k in 0 until 26) {
            val gx = l + rnd.nextFloat() * (r - l)
            val gy = yt + rnd.nextFloat() * hgt
            p.color = if (k % 2 == 0) shade(col, 0.78f) else shade(col, 1.22f)
            cv.drawCircle(gx, gy, (0.8f + rnd.nextFloat() * 0.9f) * d, p)
        }
        p.color = 0x44FFFFFF
        cv.drawRect(l, yt, r, yt + 1.6f * d, p)
        return yt
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val left = 8 * d
        val right = w - 8 * d
        val top = 16 * d
        val bottom = h - 6 * d
        val rt = 6 * d
        val rb = 24 * d
        glass.reset()
        glass.addRoundRect(
            RectF(left, top, right, bottom),
            floatArrayOf(rt, rt, rt, rt, rb, rb, rb, rb),
            Path.Direction.CW
        )
        p.style = Paint.Style.FILL
        p.color = 0x26FFFFFF
        cv.drawPath(glass, p)

        cv.save()
        cv.clipPath(glass)
        val layerH = (bottom - top - 3 * d) / 4f
        var y = bottom - 1.5f * d
        val total = layers.size
        for (i in 0 until total) {
            val f = if (i >= total - popCount) anim else 1f
            y = drawLayer(cv, layers[i], left, right, y, layerH * f, i)
        }
        val surface = y
        for (k in drain.indices) {
            y = drawLayer(cv, drain[k], left, right, y, layerH * (1f - anim), 50 + k)
        }
        cv.restore()

        if (popCount > 0 && anim < 1f && layers.isNotEmpty()) {
            p.style = Paint.Style.FILL
            p.color = SAND_COLORS[layers.last() % SAND_COLORS.size]
            cv.drawRect(w / 2 - 2 * d, 0f, w / 2 + 2 * d, surface, p)
        }

        p.style = Paint.Style.STROKE
        p.strokeWidth = 2 * d
        p.color = if (picked) 0xFF69F0AE.toInt() else 0x99FFFFFF.toInt()
        cv.drawPath(glass, p)
        p.style = Paint.Style.FILL
        p.color = 0x40FFFFFF
        cv.drawRoundRect(RectF(left + 5 * d, top + 10 * d, left + 9 * d, bottom - 28 * d), 2 * d, 2 * d, p)
        p.color = 0x66FFFFFF
        cv.drawRoundRect(RectF(left - 3 * d, top - 7 * d, right + 3 * d, top + 1 * d), 4 * d, 4 * d, p)
    }
}

class SandGame(
    c: Context,
    private val onBack: () -> Unit
) : LinearLayout(c) {

    private var level = Progress.level(c, "areia")
    private var tubes = mutableListOf<MutableList<Int>>()
    private val history = mutableListOf<List<List<Int>>>()
    private var sel = -1
    private val pots = mutableListOf<SandPotView>()

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 12), Ui.dp(c, 40), Ui.dp(c, 12), Ui.dp(c, 24))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF4A2F8F.toInt(), 0xFF1B1038.toInt())
        )
        clipChildren = false
        start()
    }

    private fun solved() = tubes.all { t -> t.isEmpty() || (t.size == 4 && t.all { it == t[0] }) }

    private fun start() {
        tubes = SortGen.generate(SortGen.colorsFor(level), SortGen.emptiesFor(level), SortGen.movesFor(level))
        history.clear()
        sel = -1
        render()
    }

    private fun undo() {
        if (history.isNotEmpty()) {
            tubes = history.removeAt(history.lastIndex).map { it.toMutableList() }.toMutableList()
            sel = -1
            render()
        }
    }

    private fun tap(i: Int) {
        if (solved()) return
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
        render()
        if (from >= 0 && moved > 0) pour(from, i, moved)
        if (solved()) celebrate()
    }

    private fun pour(from: Int, to: Int, moved: Int) {
        val src = pots[from]
        val dst = pots[to]
        val col = dst.layers.last()
        dst.popCount = moved
        src.drain = List(moved) { col }
        dst.anim = 0f
        src.anim = 0f
        val va = ValueAnimator.ofFloat(0f, 1f)
        va.duration = 450L + 90L * moved
        va.addUpdateListener { a ->
            val f = a.animatedValue as Float
            dst.anim = f
            src.anim = f
            dst.invalidate()
            src.invalidate()
        }
        va.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                dst.popCount = 0
                dst.anim = 1f
                src.drain = emptyList()
                src.anim = 1f
                dst.invalidate()
                src.invalidate()
            }
        })
        va.start()
    }

    private fun celebrate() {
        var n = 0L
        for (v in pots) {
            v.animate().scaleX(1.12f).scaleY(1.12f).setStartDelay(n * 60).setDuration(180)
                .withEndAction {
                    v.animate().scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(220).start()
                }
                .start()
            n++
        }
    }

    private fun render() {
        val c = context
        removeAllViews()
        pots.clear()

        val bar = Ui.row(c)
        bar.addView(Ui.smallBtn(c, "← Menu") { onBack() })
        bar.addView(Ui.text(c, "Areia · Nível $level", 18f, true).apply {
            setTextColor(Color.WHITE)
            setPadding(Ui.dp(c, 12), 0, 0, 0)
        })
        addView(bar)
        val msg = Ui.text(c, "", 18f, true).apply {
            setTextColor(0xFFFFEB3B.toInt())
            setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8))
        }
        addView(msg)

        var rowView: LinearLayout? = null
        for (i in tubes.indices) {
            if (i % 4 == 0) {
                val r = Ui.row(c)
                r.clipChildren = false
                addView(r)
                rowView = r
            }
            val v = SandPotView(c)
            v.layers = tubes[i].toList()
            v.picked = sel == i
            v.setOnClickListener { tap(i) }
            v.layoutParams = LinearLayout.LayoutParams(Ui.dp(c, 70), Ui.dp(c, 176)).apply {
                setMargins(Ui.dp(c, 4), Ui.dp(c, 6), Ui.dp(c, 4), Ui.dp(c, 6))
            }
            rowView!!.addView(v)
            pots.add(v)
            if (sel == i) {
                v.post {
                    v.animate().translationY(-Ui.dp(c, 14).toFloat()).setDuration(160)
                        .setInterpolator(OvershootInterpolator()).start()
                }
            }
        }

        val bottom = Ui.row(c)
        bottom.addView(Ui.smallBtn(c, "↩ Desfazer") { undo() })
        bottom.addView(Ui.smallBtn(c, "🔄 Reiniciar") { start() })
        addView(bottom)

        if (solved()) {
            msg.text = "🎉 Parabéns!"
            addView(Ui.btn(c, "Próximo nível →") { level++; Progress.setLevel(c, "areia", level); start() })
        }
    }
}
