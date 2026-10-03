package com.oliveira.joguinhosdamamae

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(10), dp(12), dp(16))
        }

        val titulo = TextView(this).apply {
            text = "🌷  Joguinhos da Mamãe"
            textSize = 28f
            setTextColor(Color.rgb(55, 55, 55))
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding(0, dp(4), 0, dp(14))
        }

        root.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val grade = GridLayout(this).apply {
            columnCount = 2
            rowCount = 3
            useDefaultMargins = false
        }

        adicionarTile(grade, "🀄\nMahjong", Color.rgb(230, 245, 235))
        adicionarTile(grade, "🐦\nPassarinhos", Color.rgb(225, 240, 250))
        adicionarTile(grade, "🔩\nParafusos\ncoloridos", Color.rgb(245, 235, 215))
        adicionarTile(grade, "🏖️\nAreia\ncolorida", Color.rgb(250, 240, 215))
        adicionarTile(grade, "🔤\nCaça-palavras", Color.rgb(235, 230, 250))

        val vazio = TextView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        grade.addView(
            vazio,
            GridLayout.LayoutParams().apply {
                width = 0
                height = dp(150)
                columnSpec = GridLayout.spec(1)
                rowSpec = GridLayout.spec(2)
            }
        )

        root.addView(
            grade,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
    }

    private fun adicionarTile(
        grade: GridLayout,
        texto: String,
        cor: Int
    ) {
        val tile = TextView(this).apply {
            text = texto
            textSize = 22f
            setTextColor(Color.rgb(45, 45, 45))
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)

            background = GradientDrawable().apply {
                setColor(cor)
                cornerRadius = dp(3).toFloat()
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                // Os jogos serão ligados aqui.
            }
        }

        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(150)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(5), dp(5), dp(5), dp(5))
        }

        grade.addView(tile, params)
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}
