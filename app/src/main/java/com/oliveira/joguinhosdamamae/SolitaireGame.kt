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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.hypot
import kotlin.math.max

const val SOL_NONE = 0
const val SOL_STOCK = 1
const val SOL_WASTE = 2
const val SOL_TAB = 3
const val SOL_FOUND = 4

class SolCard(val s: Int, val r: Int, val up: Boolean) {
    fun face(u: Boolean) = SolCard(s, r, u)
    fun red() = s == 1 || s == 2
}

class SolState(
    val stock: ArrayList<SolCard>,
    val waste: ArrayList<SolCard>,
    val found: IntArray,
    val tab: Array<ArrayList<SolCard>>
) {
    fun copy() = SolState(ArrayList(stock), ArrayList(waste), found.copyOf(), Array(7) { ArrayList(tab[it]) })
}

class SolHint(val srcT: Int, val srcC: Int, val srcI: Int, val dstT: Int, val dstC: Int)

class SolitaireView(ctx: Context) : View(ctx) {
    var onChange: () -> Unit = {}
    var onWin: () -> Unit = {}
    var onMsg: (String) -> Unit = {}
    var moves = 0

    private var st = SolState(ArrayList(), ArrayList(), IntArray(4), Array(7) { ArrayList<SolCard>() })
    private val undoStack = ArrayList<SolState>()
    private var selT = SOL_NONE
    private var selCol = 0
    private var selIdx = 0
    private var hint: SolHint? = null

    private val d = ctx.resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG)
    private val suits = arrayOf("♠", "♥", "♦", "♣")

    private var cw = 0f
    private var ch = 0f
    private var gap = 0f
    private var pad = 0f
    private var topY = 0f
    private var tabY = 0f
    private var upOff = 0f
    private var downOff = 0f
    private var downX = 0f
    private var downY = 0f

    private class Hit(val t: Int, val col: Int, val idx: Int)

    private fun metrics(w: Float) {
        pad = 4 * d
        gap = 3 * d
        cw = (w - 2 * pad - 6 * gap) / 7f
        ch = cw * 1.45f
        topY = pad
        tabY = pad + ch + 16 * d
        upOff = ch * 0.38f
        downOff = ch * 0.17f
    }

    private fun colX(c: Int) = pad + c * (cw + gap)

    private fun cardY(col: List<SolCard>, i: Int): Float {
        var y = tabY
        for (k in 0 until i) y += if (col[k].up) upOff else downOff
        return y
    }

    private fun rank(r: Int): String = when (r) {
        1 -> "A"
        11 -> "J"
        12 -> "Q"
        13 -> "K"
        else -> r.toString()
    }

    fun newGame() {
        val deck = ArrayList<SolCard>()
        for (s in 0 until 4) for (r in 1..13) deck.add(SolCard(s, r, false))
        deck.shuffle()
        val tab = Array(7) { ArrayList<SolCard>() }
        var k = 0
        for (c in 0 until 7) for (i in 0..c) {
            val card = deck[k]
            k++
            tab[c].add(if (i == c) card.face(true) else card)
        }
        val stock = ArrayList<SolCard>()
        while (k < deck.size) {
            stock.add(deck[k])
            k++
        }
        st = SolState(stock, ArrayList(), IntArray(4), tab)
        undoStack.clear()
        moves = 0
        clearSel()
        hint = null
        refresh()
    }

    private fun refresh() {
        requestLayout()
        invalidate()
        onChange()
    }

    private fun clearSel() {
        selT = SOL_NONE
    }

    private fun push() {
        undoStack.add(st.copy())
        if (undoStack.size > 300) undoStack.removeAt(0)
        moves++
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        st = undoStack.removeAt(undoStack.size - 1)
        if (moves > 0) moves--
        clearSel()
        hint = null
        refresh()
    }

    private fun canFound(c: SolCard) = st.found[c.s] == c.r - 1

    private fun canTab(c: SolCard, col: Int): Boolean {
        val pile = st.tab[col]
        if (pile.isEmpty()) return c.r == 13
        val top = pile[pile.size - 1]
        return top.up && top.r == c.r + 1 && top.red() != c.red()
    }

    private fun selectedCards(): List<SolCard>? {
        if (selT == SOL_WASTE) {
            val w = st.waste.lastOrNull() ?: return null
            return listOf(w)
        }
        if (selT == SOL_TAB) {
            val col = st.tab[selCol]
            if (selIdx >= col.size) return null
            return ArrayList(col.subList(selIdx, col.size))
        }
        return null
    }

    private fun removeSelected() {
        if (selT == SOL_WASTE) {
            st.waste.removeAt(st.waste.size - 1)
        } else if (selT == SOL_TAB) {
            val col = st.tab[selCol]
            while (col.size > selIdx) col.removeAt(col.size - 1)
        }
    }

    private fun afterMove() {
        for (col in st.tab) {
            if (col.isNotEmpty() && !col[col.size - 1].up) col[col.size - 1] = col[col.size - 1].face(true)
        }
        clearSel()
        hint = null
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        refresh()
        if (st.found.all { it == 13 }) onWin()
    }

    private fun moveSelToTab(dc: Int): Boolean {
        val cards = selectedCards() ?: return false
        if (!canTab(cards[0], dc)) return false
        push()
        removeSelected()
        st.tab[dc].addAll(cards)
        afterMove()
        return true
    }

    private fun moveSelToFound(only: Int): Boolean {
        val cards = selectedCards() ?: return false
        if (cards.size != 1) return false
        val c = cards[0]
        if (only >= 0 && c.s != only) return false
        if (!canFound(c)) return false
        push()
        removeSelected()
        st.found[c.s] = st.found[c.s] + 1
        afterMove()
        return true
    }

    private fun autoTab(): Boolean {
        val cards = selectedCards() ?: return false
        var target = -1
        for (dc in 0 until 7) {
            if (selT == SOL_TAB && dc == selCol) continue
            if (st.tab[dc].isNotEmpty() && canTab(cards[0], dc)) {
                target = dc
                break
            }
        }
        if (target < 0) {
            for (dc in 0 until 7) {
                if (selT == SOL_TAB && dc == selCol) continue
                if (st.tab[dc].isEmpty() && selT == SOL_TAB && selIdx == 0) continue
                if (canTab(cards[0], dc)) {
                    target = dc
                    break
                }
            }
        }
        if (target < 0) return false
        return moveSelToTab(target)
    }

    private fun drawFromStock() {
        if (st.stock.isEmpty() && st.waste.isEmpty()) return
        push()
        if (st.stock.isNotEmpty()) {
            val c = st.stock.removeAt(st.stock.size - 1)
            st.waste.add(c.face(true))
        } else {
            while (st.waste.isNotEmpty()) {
                val c = st.waste.removeAt(st.waste.size - 1)
                st.stock.add(c.face(false))
            }
        }
        afterMove()
    }

    private fun findHint(): SolHint? {
        val w = st.waste.lastOrNull()
        if (w != null && canFound(w)) return SolHint(SOL_WASTE, 0, 0, SOL_FOUND, w.s)
        for (c in 0 until 7) {
            val col = st.tab[c]
            val t = col.lastOrNull() ?: continue
            if (t.up && canFound(t)) return SolHint(SOL_TAB, c, col.size - 1, SOL_FOUND, t.s)
        }
        for (c in 0 until 7) {
            val col = st.tab[c]
            val firstUp = col.indexOfFirst { it.up }
            if (firstUp <= 0) continue
            for (dc in 0 until 7) {
                if (dc != c && canTab(col[firstUp], dc)) return SolHint(SOL_TAB, c, firstUp, SOL_TAB, dc)
            }
        }
        if (w != null) {
            for (dc in 0 until 7) if (canTab(w, dc)) return SolHint(SOL_WASTE, 0, 0, SOL_TAB, dc)
        }
        for (c in 0 until 7) {
            val col = st.tab[c]
            val firstUp = col.indexOfFirst { it.up }
            if (firstUp != 0) continue
            for (dc in 0 until 7) {
                if (dc != c && st.tab[dc].isNotEmpty() && canTab(col[0], dc)) {
                    return SolHint(SOL_TAB, c, 0, SOL_TAB, dc)
                }
            }
        }
        if (st.stock.isNotEmpty() || st.waste.isNotEmpty()) return SolHint(SOL_STOCK, 0, 0, SOL_NONE, 0)
        return null
    }

    fun showHint() {
        val h = findHint()
        if (h == null) {
            onMsg("Sem jogadas possíveis. Desfaça ou comece um novo jogo")
            return
        }
        clearSel()
        hint = h
        onMsg(
            when {
                h.srcT == SOL_STOCK && st.stock.isEmpty() -> "Dica: toque no monte para virar as cartas de novo"
                h.srcT == SOL_STOCK -> "Dica: toque no monte para comprar uma carta"
                h.dstT == SOL_FOUND -> "Dica: leve a carta destacada para as pilhas de cima"
                else -> "Dica: leve a carta destacada para a coluna destacada"
            }
        )
        invalidate()
        postDelayed({
            if (hint === h) {
                hint = null
                invalidate()
            }
        }, 4000)
    }

    private fun hitTest(x: Float, y: Float): Hit? {
        if (y >= topY && y <= topY + ch) {
            for (c in 0 until 7) {
                if (x >= colX(c) && x <= colX(c) + cw) {
                    return when {
                        c == 0 -> Hit(SOL_STOCK, 0, 0)
                        c == 1 -> Hit(SOL_WASTE, 0, 0)
                        c >= 3 -> Hit(SOL_FOUND, c - 3, 0)
                        else -> null
                    }
                }
            }
            return null
        }
        for (c in 0 until 7) {
            if (x < colX(c) || x > colX(c) + cw) continue
            val col = st.tab[c]
            if (col.isEmpty()) {
                return if (y >= tabY && y <= tabY + ch) Hit(SOL_TAB, c, -1) else null
            }
            for (i in col.size - 1 downTo 0) {
                val y0 = cardY(col, i)
                val h = if (i == col.size - 1) ch else (if (col[i].up) upOff else downOff)
                if (y >= y0 && y <= y0 + h) return Hit(SOL_TAB, c, i)
            }
            return null
        }
        return null
    }

    private fun tapAt(x: Float, y: Float) {
        hint = null
        val h = hitTest(x, y)
        if (h == null) {
            clearSel()
            invalidate()
            return
        }
        when (h.t) {
            SOL_STOCK -> {
                clearSel()
                drawFromStock()
            }
            SOL_WASTE -> {
                if (st.waste.isEmpty()) {
                    clearSel()
                } else if (selT == SOL_WASTE) {
                    if (!moveSelToFound(-1) && !autoTab()) clearSel()
                } else {
                    selT = SOL_WASTE
                }
            }
            SOL_FOUND -> {
                if (selT != SOL_NONE) moveSelToFound(h.col)
            }
            SOL_TAB -> {
                val col = h.col
                if (selT != SOL_NONE && !(selT == SOL_TAB && selCol == col)) {
                    if (moveSelToTab(col)) return
                }
                val pile = st.tab[col]
                if (h.idx >= 0 && pile[h.idx].up) {
                    if (selT == SOL_TAB && selCol == col && selIdx == h.idx) {
                        if (!moveSelToFound(-1) && !autoTab()) clearSel()
                    } else {
                        selT = SOL_TAB
                        selCol = col
                        selIdx = h.idx
                    }
                } else {
                    clearSel()
                }
            }
        }
        invalidate()
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        metrics(w.toFloat())
        var bottom = tabY + ch * 4f
        for (c in 0 until 7) {
            val col = st.tab[c]
            val b = if (col.isEmpty()) tabY + ch else cardY(col, col.size - 1) + ch
            bottom = max(bottom, b)
        }
        setMeasuredDimension(w, (bottom + pad).toInt())
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        metrics(width.toFloat())
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (hypot(e.x - downX, e.y - downY) < 14 * d) tapAt(e.x, e.y)
                return true
            }
        }
        return true
    }

    private fun drawSlot(cv: Canvas, x: Float, y: Float, label: String) {
        val rect = RectF(x, y, x + cw, y + ch)
        fill.style = Paint.Style.FILL
        fill.color = 0x22FFFFFF
        cv.drawRoundRect(rect, 7 * d, 7 * d, fill)
        fill.style = Paint.Style.STROKE
        fill.strokeWidth = 2 * d
        fill.color = 0x66FFFFFF
        cv.drawRoundRect(rect, 7 * d, 7 * d, fill)
        if (label.isNotEmpty()) {
            txt.color = 0x88FFFFFF.toInt()
            txt.typeface = Typeface.DEFAULT_BOLD
            txt.textAlign = Paint.Align.CENTER
            txt.textSize = cw * 0.7f
            cv.drawText(label, x + cw / 2, y + ch * 0.64f, txt)
        }
    }

    private fun drawBack(cv: Canvas, x: Float, y: Float) {
        val rect = RectF(x, y, x + cw, y + ch)
        fill.style = Paint.Style.FILL
        fill.color = 0xFF1565C0.toInt()
        cv.drawRoundRect(rect, 7 * d, 7 * d, fill)
        fill.style = Paint.Style.STROKE
        fill.strokeWidth = 2 * d
        fill.color = Color.WHITE
        cv.drawRoundRect(rect, 7 * d, 7 * d, fill)
        fill.strokeWidth = 1.5f * d
        fill.color = 0xFF90CAF9.toInt()
        cv.drawRoundRect(RectF(x + 5 * d, y + 5 * d, x + cw - 5 * d, y + ch - 5 * d), 4 * d, 4 * d, fill)
    }

    private fun drawCard(cv: Canvas, x: Float, y: Float, c: SolCard) {
        val rect = RectF(x, y, x + cw, y + ch)
        fill.style = Paint.Style.FILL
        fill.color = Color.WHITE
        cv.drawRoundRect(rect, 7 * d, 7 * d, fill)
        fill.style = Paint.Style.STROKE
        fill.strokeWidth = 2 * d
        fill.color = 0xFF455A64.toInt()
        cv.drawRoundRect(rect, 7 * d, 7 * d, fill)
        txt.color = if (c.red()) 0xFFC62828.toInt() else 0xFF212121.toInt()
        txt.typeface = Typeface.DEFAULT_BOLD
        txt.textAlign = Paint.Align.LEFT
        val label = rank(c.r) + suits[c.s]
        var ts = cw * 0.46f
        txt.textSize = ts
        val wd = txt.measureText(label)
        val maxW = cw - 8 * d
        if (wd > maxW) {
            ts *= maxW / wd
            txt.textSize = ts
        }
        cv.drawText(label, x + 4 * d, y + ts + 3 * d, txt)
        txt.textAlign = Paint.Align.CENTER
        txt.textSize = cw * 0.8f
        cv.drawText(suits[c.s], x + cw / 2, y + ch * 0.84f, txt)
    }

    private fun ring(cv: Canvas, x: Float, y: Float, h: Float, color: Int) {
        fill.style = Paint.Style.STROKE
        fill.strokeWidth = 4 * d
        fill.color = color
        cv.drawRoundRect(RectF(x - 1 * d, y - 1 * d, x + cw + 1 * d, y + h + 1 * d), 8 * d, 8 * d, fill)
    }

    override fun onDraw(cv: Canvas) {
        metrics(width.toFloat())

        if (st.stock.isNotEmpty()) {
            drawBack(cv, colX(0), topY)
        } else {
            drawSlot(cv, colX(0), topY, if (st.waste.isNotEmpty()) "↻" else "")
        }
        val w = st.waste.lastOrNull()
        if (w != null) drawCard(cv, colX(1), topY, w) else drawSlot(cv, colX(1), topY, "")
        for (f in 0 until 4) {
            val n = st.found[f]
            val x = colX(3 + f)
            if (n > 0) drawCard(cv, x, topY, SolCard(f, n, true)) else drawSlot(cv, x, topY, suits[f])
        }
        for (c in 0 until 7) {
            val col = st.tab[c]
            val x = colX(c)
            if (col.isEmpty()) {
                drawSlot(cv, x, tabY, "")
            } else {
                for (i in col.indices) {
                    val y = cardY(col, i)
                    if (col[i].up) drawCard(cv, x, y, col[i]) else drawBack(cv, x, y)
                }
            }
        }

        val orange = 0xFFFF6F00.toInt()
        if (selT == SOL_WASTE && st.waste.isNotEmpty()) {
            ring(cv, colX(1), topY, ch, orange)
        } else if (selT == SOL_TAB && selCol in 0 until 7 && selIdx < st.tab[selCol].size) {
            val col = st.tab[selCol]
            val y0 = cardY(col, selIdx)
            ring(cv, colX(selCol), y0, cardY(col, col.size - 1) + ch - y0, orange)
        }

        val h = hint
        if (h != null) {
            val yellow = 0xFFFFEB3B.toInt()
            when (h.srcT) {
                SOL_STOCK -> ring(cv, colX(0), topY, ch, yellow)
                SOL_WASTE -> ring(cv, colX(1), topY, ch, yellow)
                SOL_TAB -> {
                    val col = st.tab[h.srcC]
                    if (h.srcI < col.size) {
                        val y0 = cardY(col, h.srcI)
                        ring(cv, colX(h.srcC), y0, cardY(col, col.size - 1) + ch - y0, yellow)
                    }
                }
            }
            when (h.dstT) {
                SOL_FOUND -> ring(cv, colX(3 + h.dstC), topY, ch, yellow)
                SOL_TAB -> {
                    val col = st.tab[h.dstC]
                    val y0 = if (col.isEmpty()) tabY else cardY(col, col.size - 1)
                    ring(cv, colX(h.dstC), y0, ch, yellow)
                }
            }
        }
    }
}

class SolitaireGame(
    c: Context,
    private val onBack: () -> Unit
) : LinearLayout(c) {

    private lateinit var board: SolitaireView
    private lateinit var info: TextView
    private lateinit var msg: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 8), Ui.dp(c, 36), Ui.dp(c, 8), Ui.dp(c, 24))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF2E7D32.toInt(), 0xFF0D3B13.toInt())
        )
        build()
        board.newGame()
    }

    private fun pill(label: String, bg: Int, onClick: () -> Unit): Button {
        val c = context
        return Button(c).apply {
            text = label
            textSize = 15f
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
        bar.addView(Ui.text(c, "Paciência", 24f, true).apply {
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

        msg = Ui.text(c, "", 18f, true).apply {
            setTextColor(0xFFFFF59D.toInt())
            setPadding(Ui.dp(c, 8), Ui.dp(c, 6), Ui.dp(c, 8), Ui.dp(c, 6))
        }
        addView(msg)

        board = SolitaireView(c)
        board.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        board.onChange = {
            info.text = "Jogadas ${board.moves}"
            msg.text = ""
        }
        board.onWin = { msg.text = "🎉 Parabéns! Você ganhou!" }
        board.onMsg = { t -> msg.text = t }
        addView(board)

        val actions = Ui.row(c)
        actions.addView(pill("💡 Dica", 0xFFF9A825.toInt()) { board.showHint() })
        actions.addView(pill("↩ Desfazer", 0xFF5C6BC0.toInt()) { board.undo() })
        actions.addView(pill("🔄 Novo", 0xFF00897B.toInt()) { board.newGame() })
        addView(actions)
    }
}
