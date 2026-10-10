package com.oliveira.joguinhosdamamae

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MemoryGame(
    c: Context,
    private val onBack: () -> Unit
) : LinearLayout(c) {

    private class Card(val id: Int, val sym: String) {
        var up = false
        var matched = false
    }

    private val symbols = listOf(
        "🍎", "🍌", "🍇", "🍓", "🍊", "🍒", "🌻", "🐶", "🐱", "🐟", "🚗", "⭐", "🏠", "🎈", "🍰", "🍉", "🍍", "🥕", "🐸", "🐢", "🦋", "🌈", "🚲", "🎁"
    )

    private var level = Progress.level(c, "memoria")
    private var moves = 0
    private var cols = 3
    private var cards = listOf<Card>()
    private val views = mutableListOf<TextView>()
    private var first: Card? = null
    private var busy = false
    private var hintIds: Set<Int> = emptySet()

    private lateinit var info: TextView
    private lateinit var msg: TextView
    private lateinit var boardBox: LinearLayout
    private lateinit var nextHolder: LinearLayout

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 12), Ui.dp(c, 36), Ui.dp(c, 12), Ui.dp(c, 24))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFFEC407A.toInt(), 0xFF880E4F.toInt())
        )
        build()
        start()
    }

    private fun pill(label: String, bg: Int, onClick: () -> Unit): Button {
        val c = context
        return Button(c).apply {
            text = label
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = Ui.shape(bg, Ui.dp(c, 16).toFloat())
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, Ui.dp(c, 56), 1f).apply {
                setMargins(Ui.dp(c, 4), Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 4))
            }
        }
    }

    private fun build() {
        val c = context
        val bar = Ui.row(c)
        bar.addView(Ui.smallBtn(c, "← Menu") { onBack() })
        bar.addView(Ui.text(c, "Memória", 24f, true).apply {
            setTextColor(Color.WHITE)
            setPadding(Ui.dp(c, 12), 0, 0, 0)
        })
        addView(bar)

        info = TextView(c).apply {
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(Ui.dp(c, 16), Ui.dp(c, 8), Ui.dp(c, 16), Ui.dp(c, 8))
            background = Ui.shape(0x33FFFFFF, Ui.dp(c, 18).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, Ui.dp(c, 8), 0, Ui.dp(c, 4)) }
        }
        addView(info)

        msg = Ui.text(c, "", 22f, true).apply {
            setTextColor(0xFFFFF59D.toInt())
            setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8))
        }
        addView(msg)

        boardBox = LinearLayout(c)
        boardBox.orientation = LinearLayout.VERTICAL
        boardBox.gravity = Gravity.CENTER_HORIZONTAL
        addView(boardBox)

        val actions = Ui.row(c)
        actions.addView(pill("💡 Dica", 0xFFF9A825.toInt()) { showHint() })
        actions.addView(pill("🔄 Novo jogo", 0xFF3949AB.toInt()) { start() })
        addView(actions)

        nextHolder = LinearLayout(c)
        nextHolder.orientation = LinearLayout.VERTICAL
        addView(nextHolder)
    }

    private fun start() {
        val pairs = when (level) {
            1 -> 6
            2 -> 8
            3 -> 10
            4 -> 12
            5 -> 14
            else -> 16
        }
        cols = if (pairs == 6) 3 else 4
        val chosen = symbols.shuffled().take(pairs)
        var n = 0
        cards = chosen.flatMap { listOf(it, it) }.shuffled().map { Card(n++, it) }
        moves = 0
        first = null
        busy = false
        hintIds = emptySet()
        msg.text = ""
        nextHolder.removeAllViews()
        render()
    }

    private fun faceSize(): Float = if (cols == 3) 58f else 44f

    private fun style(tv: TextView, card: Card) {
        val c = context
        val radius = Ui.dp(c, 18).toFloat()
        val thick = Ui.dp(c, 5)
        tv.gravity = Gravity.CENTER
        when {
            card.matched -> {
                tv.text = card.sym
                tv.textSize = faceSize()
                tv.setTextColor(Color.BLACK)
                tv.background = Ui.shape(0xFFC8E6C9.toInt(), radius, thick, 0xFF2E7D32.toInt())
            }
            card.up -> {
                tv.text = card.sym
                tv.textSize = faceSize()
                tv.setTextColor(Color.BLACK)
                tv.background = Ui.shape(Color.WHITE, radius, thick, 0xFFFFB300.toInt())
            }
            else -> {
                tv.text = "?"
                tv.textSize = 40f
                tv.setTypeface(null, Typeface.BOLD)
                tv.setTextColor(Color.WHITE)
                val edge = if (hintIds.contains(card.id)) 0xFFFFEB3B.toInt() else Color.WHITE
                tv.background = Ui.shape(0xFF283593.toInt(), radius, thick, edge)
            }
        }
    }

    private fun render() {
        val c = context
        boardBox.removeAllViews()
        views.clear()
        info.text = "Nível $level   •   Jogadas $moves"
        val density = c.resources.displayMetrics.density
        val screenW = c.resources.displayMetrics.widthPixels
        val gap = Ui.dp(c, 6)
        val avail = screenW - Ui.dp(c, 24) - gap * 2 * cols
        val w = avail / cols
        val h = (w * 1.1f).toInt()
        var idx = 0
        for (r in 0 until cards.size / cols) {
            val row = Ui.row(c)
            for (k in 0 until cols) {
                val card = cards[idx]
                val tv = TextView(c)
                style(tv, card)
                tv.cameraDistance = 8000f * density
                tv.layoutParams = LinearLayout.LayoutParams(w, h).apply {
                    setMargins(gap, gap, gap, gap)
                }
                tv.setOnClickListener { tap(card, tv) }
                row.addView(tv)
                views.add(tv)
                idx++
            }
            boardBox.addView(row)
        }
    }

    private fun flip(tv: TextView, card: Card, up: Boolean) {
        card.up = up
        tv.animate().rotationY(90f).setDuration(110).withEndAction {
            style(tv, card)
            tv.rotationY = -90f
            tv.animate().rotationY(0f).setDuration(110).start()
        }.start()
    }

    private fun restyle(card: Card) {
        val i = cards.indexOf(card)
        if (i >= 0 && i < views.size) style(views[i], card)
    }

    private fun tap(card: Card, tv: TextView) {
        if (busy || card.up || card.matched) return
        hintIds = emptySet()
        flip(tv, card, true)
        val f = first
        if (f == null) {
            first = card
            return
        }
        first = null
        moves++
        info.text = "Nível $level   •   Jogadas $moves"
        busy = true
        if (f.sym == card.sym) {
            postDelayed({
                f.matched = true
                card.matched = true
                restyle(f)
                restyle(card)
                tv.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                busy = false
                checkWin()
            }, 500)
        } else {
            postDelayed({
                val i1 = cards.indexOf(f)
                val i2 = cards.indexOf(card)
                if (i1 >= 0 && i2 >= 0 && i1 < views.size && i2 < views.size) {
                    flip(views[i1], f, false)
                    flip(views[i2], card, false)
                }
                busy = false
            }, maxOf(900, 1500 - level * 60).toLong())
        }
    }

    private fun checkWin() {
        if (cards.isEmpty() || !cards.all { it.matched }) return
        msg.text = "🎉 Parabéns! Achou todos!"
        nextHolder.removeAllViews()
        nextHolder.addView(Ui.btn(context, "Próximo nível →") {
            level++
            Progress.setLevel(context, "memoria", level)
            start()
        })
        var n = 0L
        for (v in views) {
            v.animate().scaleX(1.1f).scaleY(1.1f).setStartDelay(n * 40).setDuration(180)
                .withEndAction {
                    v.animate().scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(200).start()
                }.start()
            n++
        }
    }

    private fun restyleAll() {
        for (i in cards.indices) if (i < views.size) style(views[i], cards[i])
    }

    private fun showHint() {
        if (busy) return
        val f = first
        val hidden = cards.filter { !it.matched && !it.up }
        val target: List<Card>? = if (f != null) {
            hidden.filter { it.sym == f.sym }.take(1)
        } else {
            hidden.groupBy { it.sym }.values.firstOrNull { it.size >= 2 }?.take(2)
        }
        if (target == null || target.isEmpty()) return
        val ids = target.map { it.id }.toSet()
        hintIds = ids
        restyleAll()
        postDelayed({
            if (hintIds == ids) {
                hintIds = emptySet()
                restyleAll()
            }
        }, 2500)
    }
}
