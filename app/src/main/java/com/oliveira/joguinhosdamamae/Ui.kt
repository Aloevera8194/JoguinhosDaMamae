package com.oliveira.joguinhosdamamae

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object Ui {
    const val BG = 0xFFF4F1EA.toInt()
    const val GREEN = 0xFF1F7A4A.toInt()
    const val CARD = 0xFFFFFFFF.toInt()
    const val MUTED = 0xFFE3DED2.toInt()
    const val TEXT = 0xFF222222.toInt()

    fun dp(c: Context, n: Int): Int = (n * c.resources.displayMetrics.density).toInt()

    fun shape(color: Int, radius: Float, stroke: Int = 0, strokeColor: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (stroke > 0) setStroke(stroke, strokeColor)
        }

    fun column(c: Context): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(c, 14), dp(c, 28), dp(c, 14), dp(c, 24))
        setBackgroundColor(BG)
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun row(c: Context): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    fun scroll(c: Context, v: View): ScrollView = ScrollView(c).apply {
        setBackgroundColor(BG)
        isFillViewport = true
        addView(v)
    }

    fun text(c: Context, s: String, size: Float = 16f, bold: Boolean = false): TextView =
        TextView(c).apply {
            text = s
            textSize = size
            setTextColor(TEXT)
            gravity = Gravity.CENTER
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    fun title(c: Context, s: String): TextView = text(c, s, 26f, true).apply {
        setPadding(0, dp(c, 8), 0, dp(c, 18))
    }

    fun btn(c: Context, s: String, onClick: () -> Unit): Button = Button(c).apply {
        text = s
        textSize = 17f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = shape(GREEN, dp(c, 14).toFloat())
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 58)
        ).apply { setMargins(0, dp(c, 8), 0, dp(c, 8)) }
    }

    fun smallBtn(c: Context, s: String, onClick: () -> Unit): Button = Button(c).apply {
        text = s
        textSize = 14f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = shape(GREEN, dp(c, 12).toFloat())
        setPadding(dp(c, 14), 0, dp(c, 14), 0)
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(c, 44)
        ).apply { setMargins(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4)) }
    }

    /** Cabeçalho com botão voltar + título + mensagem de status. Devolve o TextView da mensagem. */
    fun header(c: Context, parent: LinearLayout, title: String, onBack: () -> Unit): TextView {
        val bar = row(c)
        bar.addView(smallBtn(c, "← Menu", onBack))
        bar.addView(text(c, title, 18f, true).apply {
            setPadding(dp(c, 12), 0, 0, 0)
        })
        parent.addView(bar)
        val msg = text(c, "", 18f, true).apply {
            setTextColor(GREEN)
            setPadding(0, dp(c, 8), 0, dp(c, 8))
        }
        parent.addView(msg)
        return msg
    }
}
