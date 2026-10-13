package com.oliveira.joguinhosdamamae

import kotlin.random.Random

object SortGen {
    const val CAP = 4

    fun colorsFor(level: Int): Int = minOf(3 + level, 9)

    // Com apenas 1 pote vazio o gerador nunca conseguia montar a fase (a partir da fase 13)
    // e ficava preso num laço infinito, travando o app. 2 potes vazios funcionam em qualquer fase.
    fun emptiesFor(level: Int): Int = if (level <= 3) 3 else 2

    fun movesFor(level: Int): Int = minOf(120 + level * 25, 600)

    private fun solved(t: List<List<Int>>): Boolean {
        for (p in t) {
            if (p.isEmpty()) continue
            if (p.size != CAP) return false
            for (x in p) if (x != p[0]) return false
        }
        return true
    }

    fun generate(colors: Int, empties: Int, moves: Int): MutableList<MutableList<Int>> {
        // número limitado de tentativas: nunca fica preso para sempre
        for (tentativa in 0 until 300) {
            val t = MutableList(colors + empties) { i ->
                if (i < colors) MutableList(CAP) { i } else mutableListOf<Int>()
            }
            var done = 0
            var guard = 0
            while (done < moves && guard < moves * 50) {
                guard++
                val b = Random.nextInt(t.size)
                val tb = t[b]
                if (tb.isEmpty()) continue
                val col = tb[tb.size - 1]
                var run = 0
                while (run < tb.size && tb[tb.size - 1 - run] == col) run++
                val m = 1 + Random.nextInt(run)
                if (tb.size - m > 0 && tb[tb.size - m - 1] != col) continue
                val cands = t.indices.filter { j ->
                    j != b && t[j].size + m <= CAP && (t[j].isEmpty() || t[j][t[j].size - 1] != col)
                }
                if (cands.isEmpty()) continue
                val a = cands[Random.nextInt(cands.size)]
                for (k in 0 until m) t[a].add(tb.removeAt(tb.size - 1))
                done++
            }
            if (done >= moves / 2 && !solved(t)) {
                t.shuffle()
                return t
            }
        }
        // não achou: tenta com mais um pote vazio (mais fácil de gerar)
        if (empties < 3) return generate(colors, empties + 1, moves)
        // último recurso: quase pronto, com duas bolinhas trocadas (resolve usando os potes vazios)
        val t = MutableList(colors + empties) { i ->
            if (i < colors) MutableList(CAP) { i } else mutableListOf<Int>()
        }
        if (colors >= 2) {
            val a = t[0].removeAt(t[0].size - 1)
            val b = t[1].removeAt(t[1].size - 1)
            t[0].add(b)
            t[1].add(a)
        }
        t.shuffle()
        return t
    }
}
