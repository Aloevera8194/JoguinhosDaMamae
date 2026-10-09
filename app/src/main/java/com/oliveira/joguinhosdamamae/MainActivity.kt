package com.oliveira.joguinhosdamamae

import android.app.Activity
import android.os.Bundle
import android.view.View

class MainActivity : Activity() {
    private var inMenu = true

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        showMenu()
    }

    fun showMenu() {
        inMenu = true
        val root = Ui.column(this)
        root.addView(Ui.title(this, "🧩 Joguinhos da Mamãe"))
        root.addView(Ui.btn(this, "🔩 Ordenar Porcas") { open(SortGame(this, true, ::showMenu)) })
        root.addView(Ui.btn(this, "🐦 Ordenar Pássaros") { open(SortGame(this, false, ::showMenu)) })
        root.addView(Ui.btn(this, "🀄 Mahjong") { open(MahjongGame(this, ::showMenu)) })
        root.addView(Ui.btn(this, "🔎 Caça-Palavras") { open(WordSearchGame(this, ::showMenu)) })
        root.addView(Ui.btn(this, "✏️ Palavras Cruzadas") { open(CrosswordGame(this, ::showMenu)) })
        root.addView(Ui.btn(this, "🎨 Areia Colorida") { open(SandGame(this, ::showMenu)) })
        root.addView(Ui.btn(this, "🔢 Sudoku") { open(SudokuGame(this, ::showMenu)) })
        setContentView(Ui.scroll(this, root))
    }

    private fun open(v: View) {
        inMenu = false
        setContentView(Ui.scroll(this, v))
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (inMenu) super.onBackPressed() else showMenu()
    }
}
