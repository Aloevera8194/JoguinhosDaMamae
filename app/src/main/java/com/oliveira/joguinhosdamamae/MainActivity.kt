package com.oliveira.joguinhosdamamae

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.app.Activity

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tela = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        val titulo = TextView(this).apply {
            text = "🌷 Joguinhos da Mamãe"
            textSize = 30f
            setTextColor(Color.rgb(60, 60, 60))
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 30)
        }

        tela.addView(titulo)

        adicionarJogo(tela, "🀄  Mahjong")
        adicionarJogo(tela, "🐦  Passarinhos")
        adicionarJogo(tela, "🔩  Parafusos coloridos")
        adicionarJogo(tela, "🏖️  Areia colorida")
        adicionarJogo(tela, "🔤  Caça-palavras")

        setContentView(tela)
    }

    private fun adicionarJogo(tela: LinearLayout, nome: String) {
        val botao = TextView(this).apply {
            text = nome
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(50, 50, 50))
            setBackgroundColor(Color.rgb(240, 240, 240))
            setPadding(20, 25, 20, 25)
            isClickable = true
            isFocusable = true
        }

        val espaco = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 18)
        }

        tela.addView(botao, espaco)
    }
}
