package com.oliveira.joguinhosdamamae

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

val WS_COLORS = intArrayOf(
    0xFFEF5350.toInt(), 0xFF42A5F5.toInt(), 0xFFAB47BC.toInt(), 0xFFFFA726.toInt(),
    0xFF26A69A.toInt(), 0xFFEC407A.toInt(), 0xFF7E57C2.toInt(), 0xFF8D6E63.toInt(),
    0xFF5C6BC0.toInt(), 0xFF66BB6A.toInt()
)

class FoundLine(val word: String, val sr: Int, val sc: Int, val er: Int, val ec: Int, val color: Int)

class WordGridView(ctx: Context) : View(ctx) {
    var size = 8
    var grid: Array<CharArray> = Array(0) { CharArray(0) }
    var found: List<FoundLine> = emptyList()
    var hintCell = -1
    var onSelect: (String, Int, Int, Int, Int) -> Unit = { _, _, _, _, _ -> }

    private var dragging = false
    private var sr = 0
    private var sc = 0
    private var er = 0
    private var ec = 0
    private var cell = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = ctx.resources.displayMetrics.density

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val s = MeasureSpec.getSize(wSpec)
        setMeasuredDimension(s, s)
    }

    private fun snap(x: Float, y: Float) {
        val cx = (sc * cell + cell / 2).toDouble()
        val cy = (sr * cell + cell / 2).toDouble()
        val dx = x.toDouble() - cx
        val dy = y.toDouble() - cy
        if (hypot(dx, dy) < cell * 0.5) {
            er = sr
            ec = sc
            return
        }
        val ang = atan2(dy, dx)
        val k = Math.round(ang / (PI / 4)).toInt()
        val ux = cos(k * PI / 4)
        val uy = sin(k * PI / 4)
        val dc = Math.round(ux).toInt()
        val dr = Math.round(uy).toInt()
        val step = hypot(dc.toDouble(), dr.toDouble()) * cell
        val proj = dx * ux + dy * uy
        var len = Math.round(proj / step).toInt().coerceAtLeast(0)
        while (len > 0 && (sr + dr * len !in 0 until size || sc + dc * len !in 0 until size)) len--
        er = sr + dr * len
        ec = sc + dc * len
    }

    private fun finish() {
        if (sr == er && sc == ec) return
        val dr = Integer.signum(er - sr)
        val dc = Integer.signum(ec - sc)
        val len = max(abs(er - sr), abs(ec - sc))
        val sb = StringBuilder()
        for (k in 0..len) sb.append(grid[sr + dr * k][sc + dc * k])
        onSelect(sb.toString(), sr, sc, er, ec)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        cell = width.toFloat() / size
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                sr = (e.y / cell).toInt().coerceIn(0, size - 1)
                sc = (e.x / cell).toInt().coerceIn(0, size - 1)
                er = sr
                ec = sc
                dragging = true
                hintCell = -1
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    snap(e.x, e.y)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    snap(e.x, e.y)
                    dragging = false
                    finish()
                    er = sr
                    ec = sc
                    invalidate()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                invalidate()
            }
        }
        return true
    }

    private fun capsule(cv: Canvas, r1: Int, c1: Int, r2: Int, c2: Int, color: Int, alpha: Int) {
        val x1 = c1 * cell + cell / 2
        val y1 = r1 * cell + cell / 2
        val x2 = c2 * cell + cell / 2
        val y2 = r2 * cell + cell / 2
        val len = hypot(x2 - x1, y2 - y1)
        val ang = Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
        val rad = cell * 0.43f
        cv.save()
        cv.translate(x1, y1)
        cv.rotate(ang)
        p.style = Paint.Style.FILL
        p.color = (color and 0x00FFFFFF) or (alpha shl 24)
        cv.drawRoundRect(RectF(-rad, -rad, len + rad, rad), rad, rad, p)
        cv.restore()
    }

    override fun onDraw(cv: Canvas) {
        cell = width.toFloat() / size
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        cv.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 18 * d, 18 * d, p)

        for (f in found) capsule(cv, f.sr, f.sc, f.er, f.ec, f.color, 0xCC)
        if (dragging) capsule(cv, sr, sc, er, ec, 0xFFFFB300.toInt(), 0xAA)

        if (hintCell >= 0) {
            val hr = hintCell / size
            val hc = hintCell % size
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3 * d
            p.color = 0xFFFF6F00.toInt()
            cv.drawCircle(hc * cell + cell / 2, hr * cell + cell / 2, cell * 0.46f, p)
        }

        val inFound = BooleanArray(size * size)
        for (f in found) {
            val dr = Integer.signum(f.er - f.sr)
            val dc = Integer.signum(f.ec - f.sc)
            val len = max(abs(f.er - f.sr), abs(f.ec - f.sc))
            for (k in 0..len) inFound[(f.sr + dr * k) * size + f.sc + dc * k] = true
        }

        p.style = Paint.Style.FILL
        p.typeface = Typeface.DEFAULT_BOLD
        p.textAlign = Paint.Align.CENTER
        p.textSize = cell * 0.55f
        for (r in 0 until size) for (c in 0 until size) {
            p.color = if (inFound[r * size + c]) Color.WHITE else 0xFF263238.toInt()
            val cx = c * cell + cell / 2
            val cy = r * cell + cell / 2
            cv.drawText(grid[r][c].toString(), cx, cy - (p.descent() + p.ascent()) / 2, p)
        }
    }
}

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

    private var level = Progress.level(c, "cacapalavras")
    private var size = 8
    private var grid = Array(0) { CharArray(0) }
    private var placed = listOf<String>()
    private val starts = HashMap<String, Int>()
    private val foundLines = mutableListOf<FoundLine>()

    private lateinit var title: TextView
    private lateinit var msg: TextView
    private lateinit var board: WordGridView
    private lateinit var chipsBox: LinearLayout
    private lateinit var nextHolder: LinearLayout

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 12), Ui.dp(c, 36), Ui.dp(c, 12), Ui.dp(c, 24))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF00897B.toInt(), 0xFF004D40.toInt())
        )
        build()
        start()
    }

    private fun build() {
        val c = context
        val bar = Ui.row(c)
        bar.addView(Ui.smallBtn(c, "← Menu") { onBack() })
        title = Ui.text(c, "Caça-Palavras", 18f, true).apply {
            setTextColor(Color.WHITE)
            setPadding(Ui.dp(c, 12), 0, 0, 0)
        }
        bar.addView(title)
        addView(bar)

        msg = Ui.text(c, "", 18f, true).apply {
            setTextColor(0xFFFFEB3B.toInt())
            setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8))
        }
        addView(msg)

        board = WordGridView(c)
        board.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        board.onSelect = { s, r1, c1, r2, c2 -> onPick(s, r1, c1, r2, c2) }
        addView(board)

        chipsBox = LinearLayout(c)
        chipsBox.orientation = VERTICAL
        chipsBox.gravity = Gravity.CENTER_HORIZONTAL
        chipsBox.setPadding(0, Ui.dp(c, 12), 0, Ui.dp(c, 4))
        addView(chipsBox)

        val bottom = Ui.row(c)
        bottom.addView(Ui.smallBtn(c, "💡 Dica") { hint() })
        bottom.addView(Ui.smallBtn(c, "🔄 Novo jogo") { start() })
        addView(bottom)

        nextHolder = LinearLayout(c)
        nextHolder.orientation = VERTICAL
        addView(nextHolder)
    }

    private fun start() {
        size = minOf(7 + level, 12)
        grid = Array(size) { CharArray(size) { '\u0000' } }
        starts.clear()
        val dirs = listOf(
            0 to 1, 1 to 0, 1 to 1, 0 to -1, -1 to 0, -1 to -1, 1 to -1, -1 to 1
        ).take(if (level < 3) 3 else if (level < 5) 5 else 8)
        val chosen = bank.filter { it.length <= size }.shuffled().take(minOf(4 + level, 14))
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
                starts[w] = r * size + col
                ok.add(w)
                done = true
            }
        }
        placed = ok
        val filler = ok.joinToString("").toList().ifEmpty { ('A'..'Z').toList() }

        for (r in 0 until size) for (col in 0 until size) {
            if (grid[r][col] == '\u0000') grid[r][col] = if (level >= 8) filler.random() else ('A'..'Z').random()
        }
        foundLines.clear()
        title.text = "Caça-Palavras · Nível $level"
        msg.text = ""
        nextHolder.removeAllViews()
        board.size = size
        board.grid = grid
        board.found = emptyList()
        board.hintCell = -1
        board.requestLayout()
        board.invalidate()
        refreshChips()
    }

    private fun onPick(s: String, r1: Int, c1: Int, r2: Int, c2: Int) {
        val w = placed.firstOrNull { it == s || it == s.reversed() }
        if (w == null || foundLines.any { it.word == w }) return
        foundLines.add(FoundLine(w, r1, c1, r2, c2, WS_COLORS[foundLines.size % WS_COLORS.size]))
        board.found = foundLines.toList()
        board.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        board.invalidate()
        refreshChips()
        if (foundLines.size == placed.size) {
            msg.text = "🎉 Achou todas!"
            nextHolder.addView(Ui.btn(context, "Próximo nível →") {
                level++
                Progress.setLevel(context, "cacapalavras", level)
                start()
            })
            board.animate().scaleX(1.03f).scaleY(1.03f).setDuration(160).withEndAction {
                board.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }.start()
        }
    }

    private fun hint() {
        val w = placed.firstOrNull { x -> foundLines.none { it.word == x } } ?: return
        board.hintCell = starts[w] ?: -1
        board.invalidate()
        postDelayed({
            board.hintCell = -1
            board.invalidate()
        }, 2500)
    }

    private fun refreshChips() {
        val c = context
        chipsBox.removeAllViews()
        for (chunk in placed.chunked(3)) {
            val row = Ui.row(c)
            for (w in chunk) {
                val line = foundLines.firstOrNull { it.word == w }
                val chip = TextView(c)
                chip.text = w
                chip.textSize = 14f
                chip.gravity = Gravity.CENTER
                chip.setTypeface(null, Typeface.BOLD)
                chip.setTextColor(Color.WHITE)
                chip.setPadding(Ui.dp(c, 12), Ui.dp(c, 6), Ui.dp(c, 12), Ui.dp(c, 6))
                if (line != null) {
                    chip.background = Ui.shape(line.color, Ui.dp(c, 16).toFloat())
                    chip.paintFlags = chip.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    chip.background = Ui.shape(0x33FFFFFF, Ui.dp(c, 16).toFloat())
                }
                chip.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4))
                }
                row.addView(chip)
            }
            chipsBox.addView(row)
        }
    }
}
