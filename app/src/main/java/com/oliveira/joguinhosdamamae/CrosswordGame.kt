package com.oliveira.joguinhosdamamae

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class CrosswordGame(
    c: Context,
    private val onBack: () -> Unit
) : LinearLayout(c) {

    private class W(val r: Int, val col: Int, val across: Boolean, val a: String, val clue: String)

    private val puzzles = listOf(
        listOf(
            W(0, 0, true, "FLOR", "Enfeita o jardim"),
            W(0, 0, false, "FOGO", "Queima e dá calor"),
            W(0, 2, false, "OVAL", "Formato de ovo"),
            W(2, 0, true, "GRAU", "Unidade de temperatura")
        ),
        listOf(
            W(0, 0, true, "PRAIA", "Areia e mar"),
            W(0, 0, false, "PATO", "Ave que nada e faz quá"),
            W(0, 2, false, "AMOR", "Sentimento de carinho"),
            W(0, 4, false, "ALHO", "Tempero branco em dentes"),
            W(3, 2, true, "RIO", "Água que corre para o mar")
        ),
        listOf(
            W(0, 0, true, "BOLO", "Doce de aniversário"),
            W(0, 0, false, "BEIJO", "Carinho dado com os lábios"),
            W(0, 2, false, "LUA", "Brilha à noite"),
            W(2, 0, true, "IRA", "Raiva"),
            W(4, 0, true, "OLHO", "Usamos para ver")
        )
    )

    private var idx = 0
    private val edits = mutableMapOf<Int, EditText>()
    private val answers = mutableMapOf<Int, Char>()

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Ui.dp(c, 14), Ui.dp(c, 28), Ui.dp(c, 14), Ui.dp(c, 24))
        setBackgroundColor(Ui.BG)
        build()
    }

    private fun build() {
        val ctx = context
        removeAllViews()
        edits.clear()
        answers.clear()
        val p = puzzles[idx]

        for (w in p) for (i in w.a.indices) {
            val r = w.r + if (w.across) 0 else i
            val col = w.col + if (w.across) i else 0
            answers[r * 10 + col] = w.a[i]
        }
        val nums = mutableMapOf<Int, Int>()
        p.map { it.r * 10 + it.col }.distinct().sorted().forEachIndexed { n, k -> nums[k] = n + 1 }

        val msg = Ui.header(ctx, this, "Cruzada ${idx + 1}/${puzzles.size}") { onBack() }

        val rows = answers.keys.maxOf { it / 10 }
        val cols = answers.keys.maxOf { it % 10 }
        val size = Ui.dp(ctx, 58)
        val order = answers.keys.sorted()

        for (r in 0..rows) {
            val row = Ui.row(ctx)
            for (col in 0..cols) {
                val key = r * 10 + col
                val lp = LayoutParams(size, size).apply {
                    setMargins(Ui.dp(ctx, 2), Ui.dp(ctx, 2), Ui.dp(ctx, 2), Ui.dp(ctx, 2))
                }
                if (!answers.containsKey(key)) {
                    row.addView(View(ctx).apply { layoutParams = lp })
                    continue
                }
                val cell = FrameLayout(ctx)
                cell.layoutParams = lp
                val et = EditText(ctx).apply {
                    filters = arrayOf(InputFilter.LengthFilter(1), InputFilter.AllCaps())
                    gravity = Gravity.CENTER
                    textSize = 22f
                    inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    setTextColor(Ui.TEXT)
                    setPadding(0, 0, 0, 0)
                    background = Ui.shape(Ui.CARD, Ui.dp(ctx, 8).toFloat(), Ui.dp(ctx, 2), Ui.GREEN)
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            background = Ui.shape(Ui.CARD, Ui.dp(ctx, 8).toFloat(), Ui.dp(ctx, 2), Ui.GREEN)
                            if (s != null && s.length == 1) {
                                val next = order.getOrNull(order.indexOf(key) + 1)
                                if (next != null) edits[next]?.requestFocus()
                            }
                        }
                    })
                }
                edits[key] = et
                cell.addView(et, FrameLayout.LayoutParams(size, size))
                nums[key]?.let { n ->
                    cell.addView(TextView(ctx).apply {
                        text = n.toString()
                        textSize = 10f
                        setTextColor(Ui.TEXT)
                        setPadding(Ui.dp(ctx, 4), Ui.dp(ctx, 2), 0, 0)
                    }, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
                    ))
                }
                row.addView(cell)
            }
            addView(row)
        }

        val bar = Ui.row(ctx)
        bar.addView(Ui.smallBtn(ctx, "✔ Conferir") { check(msg) })
        addView(bar)

        val across = StringBuilder("→ Horizontais\n")
        val down = StringBuilder("↓ Verticais\n")
        for (w in p) {
            val line = "${nums[w.r * 10 + w.col]}. ${w.clue}\n"
            if (w.across) across.append(line) else down.append(line)
        }
        addView(Ui.text(ctx, across.toString().trim(), 16f).apply {
            gravity = Gravity.START
            setPadding(0, Ui.dp(ctx, 16), 0, Ui.dp(ctx, 8))
        })
        addView(Ui.text(ctx, down.toString().trim(), 16f).apply {
            gravity = Gravity.START
        })
    }

    private fun check(msg: TextView) {
        val ctx = context
        var all = true
        for ((k, et) in edits) {
            val t = et.text.toString().uppercase()
            val ok = t.isNotEmpty() && t[0] == answers[k]
            if (!ok) all = false
            if (t.isNotEmpty()) {
                et.background = Ui.shape(
                    if (ok) 0xFFBDF0CF.toInt() else 0xFFF7C0C0.toInt(),
                    Ui.dp(ctx, 8).toFloat(), Ui.dp(ctx, 2), Ui.GREEN
                )
            }
        }
        if (all) {
            msg.text = "🎉 Tudo certo!"
            addView(Ui.btn(ctx, "Próxima cruzada →") {
                idx = (idx + 1) % puzzles.size
                build()
            })
        } else {
            msg.text = "Quase lá… confira os vermelhos"
        }
    }
}
