package com.oliveira.joguinhosdamamae

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class MjTile(val l: Int, val r: Int, val c: Int, var s: String) {
    var gone = false
    var a = 1f
}

class MjLayout(val name: String, val rows: Int, val cols: Int, val cells: List<Triple<Int, Int, Int>>)

object MjLayouts {
    private fun rect(l: Int, r0: Int, r1: Int, c0: Int, c1: Int): List<Triple<Int, Int, Int>> {
        val out = ArrayList<Triple<Int, Int, Int>>()
        for (r in r0..r1) for (c in c0..c1) out.add(Triple(l, r, c))
        return out
    }

    val all: List<MjLayout> = listOf(
        MjLayout("Primeiros passos", 4, 5, rect(0, 0, 3, 0, 4)),
        MjLayout("Retângulo", 5, 6, rect(0, 0, 4, 0, 5)),
        MjLayout("Dois níveis", 5, 6, rect(0, 0, 4, 0, 5) + rect(1, 1, 3, 1, 4)),
        MjLayout("Clássico", 5, 6, rect(0, 0, 4, 0, 5) + rect(1, 1, 3, 1, 4) + rect(2, 2, 2, 2, 3)),
        MjLayout(
            "Cruz", 7, 6,
            rect(0, 0, 6, 2, 3) + rect(0, 2, 4, 0, 1) + rect(0, 2, 4, 4, 5) +
                rect(1, 1, 5, 2, 3) + rect(2, 2, 4, 2, 3) + rect(3, 3, 3, 2, 3)
        ),
        MjLayout("Torre", 6, 6, rect(0, 0, 5, 0, 5) + rect(1, 1, 4, 1, 4) + rect(2, 2, 3, 2, 3)),
        MjLayout(
            "Pirâmide", 7, 6,
            rect(0, 0, 6, 0, 5) + rect(1, 1, 5, 1, 4) + rect(2, 2, 4, 2, 3) + rect(3, 3, 3, 2, 3)
        ),
        MjLayout(
            "Fortaleza", 8, 6,
            rect(0, 0, 7, 0, 5) + rect(1, 1, 6, 1, 4) + rect(2, 2, 5, 2, 3) + rect(3, 3, 4, 2, 3)
        )
    )
}

class MahjongBoard(ctx: Context) : View(ctx) {
    var sel: MjTile? = null
    var hint: Set<MjTile> = emptySet()
    var isFree: (MjTile) -> Boolean = { true }
    var onTap: (MjTile) -> Unit = {}
    var rows = 5
    var cols = 6
    private var order: List<MjTile> = emptyList()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = ctx.resources.displayMetrics.density
    private var tw = 0f
    private var th = 0f
    private var fw = 0f
    private var fh = 0f
    private var off = 0f
    private var depth = 0f
    private var ox = 0f
    private var oy = 0f
    private var totalH = 0f

    fun load(list: List<MjTile>) {
        order = list.sortedWith(compareBy({ it.l }, { it.r }, { it.c }))
        requestLayout()
        invalidate()
    }

    private fun calc(w: Float) {
        val margin = 6 * d
        tw = (w - 2 * margin) / (cols + 0.3f)
        off = tw * 0.09f
        depth = tw * 0.07f
        fw = tw - 3 * d
        fh = fw * 1.22f
        th = fh + 3 * d
        ox = margin + 2 * off
        oy = margin + 2 * off
        totalH = oy + rows * th + depth + margin
    }

    private fun tx(t: MjTile) = ox + t.c * tw - t.l * off
    private fun ty(t: MjTile) = oy + t.r * th - t.l * off

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec).toFloat()
        calc(w)
        setMeasuredDimension(w.toInt(), totalH.toInt())
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) return true
        if (e.action == MotionEvent.ACTION_UP) {
            calc(width.toFloat())
            for (t in order.reversed()) {
                if (t.gone) continue
                val x = tx(t)
                val y = ty(t)
                if (e.x >= x && e.x <= x + fw && e.y >= y && e.y <= y + fh) {
                    onTap(t)
                    break
                }
            }
            return true
        }
        return true
    }

    override fun onDraw(cv: Canvas) {
        calc(width.toFloat())
        val rx = fw * 0.14f
        for (t in order) {
            if (t.gone && t.a <= 0.01f) continue
            val x = tx(t)
            val y = ty(t)
            val faded = t.gone && t.a < 1f
            if (faded) {
                cv.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), (255 * t.a).toInt())
                val sc = 0.6f + 0.4f * t.a
                cv.scale(sc, sc, x + fw / 2, y + fh / 2)
            } else {
                cv.save()
            }

            p.style = Paint.Style.FILL
            p.color = 0x33000000
            cv.drawRoundRect(RectF(x + depth + 1.5f * d, y + depth + 2.5f * d, x + fw + depth + 1.5f * d, y + fh + depth + 2.5f * d), rx, rx, p)
            p.color = 0xFF9C8B7A.toInt()
            cv.drawRoundRect(RectF(x + depth, y + depth, x + fw + depth, y + fh + depth), rx, rx, p)

            val face = RectF(x, y, x + fw, y + fh)
            p.color = when {
                sel === t -> 0xFFFFE27A.toInt()
                hint.contains(t) -> 0xFFC8F7C5.toInt()
                else -> 0xFFFFF9E6.toInt()
            }
            cv.drawRoundRect(face, rx, rx, p)

            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.5f * d
            p.color = 0xAAFFFFFF.toInt()
            cv.drawRoundRect(RectF(x + 2 * d, y + 2 * d, x + fw - 2 * d, y + fh - 2 * d), rx, rx, p)
            p.strokeWidth = 1f * d
            p.color = 0xFF8D7B6A.toInt()
            cv.drawRoundRect(face, rx, rx, p)

            p.style = Paint.Style.FILL
            p.color = Color.BLACK
            p.textAlign = Paint.Align.CENTER
            p.textSize = fh * 0.5f
            cv.drawText(t.s, x + fw / 2, y + fh / 2 - (p.descent() + p.ascent()) / 2, p)

            if (!t.gone && !isFree(t)) {
                p.color = 0x66223344
                cv.drawRoundRect(face, rx, rx, p)
            }
            if (sel === t || hint.contains(t)) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3f * d
                p.color = if (sel === t) 0xFFE65100.toInt() else 0xFF2E7D32.toInt()
                cv.drawRoundRect(face, rx, rx, p)
            }
            cv.restore()
        }
    }
}

class MahjongGame(
    c: Context,
    private val onBack: () -> Unit
) : LinearLayout(c) {

    private val symbols = listOf(
        "⚓", "🚢", "🗼", "🧭", "🗺️", "⛵", "🐚", "🦀", "🐙", "🌊",
        "🏝️", "🐬", "🐳", "🐠", "🦞", "🌴", "☀️", "🌙", "⭐", "🎣"
    )

    private var level = Progress.level(c, "mahjong")
    private var lay: MjLayout = MjLayouts.all[0]
    private var tiles = listOf<MjTile>()
    private var sel: MjTile? = null
    private var hint: Set<MjTile> = emptySet()
    private val history = ArrayList<Pair<MjTile, MjTile>>()
    private lateinit var board: MahjongBoard
    private lateinit var msg: TextView
    private lateinit var info: TextView
    private lateinit var nextHolder: LinearLayout

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 8), Ui.dp(c, 36), Ui.dp(c, 8), Ui.dp(c, 24))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF0E5A6B.toInt(), 0xFF14213D.toInt())
        )
        build()
        newGame()
    }

    private fun build() {
        val c = context
        val bar = Ui.row(c)
        bar.addView(Ui.smallBtn(c, "← Menu") { onBack() })
        bar.addView(Ui.text(c, "Mahjong", 20f, true).apply {
            setTextColor(Color.WHITE)
            setPadding(Ui.dp(c, 12), 0, 0, 0)
        })
        addView(bar)

        info = TextView(c).apply {
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(Ui.dp(c, 16), Ui.dp(c, 6), Ui.dp(c, 16), Ui.dp(c, 6))
            background = Ui.shape(0x33FFFFFF, Ui.dp(c, 16).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, Ui.dp(c, 6), 0, Ui.dp(c, 2)) }
        }
        addView(info)

        msg = Ui.text(c, "", 17f, true).apply {
            setTextColor(0xFFFFEB3B.toInt())
            setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8))
        }
        addView(msg)

        board = MahjongBoard(c)
        board.onTap = { t -> tap(t) }
        board.isFree = { t -> free(t) }
        board.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(board)

        val r1 = Ui.row(c)
        r1.addView(Ui.smallBtn(c, "💡 Dica") { showHint() })
        r1.addView(Ui.smallBtn(c, "↩ Desfazer") { undo() })
        addView(r1)
        val r2 = Ui.row(c)
        r2.addView(Ui.smallBtn(c, "🔀 Embaralhar") { shuffleRemaining() })
        r2.addView(Ui.smallBtn(c, "🔄 Novo jogo") { newGame() })
        addView(r2)

        nextHolder = LinearLayout(c)
        nextHolder.orientation = LinearLayout.VERTICAL
        addView(nextHolder)
    }

    private fun freeIn(rem: List<Triple<Int, Int, Int>>, p: Triple<Int, Int, Int>): Boolean {
        val top = rem.filter { it.second == p.second && it.third == p.third }.maxOf { it.first }
        if (top != p.first) return false
        val left = rem.any { it.first == p.first && it.second == p.second && it.third == p.third - 1 }
        val right = rem.any { it.first == p.first && it.second == p.second && it.third == p.third + 1 }
        return !left || !right
    }

    private fun deal(): List<MjTile> {
        val pos = lay.cells
        val pairs = pos.size / 2
        val distinct = minOf(symbols.size, minOf(pairs, 5 + level))
        while (true) {
            val rem = pos.toMutableList()
            val pairSyms = (0 until pairs).map { symbols[it % distinct] }.shuffled().toMutableList()
            val assign = HashMap<Triple<Int, Int, Int>, String>()
            var ok = true
            while (rem.isNotEmpty()) {
                val fr = rem.filter { freeIn(rem, it) }
                if (fr.size < 2) {
                    ok = false
                    break
                }
                val two = fr.shuffled().take(2)
                val sym = pairSyms.removeAt(pairSyms.lastIndex)
                assign[two[0]] = sym
                assign[two[1]] = sym
                rem.remove(two[0])
                rem.remove(two[1])
            }
            if (ok) return pos.map { MjTile(it.first, it.second, it.third, assign[it] ?: "?") }
        }
    }

    private fun has(l: Int, r: Int, c: Int) =
        tiles.any { !it.gone && it.l == l && it.r == r && it.c == c }

    private fun topAt(r: Int, c: Int): MjTile? =
        tiles.filter { !it.gone && it.r == r && it.c == c }.maxByOrNull { it.l }

    private fun free(t: MjTile) =
        !t.gone && topAt(t.r, t.c) === t && (!has(t.l, t.r, t.c - 1) || !has(t.l, t.r, t.c + 1))

    private fun hasMoves(): Boolean =
        tiles.filter { free(it) }.groupBy { it.s }.any { it.value.size >= 2 }

    private fun newGame() {
        lay = MjLayouts.all[minOf((level - 1) / 2, MjLayouts.all.size - 1)]
        tiles = deal()
        sel = null
        hint = emptySet()
        history.clear()
        msg.text = ""
        nextHolder.removeAllViews()
        info.text = "Nível $level  •  ${lay.name}"
        board.rows = lay.rows
        board.cols = lay.cols
        board.load(tiles)
        push()
    }

    private fun push() {
        board.sel = sel
        board.hint = hint
        board.invalidate()
        if (tiles.all { it.gone }) {
            msg.text = "🎉 Parabéns, limpou tudo!"
            if (nextHolder.childCount == 0) {
                nextHolder.addView(Ui.btn(context, "Próximo nível →") {
                    level++
                    Progress.setLevel(context, "mahjong", level)
                    newGame()
                })
            }
        } else {
            msg.text = if (!hasMoves()) "Sem jogadas. Toque em Embaralhar" else ""
        }
    }

    private fun tap(t: MjTile) {
        if (!free(t)) return
        hint = emptySet()
        val cur = sel
        if (cur == null) {
            sel = t
        } else if (cur === t) {
            sel = null
        } else if (cur.s == t.s) {
            match(cur, t)
            sel = null
        } else {
            sel = t
        }
        push()
    }

    private fun match(a: MjTile, b: MjTile) {
        a.gone = true
        b.gone = true
        history.add(Pair(a, b))
        val va = ValueAnimator.ofFloat(1f, 0f)
        va.duration = 280
        va.addUpdateListener { anim ->
            val v = anim.animatedValue as Float
            if (a.gone) a.a = v
            if (b.gone) b.a = v
            board.invalidate()
        }
        va.start()
    }

    private fun undo() {
        if (history.isEmpty()) return
        val (a, b) = history.removeAt(history.lastIndex)
        a.gone = false
        a.a = 1f
        b.gone = false
        b.a = 1f
        sel = null
        hint = emptySet()
        nextHolder.removeAllViews()
        push()
    }

    private fun showHint() {
        val g = tiles.filter { free(it) }.groupBy { it.s }.values.firstOrNull { it.size >= 2 }
        if (g == null) {
            msg.text = "Sem jogadas. Toque em Embaralhar"
            return
        }
        sel = null
        hint = setOf(g[0], g[1])
        push()
        postDelayed({
            hint = emptySet()
            push()
        }, 1800)
    }

    private fun shuffleRemaining() {
        val rem = tiles.filter { !it.gone }
        if (rem.isEmpty()) return
        for (k in 0 until 60) {
            val s = rem.map { it.s }.shuffled()
            rem.forEachIndexed { i, t -> t.s = s[i] }
            if (hasMoves()) break
        }
        sel = null
        hint = emptySet()
        push()
    }
}
