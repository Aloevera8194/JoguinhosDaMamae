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
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

val MATCH_COLORS = intArrayOf(
    0xFFE53935.toInt(), 0xFF1E88E5.toInt(), 0xFF43A047.toInt(),
    0xFFFDD835.toInt(), 0xFFFB8C00.toInt(), 0xFF8E24AA.toInt()
)

class MatchBoardView(ctx: Context) : View(ctx) {
    val rows = 6
    val cols = 5
    var types = 4
    var onScore: (Int) -> Unit = {}
    var onMsg: (String) -> Unit = {}

    private val g = Array(rows) { IntArray(cols) }
    private val offX = Array(rows) { FloatArray(cols) }
    private val offY = Array(rows) { FloatArray(cols) }
    private val fall = Array(rows) { FloatArray(cols) }
    private val scl = Array(rows) { FloatArray(cols) { 1f } }
    private var busy = false
    private var selR = -1
    private var selC = -1
    private var hintPair: Pair<Int, Int>? = null
    private var cell = 0f
    private var downX = 0f
    private var downY = 0f
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val d = ctx.resources.displayMetrics.density

    fun isBusy() = busy

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        val h = (w.toFloat() / cols * rows).toInt()
        setMeasuredDimension(w, h)
    }

    private fun wouldMatch(r: Int, c: Int, v: Int): Boolean {
        if (c >= 2 && g[r][c - 1] == v && g[r][c - 2] == v) return true
        if (r >= 2 && g[r - 1][c] == v && g[r - 2][c] == v) return true
        return false
    }

    fun newBoard(t: Int) {
        types = t
        do {
            for (r in 0 until rows) for (c in 0 until cols) {
                var v = Random.nextInt(types)
                while (wouldMatch(r, c, v)) v = Random.nextInt(types)
                g[r][c] = v
            }
        } while (findMove() == null)
        for (r in 0 until rows) for (c in 0 until cols) {
            offX[r][c] = 0f
            offY[r][c] = 0f
            fall[r][c] = 0f
            scl[r][c] = 1f
        }
        selR = -1
        selC = -1
        hintPair = null
        busy = false
        invalidate()
    }

    private fun swapCells(r1: Int, c1: Int, r2: Int, c2: Int) {
        val t = g[r1][c1]
        g[r1][c1] = g[r2][c2]
        g[r2][c2] = t
    }

    private fun findMatches(): Set<Int> {
        val m = HashSet<Int>()
        for (r in 0 until rows) {
            var c = 0
            while (c < cols) {
                val v = g[r][c]
                var e = c
                while (e + 1 < cols && v >= 0 && g[r][e + 1] == v) e++
                if (v >= 0 && e - c + 1 >= 3) for (k in c..e) m.add(r * cols + k)
                c = e + 1
            }
        }
        for (c in 0 until cols) {
            var r = 0
            while (r < rows) {
                val v = g[r][c]
                var e = r
                while (e + 1 < rows && v >= 0 && g[e + 1][c] == v) e++
                if (v >= 0 && e - r + 1 >= 3) for (k in r..e) m.add(k * cols + c)
                r = e + 1
            }
        }
        return m
    }

    private fun findMove(): Pair<Int, Int>? {
        for (r in 0 until rows) for (c in 0 until cols) {
            if (c + 1 < cols) {
                swapCells(r, c, r, c + 1)
                val ok = findMatches().isNotEmpty()
                swapCells(r, c, r, c + 1)
                if (ok) return Pair(r * cols + c, r * cols + c + 1)
            }
            if (r + 1 < rows) {
                swapCells(r, c, r + 1, c)
                val ok = findMatches().isNotEmpty()
                swapCells(r, c, r + 1, c)
                if (ok) return Pair(r * cols + c, (r + 1) * cols + c)
            }
        }
        return null
    }

    private fun reshuffle() {
        val vals = ArrayList<Int>()
        for (r in 0 until rows) for (c in 0 until cols) vals.add(g[r][c])
        var tries = 0
        while (tries < 300) {
            tries++
            vals.shuffle()
            var i = 0
            for (r in 0 until rows) for (c in 0 until cols) {
                g[r][c] = vals[i]
                i++
            }
            if (findMatches().isEmpty() && findMove() != null) return
        }
        newBoard(types)
    }

    private fun play(ms: Long, step: (Float) -> Unit, end: () -> Unit) {
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = ms
        a.addUpdateListener {
            step(it.animatedValue as Float)
            invalidate()
        }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                end()
            }
        })
        a.start()
    }

    private fun setSwap(r1: Int, c1: Int, r2: Int, c2: Int, k: Float) {
        offX[r1][c1] = (c2 - c1) * k
        offY[r1][c1] = (r2 - r1) * k
        offX[r2][c2] = (c1 - c2) * k
        offY[r2][c2] = (r1 - r2) * k
    }

    private fun trySwap(r1: Int, c1: Int, r2: Int, c2: Int) {
        if (busy) return
        busy = true
        hintPair = null
        swapCells(r1, c1, r2, c2)
        setSwap(r1, c1, r2, c2, 1f)
        play(180, { t -> setSwap(r1, c1, r2, c2, 1f - t) }, {
            setSwap(r1, c1, r2, c2, 0f)
            if (findMatches().isEmpty()) {
                swapCells(r1, c1, r2, c2)
                setSwap(r1, c1, r2, c2, 1f)
                play(180, { t -> setSwap(r1, c1, r2, c2, 1f - t) }, {
                    setSwap(r1, c1, r2, c2, 0f)
                    busy = false
                    onMsg("Essa troca não forma 3 iguais")
                })
            } else {
                onMsg("")
                cascade(1)
            }
        })
    }

    private fun gravity() {
        for (c in 0 until cols) {
            var write = rows - 1
            for (r in rows - 1 downTo 0) {
                if (g[r][c] >= 0) {
                    if (write != r) {
                        g[write][c] = g[r][c]
                        g[r][c] = -1
                        fall[write][c] = (r - write).toFloat()
                    }
                    write--
                }
            }
            val n = write + 1
            for (r in 0 until n) {
                g[r][c] = Random.nextInt(types)
                fall[r][c] = -n.toFloat()
            }
        }
    }

    private fun cascade(mult: Int) {
        val m = findMatches()
        if (m.isEmpty()) {
            finishTurn()
            return
        }
        onScore(m.size * 10 * mult)
        if (mult > 1) onMsg("Combo x$mult!")
        play(240, { t ->
            for (i in m) scl[i / cols][i % cols] = 1f - t
        }, {
            for (i in m) {
                g[i / cols][i % cols] = -1
                scl[i / cols][i % cols] = 1f
            }
            gravity()
            play(340, { t ->
                val k = (1f - t) * (1f - t)
                for (r in 0 until rows) for (c in 0 until cols) offY[r][c] = fall[r][c] * k
            }, {
                for (r in 0 until rows) for (c in 0 until cols) {
                    offY[r][c] = 0f
                    fall[r][c] = 0f
                }
                cascade(mult + 1)
            })
        })
    }

    private fun finishTurn() {
        if (findMove() == null) {
            reshuffle()
            onMsg("Sem jogadas: peças embaralhadas")
        }
        busy = false
        invalidate()
    }

    fun showHint() {
        if (busy) return
        val m = findMove() ?: return
        hintPair = m
        selR = -1
        selC = -1
        invalidate()
        postDelayed({
            if (hintPair == m) {
                hintPair = null
                invalidate()
            }
        }, 2500)
    }

    private fun tapCell(r: Int, c: Int) {
        hintPair = null
        if (selR < 0) {
            selR = r
            selC = c
        } else if (selR == r && selC == c) {
            selR = -1
            selC = -1
        } else if (abs(selR - r) + abs(selC - c) == 1) {
            val r0 = selR
            val c0 = selC
            selR = -1
            selC = -1
            trySwap(r0, c0, r, c)
        } else {
            selR = r
            selC = c
        }
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        cell = width.toFloat() / cols
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (busy) return true
                val c0 = (downX / cell).toInt()
                val r0 = (downY / cell).toInt()
                if (r0 !in 0 until rows || c0 !in 0 until cols) return true
                val dx = e.x - downX
                val dy = e.y - downY
                if (hypot(dx, dy) > cell * 0.35f) {
                    var r1 = r0
                    var c1 = c0
                    if (abs(dx) > abs(dy)) {
                        c1 += if (dx > 0) 1 else -1
                    } else {
                        r1 += if (dy > 0) 1 else -1
                    }
                    if (r1 in 0 until rows && c1 in 0 until cols) {
                        selR = -1
                        selC = -1
                        trySwap(r0, c0, r1, c1)
                    }
                } else {
                    tapCell(r0, c0)
                }
                return true
            }
        }
        return true
    }

    private fun drawPiece(cv: Canvas, type: Int, cx: Float, cy: Float, s: Float) {
        val r = s / 2f
        path.reset()
        when (type) {
            0 -> path.addCircle(cx, cy, r, Path.Direction.CW)
            1 -> path.addRoundRect(
                RectF(cx - r * 0.92f, cy - r * 0.92f, cx + r * 0.92f, cy + r * 0.92f),
                r * 0.3f, r * 0.3f, Path.Direction.CW
            )
            2 -> {
                path.moveTo(cx, cy - r)
                path.lineTo(cx + r, cy + r * 0.85f)
                path.lineTo(cx - r, cy + r * 0.85f)
                path.close()
            }
            3 -> {
                for (k in 0 until 10) {
                    val rad = if (k % 2 == 0) r else r * 0.45f
                    val a = -PI / 2 + k * PI / 5
                    val x = cx + (rad * cos(a)).toFloat()
                    val y = cy + (rad * sin(a)).toFloat()
                    if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            4 -> {
                path.moveTo(cx, cy - r)
                path.lineTo(cx + r * 0.9f, cy)
                path.lineTo(cx, cy + r)
                path.lineTo(cx - r * 0.9f, cy)
                path.close()
            }
            else -> {
                for (k in 0 until 6) {
                    val a = k * PI / 3
                    val x = cx + (r * cos(a)).toFloat()
                    val y = cy + (r * sin(a)).toFloat()
                    if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
        }
        fill.style = Paint.Style.FILL
        fill.color = MATCH_COLORS[type % MATCH_COLORS.size]
        cv.drawPath(path, fill)
        line.style = Paint.Style.STROKE
        line.strokeWidth = 3 * d
        line.strokeJoin = Paint.Join.ROUND
        line.color = 0xFF263238.toInt()
        cv.drawPath(path, line)
        fill.color = 0x66FFFFFF
        cv.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.16f, fill)
    }

    override fun onDraw(cv: Canvas) {
        cell = width.toFloat() / cols
        fill.style = Paint.Style.FILL
        fill.color = Color.WHITE
        cv.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 18 * d, 18 * d, fill)
        val gap = 2.5f * d
        val hp = hintPair
        for (r in 0 until rows) for (c in 0 until cols) {
            val idx = r * cols + c
            val isHint = hp != null && (hp.first == idx || hp.second == idx)
            fill.color = if (isHint) 0xFFFFF59D.toInt() else 0xFFECEFF1.toInt()
            val rect = RectF(c * cell + gap, r * cell + gap, (c + 1) * cell - gap, (r + 1) * cell - gap)
            cv.drawRoundRect(rect, 10 * d, 10 * d, fill)
            if (isHint) {
                line.style = Paint.Style.STROKE
                line.strokeWidth = 4 * d
                line.color = 0xFFFFA000.toInt()
                cv.drawRoundRect(rect, 10 * d, 10 * d, line)
            }
        }
        for (r in 0 until rows) for (c in 0 until cols) {
            val v = g[r][c]
            if (v < 0) continue
            val cx = (c + 0.5f + offX[r][c]) * cell
            val cy = (r + 0.5f + offY[r][c]) * cell
            val picked = r == selR && c == selC
            if (picked) {
                line.style = Paint.Style.STROKE
                line.strokeWidth = 5 * d
                line.color = 0xFFFF6F00.toInt()
                cv.drawRoundRect(
                    RectF(c * cell + gap, r * cell + gap, (c + 1) * cell - gap, (r + 1) * cell - gap),
                    10 * d, 10 * d, line
                )
            }
            val s = cell * 0.74f * scl[r][c] * (if (picked) 1.1f else 1f)
            drawPiece(cv, v, cx, cy, s)
        }
    }
}

class MatchGame(
    c: Context,
    private val onBack: () -> Unit
) : LinearLayout(c) {

    private var level = Progress.level(c, "combinar3")
    private var score = 0
    private var reached = false

    private lateinit var board: MatchBoardView
    private lateinit var info: TextView
    private lateinit var msg: TextView
    private lateinit var nextHolder: LinearLayout

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 12), Ui.dp(c, 36), Ui.dp(c, 12), Ui.dp(c, 24))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF00796B.toInt(), 0xFF004D40.toInt())
        )
        build()
        start()
    }

    private fun goal() = 250 + 200 * level

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

    private fun updateInfo() {
        info.text = "Nível $level   •   Pontos $score / ${goal()}"
    }

    private fun build() {
        val c = context
        val bar = Ui.row(c)
        bar.addView(Ui.smallBtn(c, "← Menu") { onBack() })
        bar.addView(Ui.text(c, "Combinar 3", 24f, true).apply {
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

        msg = Ui.text(c, "", 20f, true).apply {
            setTextColor(0xFFFFF59D.toInt())
            setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8))
        }
        addView(msg)

        board = MatchBoardView(c)
        board.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        board.onScore = { p ->
            score += p
            updateInfo()
            if (!reached && score >= goal()) {
                reached = true
                msg.text = "🎉 Meta alcançada!"
                nextHolder.removeAllViews()
                nextHolder.addView(Ui.btn(context, "Próximo nível →") {
                    if (!board.isBusy()) {
                        level++
                        Progress.setLevel(context, "combinar3", level)
                        start()
                    }
                })
            }
        }
        board.onMsg = { t -> if (!reached) msg.text = t }
        addView(board)

        val actions = Ui.row(c)
        actions.addView(pill("💡 Dica", 0xFFF9A825.toInt()) { board.showHint() })
        actions.addView(pill("🔄 Novo jogo", 0xFF3949AB.toInt()) {
            if (!board.isBusy()) start()
        })
        addView(actions)

        nextHolder = LinearLayout(c)
        nextHolder.orientation = LinearLayout.VERTICAL
        addView(nextHolder)
    }

    private fun start() {
        score = 0
        reached = false
        msg.text = ""
        nextHolder.removeAllViews()
        board.newBoard(minOf(3 + level, 6))
        updateInfo()
    }
}
