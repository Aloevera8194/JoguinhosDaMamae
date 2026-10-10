package com.oliveira.joguinhosdamamae

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

fun sudokuPeer(a: Int, b: Int): Boolean =
    a / 9 == b / 9 || a % 9 == b % 9 || (a / 27 == b / 27 && (a % 9) / 3 == (b % 9) / 3)

object SudokuGen {
    fun fullGrid(): IntArray {
        fun groups(): List<Int> = (0..2).shuffled().flatMap { g -> (0..2).shuffled().map { g * 3 + it } }
        val rows = groups()
        val cols = groups()
        val digits = (1..9).shuffled()
        val g = IntArray(81)
        for (r in 0 until 9) for (c in 0 until 9) {
            val rr = rows[r]
            val cc = cols[c]
            g[r * 9 + c] = digits[(3 * (rr % 3) + rr / 3 + cc) % 9]
        }
        return g
    }

    private fun cands(a: IntArray, i: Int): Int {
        val r = i / 9
        val c = i % 9
        var used = 0
        for (k in 0 until 9) {
            used = used or (1 shl a[r * 9 + k]) or (1 shl a[k * 9 + c])
        }
        val br = r / 3 * 3
        val bc = c / 3 * 3
        for (dr in 0 until 3) for (dc in 0 until 3) {
            used = used or (1 shl a[(br + dr) * 9 + bc + dc])
        }
        return 0x3FE and used.inv()
    }

    private fun solve(a: IntArray, limit: Int): Int {
        var best = -1
        var bestMask = 0
        var bestN = 10
        for (i in 0 until 81) {
            if (a[i] != 0) continue
            val m = cands(a, i)
            val n = Integer.bitCount(m)
            if (n == 0) return 0
            if (n < bestN) {
                bestN = n
                best = i
                bestMask = m
            }
            if (n == 1) break
        }
        if (best < 0) return 1
        var total = 0
        for (v in 1..9) {
            if ((bestMask and (1 shl v)) == 0) continue
            a[best] = v
            total += solve(a, limit - total)
            if (total >= limit) break
        }
        a[best] = 0
        return total
    }

    fun puzzle(sol: IntArray, clues: Int): IntArray {
        val p = sol.copyOf()
        var left = 81
        for (i in (0 until 81).shuffled()) {
            if (left <= clues) break
            val keep = p[i]
            p[i] = 0
            if (solve(p.copyOf(), 2) != 1) p[i] = keep else left--
        }
        return p
    }
}

class SudokuBoardView(c: Context) : View(c) {
    var given = IntArray(81)
    var cur = IntArray(81)
    var notes = IntArray(81)
    var sol = IntArray(81)
    var sel = -1
    var onSelect: (Int) -> Unit = {}
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = c.resources.displayMetrics.density
    private val clip = Path()

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val s = MeasureSpec.getSize(wSpec)
        setMeasuredDimension(s, s)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
            val cell = width / 9f
            val col = (e.x / cell).toInt().coerceIn(0, 8)
            val row = (e.y / cell).toInt().coerceIn(0, 8)
            onSelect(row * 9 + col)
        }
        return true
    }

    override fun onDraw(cv: Canvas) {
        val s = width.toFloat()
        val cell = s / 9f
        clip.reset()
        clip.addRoundRect(RectF(0f, 0f, s, s), 14 * d, 14 * d, Path.Direction.CW)
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        cv.drawPath(clip, p)

        cv.save()
        cv.clipPath(clip)
        if (sel >= 0) {
            val sv = cur[sel]
            for (i in 0 until 81) {
                val col = when {
                    i == sel -> 0xFF90CAF9.toInt()
                    sv != 0 && cur[i] == sv -> 0xFFB39DDB.toInt()
                    sudokuPeer(i, sel) -> 0xFFE3F2FD.toInt()
                    else -> 0
                }
                if (col != 0) {
                    p.color = col
                    val x = (i % 9) * cell
                    val y = (i / 9) * cell
                    cv.drawRect(x, y, x + cell, y + cell, p)
                }
            }
        }
        cv.restore()

        p.style = Paint.Style.STROKE
        for (k in 0..9) {
            val thick = k % 3 == 0
            p.strokeWidth = if (thick) 2.5f * d else 1f * d
            p.color = if (thick) 0xFF37474F.toInt() else 0xFFCFD8DC.toInt()
            val pos = k * cell
            cv.drawLine(pos, 0f, pos, s, p)
            cv.drawLine(0f, pos, s, pos, p)
        }

        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        for (i in 0 until 81) {
            val cx = (i % 9) * cell + cell / 2
            val cy = (i / 9) * cell + cell / 2
            val v = cur[i]
            if (v != 0) {
                p.typeface = if (given[i] != 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                p.textSize = cell * 0.62f
                p.color = when {
                    given[i] != 0 -> 0xFF1A237E.toInt()
                    v != sol[i] -> 0xFFE53935.toInt()
                    else -> 0xFF1E88E5.toInt()
                }
                cv.drawText(v.toString(), cx, cy - (p.descent() + p.ascent()) / 2, p)
            } else if (notes[i] != 0) {
                p.typeface = Typeface.DEFAULT
                p.textSize = cell * 0.25f
                p.color = 0xFF78909C.toInt()
                for (n in 1..9) {
                    if ((notes[i] and (1 shl n)) == 0) continue
                    val nx = (i % 9) * cell + cell * (((n - 1) % 3) * 2 + 1) / 6f
                    val ny = (i / 9) * cell + cell * (((n - 1) / 3) * 2 + 1) / 6f
                    cv.drawText(n.toString(), nx, ny - (p.descent() + p.ascent()) / 2, p)
                }
            }
        }
    }
}

class SudokuGame(
    c: Context,
    private val onBack: () -> Unit
) : LinearLayout(c) {

    private var maxLevel = Progress.level(c, "sudoku")
    private var level = Progress.level(c, "sudoku")
    private var sol = IntArray(81)
    private var given = IntArray(81)
    private var cur = IntArray(81)
    private var notes = IntArray(81)
    private var sel = -1
    private var noteMode = false
    private val history = ArrayList<Pair<IntArray, IntArray>>()
    private lateinit var board: SudokuBoardView
    private lateinit var msg: TextView
    private lateinit var noteBtn: TextView
    private lateinit var prevBtn: TextView
    private lateinit var nextBtn: TextView
    private lateinit var levelView: TextView
    private lateinit var nextHolder: LinearLayout

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 12), Ui.dp(c, 28), Ui.dp(c, 12), Ui.dp(c, 24))
        setBackgroundColor(0xFFEEF3FF.toInt())
        build()
        newGame()
    }

    private fun pill(label: String, onClick: () -> Unit): TextView {
        val c = context
        return TextView(c).apply {
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, Ui.dp(c, 46), 1f).apply {
                setMargins(Ui.dp(c, 3), Ui.dp(c, 3), Ui.dp(c, 3), Ui.dp(c, 3))
            }
        }
    }

    private fun paint(v: TextView, bg: Int, fg: Int) {
        v.background = Ui.shape(bg, Ui.dp(context, 12).toFloat())
        v.setTextColor(fg)
    }

    private fun build() {
        val c = context
        msg = Ui.header(c, this, "🔢 Sudoku") { onBack() }

        val lv = Ui.row(c)
        prevBtn = pill("◀ Anterior") { goLevel(level - 1) }
        levelView = pill("Nível") { }
        nextBtn = pill("Próximo ▶") { goLevel(level + 1) }
        lv.addView(prevBtn)
        lv.addView(levelView)
        lv.addView(nextBtn)
        addView(lv)

        board = SudokuBoardView(c)
        board.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, Ui.dp(c, 8), 0, Ui.dp(c, 8)) }
        board.onSelect = { i ->
            sel = i
            push()
        }
        addView(board)

        nextHolder = LinearLayout(c)
        nextHolder.orientation = LinearLayout.VERTICAL
        addView(nextHolder)

        val act = Ui.row(c)
        noteBtn = pill("✏️ Notas") {
            noteMode = !noteMode
            refresh()
        }
        act.addView(noteBtn)
        act.addView(pill("⌫ Apagar") { erase() }.also { paint(it, 0xFFFFE0B2.toInt(), 0xFF5D4037.toInt()) })
        act.addView(pill("↩ Desfazer") { undo() }.also { paint(it, 0xFFD6DCF5.toInt(), 0xFF283593.toInt()) })
        act.addView(pill("💡 Dica") { hint() }.also { paint(it, 0xFFFFF59D.toInt(), 0xFF6D4C41.toInt()) })
        addView(act)

        val pad = Ui.row(c)
        for (n in 1..9) {
            val b = TextView(c)
            b.text = n.toString()
            b.textSize = 22f
            b.gravity = Gravity.CENTER
            b.setTypeface(null, Typeface.BOLD)
            b.setTextColor(Color.WHITE)
            b.background = Ui.shape(0xFF3F51B5.toInt(), Ui.dp(c, 10).toFloat())
            b.setOnClickListener { place(n) }
            b.layoutParams = LinearLayout.LayoutParams(0, Ui.dp(c, 54), 1f).apply {
                setMargins(Ui.dp(c, 2), Ui.dp(c, 6), Ui.dp(c, 2), 0)
            }
            pad.addView(b)
        }
        addView(pad)
        addView(Ui.btn(c, "🔄 Novo jogo") { newGame() })
    }

    private fun goLevel(n: Int) {
        if (n < 1 || n > maxLevel) return
        level = n
        newGame()
    }

    private fun clues() = maxOf(24, 46 - 2 * level)

    private fun refresh() {
        levelView.text = "Nível $level"
        paint(levelView, 0xFF3F51B5.toInt(), Color.WHITE)
        if (level > 1) paint(prevBtn, 0xFFD6DCF5.toInt(), 0xFF283593.toInt())
        else paint(prevBtn, 0xFFE8EAF0.toInt(), 0xFFAAAAAA.toInt())
        if (level < maxLevel) paint(nextBtn, 0xFFD6DCF5.toInt(), 0xFF283593.toInt())
        else paint(nextBtn, 0xFFE8EAF0.toInt(), 0xFFAAAAAA.toInt())
        if (noteMode) paint(noteBtn, 0xFF43A047.toInt(), Color.WHITE)
        else paint(noteBtn, 0xFFC8E6C9.toInt(), 0xFF1B5E20.toInt())
        noteBtn.text = if (noteMode) "✏️ Notas ON" else "✏️ Notas"
    }

    private fun push() {
        board.given = given
        board.cur = cur
        board.notes = notes
        board.sol = sol
        board.sel = sel
        board.invalidate()
    }

    private fun solved() = (0 until 81).all { cur[it] == sol[it] }

    private fun update() {
        push()
        if (solved()) {
            msg.text = "🎉 Parabéns!"
            if (nextHolder.childCount == 0) {
                nextHolder.addView(Ui.btn(context, "Próximo nível →") {
                    level++
                    if (level > maxLevel) {
                        maxLevel = level
                        Progress.setLevel(context, "sudoku", maxLevel)
                    }
                    newGame()
                })
            }
        } else {
            msg.text = ""
        }
    }

    private fun newGame() {
        sol = SudokuGen.fullGrid()
        given = SudokuGen.puzzle(sol, clues())
        cur = given.copyOf()
        notes = IntArray(81)
        history.clear()
        sel = -1
        msg.text = ""
        nextHolder.removeAllViews()
        push()
        refresh()
    }

    private fun clearPeerNotes(i: Int, n: Int) {
        for (j in 0 until 81) {
            if (sudokuPeer(i, j)) notes[j] = notes[j] and (1 shl n).inv()
        }
    }

    private fun place(n: Int) {
        if (sel < 0 || given[sel] != 0 || solved()) return
        history.add(Pair(cur.copyOf(), notes.copyOf()))
        if (noteMode) {
            if (cur[sel] == 0) notes[sel] = notes[sel] xor (1 shl n)
        } else {
            if (cur[sel] == n) {
                cur[sel] = 0
            } else {
                cur[sel] = n
                notes[sel] = 0
                clearPeerNotes(sel, n)
            }
        }
        update()
    }

    private fun erase() {
        if (sel < 0 || given[sel] != 0) return
        if (cur[sel] == 0 && notes[sel] == 0) return
        history.add(Pair(cur.copyOf(), notes.copyOf()))
        cur[sel] = 0
        notes[sel] = 0
        update()
    }

    private fun undo() {
        if (history.isEmpty()) return
        val (a, b) = history.removeAt(history.lastIndex)
        cur = a
        notes = b
        update()
    }

    private fun hint() {
        if (solved()) return
        var t = -1
        if (sel >= 0 && given[sel] == 0 && cur[sel] != sol[sel]) {
            t = sel
        } else {
            val bad = (0 until 81).filter { cur[it] != sol[it] }
            if (bad.isEmpty()) return
            t = bad.random()
        }
        history.add(Pair(cur.copyOf(), notes.copyOf()))
        cur[t] = sol[t]
        notes[t] = 0
        clearPeerNotes(t, sol[t])
        sel = t
        update()
    }
}
