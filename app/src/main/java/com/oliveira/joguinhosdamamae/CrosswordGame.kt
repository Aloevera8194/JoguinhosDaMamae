package com.oliveira.joguinhosdamamae

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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class CwWord(val r: Int, val col: Int, val across: Boolean, val a: String, val clue: String) {
    fun cells(): List<Int> = a.indices.map { i ->
        (r + if (across) 0 else i) * 10 + (col + if (across) i else 0)
    }
}

class CrossGridView(ctx: Context) : View(ctx) {
    var rows = 1
    var cols = 1
    var answers: Map<Int, Char> = emptyMap()
    var entries: Map<Int, Char> = emptyMap()
    var nums: Map<Int, Int> = emptyMap()
    var status: Map<Int, Int> = emptyMap()
    var hi: Set<Int> = emptySet()
    var cur = -1
    var onCell: (Int) -> Unit = {}

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = ctx.resources.displayMetrics.density
    private var cell = 0f
    private var left = 0f

    private fun metrics(w: Int) {
        cell = minOf(w.toFloat() / cols, 64 * d)
        left = (w - cell * cols) / 2f
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        metrics(w)
        setMeasuredDimension(w, (cell * rows).toInt() + (6 * d).toInt())
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) return true
        if (e.action == MotionEvent.ACTION_UP) {
            metrics(width)
            val col = ((e.x - left) / cell).toInt()
            val row = (e.y / cell).toInt()
            if (e.x >= left && col in 0 until cols && row in 0 until rows) {
                val key = row * 10 + col
                if (answers.containsKey(key)) onCell(key)
            }
        }
        return true
    }

    override fun onDraw(cv: Canvas) {
        metrics(width)
        val g = 2 * d
        val rad = 9 * d
        for (r in 0 until rows) for (c in 0 until cols) {
            val key = r * 10 + c
            val x = left + c * cell
            val y = r * cell
            val rect = RectF(x + g, y + g, x + cell - g, y + cell - g)
            p.style = Paint.Style.FILL
            if (!answers.containsKey(key)) {
                p.color = 0x22000000
                cv.drawRoundRect(rect, rad, rad, p)
                continue
            }
            val st = status[key] ?: 0
            p.color = when {
                key == cur -> 0xFFFFD54F.toInt()
                st == 1 -> 0xFFC8E6C9.toInt()
                st == 2 -> 0xFFFFCDD2.toInt()
                hi.contains(key) -> 0xFFFFF3C4.toInt()
                else -> Color.WHITE
            }
            cv.drawRoundRect(rect, rad, rad, p)
            if (key == cur) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3 * d
                p.color = 0xFFE65100.toInt()
                cv.drawRoundRect(rect, rad, rad, p)
                p.style = Paint.Style.FILL
            }
            val n = nums[key]
            if (n != null) {
                p.typeface = Typeface.DEFAULT_BOLD
                p.textAlign = Paint.Align.LEFT
                p.textSize = cell * 0.22f
                p.color = 0xFF8D6E63.toInt()
                cv.drawText(n.toString(), x + g + 4 * d, y + g + cell * 0.22f + 1 * d, p)
            }
            val ch = entries[key]
            if (ch != null) {
                p.typeface = Typeface.DEFAULT_BOLD
                p.textAlign = Paint.Align.CENTER
                p.textSize = cell * 0.55f
                p.color = when (st) {
                    2 -> 0xFFC62828.toInt()
                    3 -> 0xFF1565C0.toInt()
                    else -> 0xFF3E2723.toInt()
                }
                cv.drawText(ch.toString(), x + cell / 2, y + cell / 2 - (p.descent() + p.ascent()) / 2 + cell * 0.06f, p)
            }
        }
    }
}

class CrosswordGame(
    c: Context,
    private val onBack: () -> Unit
) : LinearLayout(c) {

    private val puzzles = listOf(
        listOf(
            CwWord(0, 0, true, "FLOR", "Enfeita o jardim"),
            CwWord(0, 0, false, "FOGO", "Queima e dá calor"),
            CwWord(0, 2, false, "OVAL", "Formato de ovo"),
            CwWord(2, 0, true, "GRAU", "Unidade de temperatura")
        ),
        listOf(
            CwWord(0, 0, true, "PRAIA", "Areia e mar"),
            CwWord(0, 0, false, "PATO", "Ave que nada e faz quá"),
            CwWord(0, 2, false, "AMOR", "Sentimento de carinho"),
            CwWord(0, 4, false, "ALHO", "Tempero branco em dentes"),
            CwWord(3, 2, true, "RIO", "Água que corre para o mar")
        ),
        listOf(
            CwWord(0, 0, true, "BOLO", "Doce de aniversário"),
            CwWord(0, 0, false, "BEIJO", "Carinho dado com os lábios"),
            CwWord(0, 2, false, "LUA", "Brilha à noite"),
            CwWord(2, 0, true, "IRA", "Raiva"),
            CwWord(4, 0, true, "OLHO", "Usamos para ver")
        )
    )

    private var idx = 0
    private var words: List<CwWord> = puzzles[0]
    private val answers = mutableMapOf<Int, Char>()
    private val entries = mutableMapOf<Int, Char>()
    private val status = mutableMapOf<Int, Int>()
    private val nums = mutableMapOf<Int, Int>()
    private var curKey = 0
    private var acrossDir = true
    private var done = false

    private lateinit var titleView: TextView
    private lateinit var msg: TextView
    private lateinit var grid: CrossGridView
    private lateinit var clueBar: TextView
    private lateinit var nextHolder: LinearLayout
    private lateinit var cluesBox: LinearLayout

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 12), Ui.dp(c, 36), Ui.dp(c, 12), Ui.dp(c, 24))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFFFFA726.toInt(), 0xFFE65100.toInt())
        )
        buildUi()
        loadPuzzle()
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

    private fun key(label: String, weight: Float, onClick: () -> Unit): TextView {
        val c = context
        return TextView(c).apply {
            text = label
            textSize = 18f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF3E2723.toInt())
            background = Ui.shape(Color.WHITE, Ui.dp(c, 8).toFloat())
            setOnClickListener { v ->
                v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(60).withEndAction {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                }.start()
                onClick()
            }
            layoutParams = LinearLayout.LayoutParams(0, Ui.dp(c, 46), weight).apply {
                setMargins(Ui.dp(c, 2), Ui.dp(c, 3), Ui.dp(c, 2), Ui.dp(c, 3))
            }
        }
    }

    private fun keyboard(): LinearLayout {
        val c = context
        val box = LinearLayout(c)
        box.orientation = VERTICAL
        val lines = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
        for ((ri, line) in lines.withIndex()) {
            val row = LinearLayout(c)
            row.orientation = HORIZONTAL
            row.gravity = Gravity.CENTER
            if (ri == 1) row.setPadding(Ui.dp(c, 14), 0, Ui.dp(c, 14), 0)
            for (ch in line) row.addView(key(ch.toString(), 1f) { type(ch) })
            if (ri == 2) row.addView(key("⌫", 1.5f) { back() })
            box.addView(row)
        }
        return box
    }

    private fun buildUi() {
        val c = context
        val bar = Ui.row(c)
        bar.addView(Ui.smallBtn(c, "← Menu") { onBack() })
        titleView = Ui.text(c, "Cruzada", 20f, true).apply {
            setTextColor(Color.WHITE)
            setPadding(Ui.dp(c, 12), 0, 0, 0)
        }
        bar.addView(titleView)
        addView(bar)

        msg = Ui.text(c, "", 18f, true).apply {
            setTextColor(0xFFFFF59D.toInt())
            setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8))
        }
        addView(msg)

        grid = CrossGridView(c)
        grid.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        grid.answers = answers
        grid.entries = entries
        grid.nums = nums
        grid.status = status
        grid.onCell = { k -> onCell(k) }
        addView(grid)

        clueBar = Ui.text(c, "", 16f, true).apply {
            setTextColor(Color.WHITE)
            background = Ui.shape(0x33FFFFFF, Ui.dp(c, 14).toFloat())
            setPadding(Ui.dp(c, 12), Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, Ui.dp(c, 8), 0, Ui.dp(c, 8)) }
            setOnClickListener { toggleDir() }
        }
        addView(clueBar)

        addView(keyboard())

        val actions = Ui.row(c)
        actions.addView(pill("✔ Conferir", 0xFF43A047.toInt()) { check() })
        actions.addView(pill("💡 Letra", 0xFFF9A825.toInt()) { hint() })
        actions.addView(pill("🧹 Limpar", 0xFF5C6BC0.toInt()) { clear() })
        addView(actions)

        nextHolder = LinearLayout(c)
        nextHolder.orientation = VERTICAL
        addView(nextHolder)

        cluesBox = LinearLayout(c)
        cluesBox.orientation = VERTICAL
        cluesBox.background = Ui.shape(0x22000000, Ui.dp(c, 16).toFloat())
        cluesBox.setPadding(Ui.dp(c, 14), Ui.dp(c, 8), Ui.dp(c, 14), Ui.dp(c, 8))
        cluesBox.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, Ui.dp(c, 12), 0, 0) }
        addView(cluesBox)
    }

    private fun loadPuzzle() {
        words = puzzles[idx]
        answers.clear()
        entries.clear()
        status.clear()
        nums.clear()
        for (w in words) for ((i, k) in w.cells().withIndex()) answers[k] = w.a[i]
        words.map { it.r * 10 + it.col }.distinct().sorted().forEachIndexed { n, k -> nums[k] = n + 1 }
        grid.rows = answers.keys.maxOf { it / 10 } + 1
        grid.cols = answers.keys.maxOf { it % 10 } + 1
        grid.requestLayout()
        done = false
        curKey = words[0].cells()[0]
        acrossDir = words[0].across
        titleView.text = "Cruzada ${idx + 1}/${puzzles.size}"
        msg.text = ""
        nextHolder.removeAllViews()
        buildClues()
        refresh()
    }

    private fun buildClues() {
        val c = context
        cluesBox.removeAllViews()
        val across = StringBuilder("→ Horizontais\n")
        val down = StringBuilder("↓ Verticais\n")
        for (w in words) {
            val line = "${nums[w.r * 10 + w.col]}. ${w.clue}\n"
            if (w.across) across.append(line) else down.append(line)
        }
        for (s in listOf(across.toString().trim(), down.toString().trim())) {
            cluesBox.addView(Ui.text(c, s, 15f).apply {
                gravity = Gravity.START
                setTextColor(Color.WHITE)
                setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 6))
            })
        }
    }

    private fun wordAt(key: Int, across: Boolean): CwWord? =
        words.firstOrNull { it.across == across && it.cells().contains(key) }

    private fun refresh() {
        val w = wordAt(curKey, acrossDir)
        grid.cur = curKey
        grid.hi = w?.cells()?.toSet() ?: emptySet()
        grid.invalidate()
        clueBar.text = if (w == null) "" else
            "${nums[w.r * 10 + w.col]} ${if (w.across) "→" else "↓"}  ${w.clue}  (${w.a.length} letras)"
    }

    private fun onCell(key: Int) {
        if (key == curKey) {
            if (wordAt(key, !acrossDir) != null) acrossDir = !acrossDir
        } else {
            curKey = key
            if (wordAt(key, acrossDir) == null) acrossDir = !acrossDir
        }
        refresh()
    }

    private fun toggleDir() {
        if (wordAt(curKey, !acrossDir) != null) {
            acrossDir = !acrossDir
            refresh()
        }
    }

    private fun advance(step: Int) {
        val w = wordAt(curKey, acrossDir) ?: return
        val cells = w.cells()
        val n = cells.indexOf(curKey) + step
        if (n in cells.indices) curKey = cells[n]
    }

    private fun type(ch: Char) {
        if (done) return
        entries[curKey] = ch
        status.remove(curKey)
        msg.text = ""
        advance(1)
        refresh()
        afterChange()
    }

    private fun back() {
        if (done) return
        if (!entries.containsKey(curKey)) advance(-1)
        entries.remove(curKey)
        status.remove(curKey)
        refresh()
    }

    private fun afterChange() {
        if (answers.all { (k, v) -> entries[k] == v }) {
            done = true
            msg.text = "🎉 Tudo certo!"
            nextHolder.removeAllViews()
            nextHolder.addView(Ui.btn(context, "Próxima cruzada →") {
                idx = (idx + 1) % puzzles.size
                loadPuzzle()
            })
            grid.animate().scaleX(1.03f).scaleY(1.03f).setDuration(160).withEndAction {
                grid.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }.start()
        }
    }

    private fun check() {
        if (done) return
        var wrong = 0
        var empty = 0
        for ((k, v) in answers) {
            val e = entries[k]
            if (e == null) {
                empty++
                continue
            }
            if (status[k] == 3) continue
            if (e == v) status[k] = 1 else {
                status[k] = 2
                wrong++
            }
        }
        grid.invalidate()
        afterChange()
        if (!done) {
            msg.text = if (wrong > 0) "Quase lá… confira os vermelhos"
            else "Tudo certo até aqui! Faltam $empty letras"
        }
    }

    private fun hint() {
        if (done) return
        var t = curKey
        if (entries[t] == answers[t]) {
            t = answers.keys.sorted().firstOrNull { entries[it] != answers[it] } ?: return
        }
        val ans = answers[t] ?: return
        entries[t] = ans
        status[t] = 3
        curKey = t
        if (wordAt(t, acrossDir) == null) acrossDir = !acrossDir
        refresh()
        afterChange()
    }

    private fun clear() {
        entries.clear()
        status.clear()
        done = false
        msg.text = ""
        nextHolder.removeAllViews()
        refresh()
    }
}
