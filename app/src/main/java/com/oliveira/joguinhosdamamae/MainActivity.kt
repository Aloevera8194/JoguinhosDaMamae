package com.oliveira.joguinhosdamamae

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var area: FrameLayout
    private val prefs by lazy { getSharedPreferences("progresso", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mostrarMenu()
    }

    private fun mostrarMenu() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 245, 245))
            setPadding(dp(10), dp(8), dp(10), dp(10))
        }

        val titulo = TextView(this).apply {
            text = "🌷  Joguinhos da Mamãe"
            textSize = 29f
            setTextColor(Color.rgb(45, 45, 45))
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(12))
        }
        root.addView(titulo, LinearLayout.LayoutParams(-1, dp(70)))

        val grade = GridLayout(this).apply {
            columnCount = 2
            useDefaultMargins = false
        }

        tile(grade, "🀄\nMahjong", Color.rgb(72, 145, 104), 2)
        tile(grade, "🐦\nPassarinhos", Color.rgb(55, 125, 175), 1)
        tile(grade, "🔩\nParafusos\ncoloridos", Color.rgb(205, 145, 55), 1)
        tile(grade, "🏖️\nAreia\ncolorida", Color.rgb(205, 120, 70), 1)
        tile(grade, "🔤\nCaça-palavras", Color.rgb(125, 95, 175), 2)

        root.addView(
            grade,
            LinearLayout.LayoutParams(-1, 0).apply { weight = 1f }
        )

        setContentView(root)
    }

    private fun tile(
        grade: GridLayout,
        texto: String,
        cor: Int,
        span: Int
    ) {
        val t = TextView(this).apply {
            text = texto
            textSize = if (span == 2) 25f else 21f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            background = GradientDrawable().apply {
                setColor(cor)
                cornerRadius = dp(2).toFloat()
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        t.setOnClickListener {
            when {
                texto.contains("Mahjong") -> abrirJogo(Jogo.MAHJONG)
                texto.contains("Passarinhos") -> abrirJogo(Jogo.PASSARINHOS)
                texto.contains("Parafusos") -> abrirJogo(Jogo.PARAFUSOS)
                texto.contains("Areia") -> abrirJogo(Jogo.AREIA)
                texto.contains("Caça-palavras") -> abrirJogo(Jogo.PALAVRAS)
            }
        }

        val lp = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(if (span == 2) 180 else 155)
            columnSpec = GridLayout.spec(
                GridLayout.UNDEFINED,
                span,
                1f
            )
            setMargins(dp(5), dp(5), dp(5), dp(5))
        }
        grade.addView(t, lp)
    }

    private fun abrirJogo(jogo: Jogo) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val topo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(18), dp(8), dp(8))
        }

        val voltar = TextView(this).apply {
            text = "‹"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
        }
        voltar.setOnClickListener { mostrarMenu() }

        val nome = TextView(this).apply {
            text = jogo.titulo
            textSize = 26f
            setTextColor(Color.rgb(45, 45, 45))
            gravity = Gravity.CENTER
        }

        topo.addView(voltar, LinearLayout.LayoutParams(dp(60), dp(68)))
        topo.addView(nome, LinearLayout.LayoutParams(0, dp(68), 1f))

        val reiniciar = TextView(this).apply {
            text = "↻"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
        }
        reiniciar.setOnClickListener {
            prefs.edit().remove(jogo.chave).apply()
            abrirJogo(jogo)
        }
        topo.addView(reiniciar, LinearLayout.LayoutParams(dp(60), dp(68)))

        root.addView(topo)

        area = FrameLayout(this)
        root.addView(area, LinearLayout.LayoutParams(-1, 0, 1f))

        val ajuda = TextView(this).apply {
            text = jogo.instrucao
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(70, 70, 70))
            setPadding(dp(8), dp(8), dp(8), dp(12))
        }
        root.addView(ajuda, LinearLayout.LayoutParams(-1, dp(55)))

        area.addView(JogoView(this, jogo))

        setContentView(root)
    }

    enum class Jogo(
        val titulo: String,
        val chave: String,
        val instrucao: String
    ) {
        MAHJONG("🀄 Mahjong", "mahjong", "Toque em duas peças iguais para formar um par."),
        PASSARINHOS("🐦 Passarinhos", "passarinhos", "Toque nos passarinhos para ajudá-los a voar."),
        PARAFUSOS("🔩 Parafusos coloridos", "parafusos", "Toque em um parafuso e depois em outro tubo."),
        AREIA("🏖️ Areia colorida", "areia", "Escolha uma cor e toque nos espaços para preenchê-los."),
        PALAVRAS("🔤 Caça-palavras", "palavras", "Toque no começo e depois no final de uma palavra.")
    }

    inner class JogoView(
        context: Context,
        private val jogo: Jogo
    ) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val random = java.util.Random()

        // Mahjong
        private val mahjong = IntArray(16)
        private val mahjongAberta = BooleanArray(16)
        private var primeira = -1

        // Passarinhos
        private var passarinhoX = 0
        private var passarinhoY = 0
        private var pontosPassaro = 0

        // Parafusos
        private val tubos = Array(3) { mutableListOf<Int>() }
        private var parafusoSelecionado = -1

        // Areia
        private val areia = Array(6) { IntArray(5) }
        private var corAreia = 1

        // Caça-palavras
        private val letras = Array(8) { CharArray(8) { 'A' } }
        private val niveis = arrayOf(
            arrayOf("CASA", "FLOR", "SOL", "AMOR"),
            arrayOf("MAMAE", "LUA", "GATO", "VIDA"),
            arrayOf("JOGO", "BOLA", "PATO", "CEU"),
            arrayOf("PAZ", "AMIGO", "CASA", "FLOR"),
            arrayOf("MUSICA", "DIA", "SOL", "MAR"),
            arrayOf("FAMILIA", "AMOR", "LUA", "GATO"),
            arrayOf("JARDIM", "FLOR", "PASSARO", "CEU"),
            arrayOf("ALEGRIA", "VIDA", "PAZ", "CASA"),
            arrayOf("CARINHO", "AMIGO", "SOL", "MAR"),
            arrayOf("MAMAE", "FAMILIA", "AMOR", "FLOR")
        )
        private var nivelPalavras = 0
        private val encontradas = mutableSetOf<String>()
        private var palavraInicio = -1
        private var palavraFim = -1

        init {
            carregar()
            setBackgroundColor(Color.WHITE)
        }

        private fun carregar() {
            when (jogo) {
                Jogo.MAHJONG -> {
                    val salvo = prefs.getString("mahjong", "")
                    if (salvo.isNullOrEmpty()) iniciarMahjong()
                    else {
                        salvo.split(",").forEachIndexed { i, v ->
                            if (i < 16) mahjongAberta[i] = v == "1"
                        }
                        for (i in 0 until 16) mahjong[i] = i / 2
                    }
                }
                Jogo.PASSARINHOS -> {
                    pontosPassaro = prefs.getInt("passarinhos", 0)
                    novoPassaro()
                }
                Jogo.PARAFUSOS -> iniciarParafusos()
                Jogo.AREIA -> {
                    val s = prefs.getString("areia", "")
                    if (!s.isNullOrEmpty()) {
                        val a = s.split(",")
                        for (i in 0 until 30)
                            areia[i / 5][i % 5] = a.getOrNull(i)?.toIntOrNull() ?: 0
                    }
                }
                Jogo.PALAVRAS -> {
                    nivelPalavras = prefs.getInt("palavras_nivel", 0)
                        .coerceIn(0, niveis.lastIndex)
                    val s = prefs.getString("palavras", "")
                    if (!s.isNullOrEmpty()) encontradas.addAll(s.split(","))
                    gerarNivelPalavras()
                }
            }
        }

        private fun iniciarMahjong() {
            val valores = (0 until 8).flatMap { listOf(it, it) }.shuffled()
            for (i in 0 until 16) {
                mahjong[i] = valores[i]
                mahjongAberta[i] = false
            }
        }

        private fun novoPassaro() {
            passarinhoX = random.nextInt(5)
            passarinhoY = random.nextInt(5)
        }

        private fun gerarNivelPalavras() {
            for (y in 0 until 8)
                for (x in 0 until 8)
                    letras[y][x] = ('A'.code + random.nextInt(26)).toChar()

            val rng = java.util.Random(5000L + nivelPalavras)

            for (palavra in niveis[nivelPalavras]) {
                var colocada = false

                repeat(500) {
                    if (colocada) return@repeat

                    val direcao = rng.nextInt(8)
                    val dx = when (direcao) {
                        0, 1, 7 -> 1
                        2, 6 -> 0
                        else -> -1
                    }
                    val dy = when (direcao) {
                        0, 2, 3 -> 0
                        1, 4 -> 1
                        else -> -1
                    }

                    if (dx == 0 && dy == 0) return@repeat

                    val x0 = rng.nextInt(8)
                    val y0 = rng.nextInt(8)
                    val x1 = x0 + dx * (palavra.length - 1)
                    val y1 = y0 + dy * (palavra.length - 1)

                    if (x1 !in 0..7 || y1 !in 0..7) return@repeat

                    var pode = true

                    for (i in palavra.indices) {
                        val ch = letras[y0 + dy * i][x0 + dx * i]
                        if (ch != 'A' && ch != palavra[i]) {
                            pode = false
                            break
                        }
                    }

                    if (pode) {
                        for (i in palavra.indices)
                            letras[y0 + dy * i][x0 + dx * i] = palavra[i]
                        colocada = true
                    }
                }
            }
        }

        private fun iniciarParafusos() {
            tubos.forEach { it.clear() }
            val cores = mutableListOf(0,0,0,0,1,1,1,1,2,2,2,2)
            cores.shuffle()
            for (i in cores.indices) tubos[i % 3].add(cores[i])
            tubos[0].clear()
            tubos[1].clear()
            tubos[2].clear()
            for (i in 0 until 4) tubos[0].add(cores[i])
            for (i in 4 until 8) tubos[1].add(cores[i])
            for (i in 8 until 12) tubos[2].add(cores[i])
            parafusoSelecionado = -1
        }

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            when (jogo) {
                Jogo.MAHJONG -> desenharMahjong(c)
                Jogo.PASSARINHOS -> desenharPassarinhos(c)
                Jogo.PARAFUSOS -> desenharParafusos(c)
                Jogo.AREIA -> desenharAreia(c)
                Jogo.PALAVRAS -> desenharPalavras(c)
            }
        }

        private fun texto(c: Canvas, s: String, x: Float, y: Float, tamanho: Float, cor: Int) {
            paint.color = cor
            paint.textSize = tamanho
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            c.drawText(s, x, y, paint)
        }

        private fun desenharMahjong(c: Canvas) {
            val w = width / 4f
            val h = height / 4f
            for (i in 0 until 16) {
                val x = i % 4 * w
                val y = i / 4 * h
                paint.color = if (mahjongAberta[i]) Color.rgb(220,220,220) else Color.rgb(245,238,215)
                c.drawRoundRect(x+8,y+8,x+w-8,y+h-8,14f,14f,paint)
                if (!mahjongAberta[i]) {
                    val simbolos = arrayOf("🌸","🍀","⭐","❤️","☀️","🌷","🍎","🦋")
                    texto(c, simbolos[mahjong[i]], x+w/2, y+h/2+15, 38f, Color.DKGRAY)
                }
            }
            val feitos = mahjongAberta.count { it }
            texto(c, "Pares: ${feitos/2} / 8", width/2f, height-18f, 20f, Color.DKGRAY)
        }

        private fun desenharPassarinhos(c: Canvas) {
            val tamanho = minOf(width, height) / 6f
            for (y in 0 until 5) {
                for (x in 0 until 5) {
                    paint.color = Color.rgb(235,245,250)
                    c.drawRoundRect(
                        x*tamanho+6, y*tamanho+6,
                        (x+1)*tamanho-6, (y+1)*tamanho-6,
                        18f,18f,paint
                    )
                }
            }
            texto(
                c, "🐦",
                passarinhoX*tamanho+tamanho/2,
                passarinhoY*tamanho+tamanho*0.72f,
                tamanho*0.55f,
                Color.DKGRAY
            )
            texto(c, "Passarinhos: $pontosPassaro", width/2f, height-25f, 23f, Color.DKGRAY)
        }

        private fun desenharParafusos(c: Canvas) {
            val bw = width / 3f
            for (t in 0..2) {
                paint.color = Color.rgb(235,235,235)
                c.drawRoundRect(t*bw+15,60f,(t+1)*bw-15,height-60f,25f,25f,paint)
                tubos[t].forEachIndexed { i, cor ->
                    val yy = height - 105f - i*75f
                    val cc = when(cor) {
                        0 -> Color.rgb(70,145,210)
                        1 -> Color.rgb(235,105,105)
                        else -> Color.rgb(95,175,105)
                    }
                    paint.color = cc
                    c.drawCircle(t*bw+bw/2, yy, 25f, paint)
                    paint.color = Color.DKGRAY
                    paint.strokeWidth = 6f
                    c.drawLine(t*bw+bw/2-16,yy-12,t*bw+bw/2+16,yy+12,paint)
                }
            }
            texto(c, "Escolha um parafuso e depois outro tubo", width/2f, 35f, 18f, Color.DKGRAY)
        }

        private fun desenharAreia(c: Canvas) {
            val cell = minOf(width/5f, (height-150f)/6f)
            val inicioX = (width - cell*5)/2f
            val inicioY = 90f
            val cores = arrayOf(
                Color.rgb(240,220,175),
                Color.rgb(235,100,100),
                Color.rgb(80,150,215),
                Color.rgb(100,180,110),
                Color.rgb(230,190,70)
            )
            for (y in 0 until 6) {
                for (x in 0 until 5) {
                    paint.color = cores[areia[y][x]]
                    c.drawRoundRect(
                        inicioX+x*cell+4,inicioY+y*cell+4,
                        inicioX+(x+1)*cell-4,inicioY+(y+1)*cell-4,
                        10f,10f,paint
                    )
                }
            }
            for (i in 1..4) {
                paint.color = cores[i]
                c.drawCircle(width/2f+(i-2.5f)*55f,height-65f,20f,paint)
            }
            texto(c,"Escolha a cor",width/2f,35f,20f,Color.DKGRAY)
        }

        private fun desenharPalavras(c: Canvas) {
            val cell = minOf(width / 8f, (height - 145f) / 8f)
            val ox = (width - cell * 8) / 2f
            val oy = 72f

            texto(
                c,
                "Nível ${nivelPalavras + 1} de ${niveis.size}",
                width / 2f,
                32f,
                22f,
                Color.DKGRAY
            )

            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val i = y * 8 + x
                    val selecionado = i == palavraInicio || i == palavraFim

                    paint.color = when {
                        selecionado -> Color.rgb(205, 180, 240)
                        (x + y) % 2 == 0 -> Color.rgb(240, 235, 250)
                        else -> Color.rgb(250, 248, 255)
                    }

                    c.drawRoundRect(
                        ox + x * cell + 2,
                        oy + y * cell + 2,
                        ox + (x + 1) * cell - 2,
                        oy + (y + 1) * cell - 2,
                        8f, 8f, paint
                    )

                    texto(
                        c,
                        letras[y][x].toString(),
                        ox + x * cell + cell / 2,
                        oy + y * cell + cell * 0.70f,
                        minOf(28f, cell * 0.52f),
                        Color.DKGRAY
                    )
                }
            }

            val lista = niveis[nivelPalavras]

            texto(
                c,
                lista.joinToString("   ") {
                    if (it in encontradas) "✓ $it" else "○ $it"
                },
                width / 2f,
                height - 62f,
                17f,
                Color.DKGRAY
            )

            texto(
                c,
                "Encontradas: ${encontradas.size} / ${lista.size}",
                width / 2f,
                height - 22f,
                19f,
                Color.DKGRAY
            )
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.action != MotionEvent.ACTION_UP) return true
            val x=e.x
            val y=e.y
            when (jogo) {
                Jogo.MAHJONG -> toqueMahjong(x,y)
                Jogo.PASSARINHOS -> toquePassaro(x,y)
                Jogo.PARAFUSOS -> toqueParafuso(x,y)
                Jogo.AREIA -> toqueAreia(x,y)
                Jogo.PALAVRAS -> toquePalavra(x,y)
            }
            invalidate()
            return true
        }

        private fun toqueMahjong(x:Float,y:Float) {
            val col=(x/(width/4f)).toInt().coerceIn(0,3)
            val row=(y/(height/4f)).toInt().coerceIn(0,3)
            val i=row*4+col
            if (mahjongAberta[i]) return
            if (primeira==-1) {
                primeira=i
                mahjongAberta[i]=true
            } else {
                mahjongAberta[i]=true
                if (mahjong[primeira] != mahjong[i]) {
                    postDelayed({
                        mahjongAberta[primeira]=false
                        mahjongAberta[i]=false
                        primeira=-1
                        invalidate()
                    },600)
                } else {
                    primeira=-1
                }
            }
            prefs.edit().putString("mahjong",mahjongAberta.joinToString(",") { if(it) "1" else "0" }).apply()
        }

        private fun toquePassaro(x:Float,y:Float) {
            val cell=minOf(width,height)/6f
            val cx=(x/cell).toInt()
            val cy=(y/cell).toInt()
            if (cx==passarinhoX && cy==passarinhoY) {
                pontosPassaro++
                prefs.edit().putInt("passarinhos",pontosPassaro).apply()
                novoPassaro()
            }
        }

        private fun toqueParafuso(x:Float,y:Float) {
            val bw=width/3f
            val t=(x/bw).toInt().coerceIn(0,2)
            if (y<50f) return
            if (parafusoSelecionado==-1) {
                if (tubos[t].isNotEmpty()) parafusoSelecionado=t
            } else {
                if (t != parafusoSelecionado &&
                    tubos[parafusoSelecionado].isNotEmpty() &&
                    tubos[t].size<4 &&
                    (tubos[t].isEmpty() ||
                     tubos[t].last()==tubos[parafusoSelecionado].last())
                ) {
                    tubos[t].add(tubos[parafusoSelecionado].removeAt(tubos[parafusoSelecionado].lastIndex))
                }
                parafusoSelecionado=-1
            }
        }

        private fun toqueAreia(x:Float,y:Float) {
            if (y>height-120) {
                val pos=((x-width/2f)/55f+2.5f).toInt()
                if(pos in 1..4) corAreia=pos
                return
            }
            val cell=minOf(width/5f,(height-150f)/6f)
            val ox=(width-cell*5)/2f
            val oy=90f
            val cx=((x-ox)/cell).toInt()
            val cy=((y-oy)/cell).toInt()
            if(cx in 0..4 && cy in 0..5) {
                areia[cy][cx]=corAreia
                val s=areia.flatMap{it.asList()}.joinToString(",")
                prefs.edit().putString("areia",s).apply()
            }
        }

        private fun toquePalavra(x:Float,y:Float) {
            val cell = minOf(width / 8f, (height - 145f) / 8f)
            val ox = (width - cell * 8) / 2f
            val oy = 72f

            val cx = ((x - ox) / cell).toInt()
            val cy = ((y - oy) / cell).toInt()

            if (cx !in 0..7 || cy !in 0..7) return

            val i = cy * 8 + cx

            if (palavraInicio == -1) {
                palavraInicio = i
                palavraFim = -1
                invalidate()
                return
            }

            palavraFim = i

            val x1 = palavraInicio % 8
            val y1 = palavraInicio / 8
            val x2 = i % 8
            val y2 = i / 8

            val dx = (x2 - x1).coerceIn(-1, 1)
            val dy = (y2 - y1).coerceIn(-1, 1)

            if (dx == 0 && dy == 0) {
                palavraInicio = -1
                palavraFim = -1
                invalidate()
                return
            }

            val n = maxOf(
                kotlin.math.abs(x2 - x1),
                kotlin.math.abs(y2 - y1)
            ) + 1

            val sb = StringBuilder()

            for (k in 0 until n) {
                val xx = x1 + dx * k
                val yy = y1 + dy * k

                if (xx !in 0..7 || yy !in 0..7) {
                    palavraInicio = -1
                    palavraFim = -1
                    invalidate()
                    return
                }

                sb.append(letras[yy][xx])
            }

            val palavra = sb.toString()
            val invertida = palavra.reversed()
            val alvo = niveis[nivelPalavras]

            if (palavra in alvo) {
                encontradas.add(palavra)
            } else if (invertida in alvo) {
                encontradas.add(invertida)
            }

            prefs.edit()
                .putString("palavras", encontradas.joinToString(","))
                .putInt("palavras_nivel", nivelPalavras)
                .apply()

            if (encontradas.size == alvo.size &&
                nivelPalavras < niveis.lastIndex
            ) {
                nivelPalavras++
                encontradas.clear()
                gerarNivelPalavras()

                prefs.edit()
                    .putInt("palavras_nivel", nivelPalavras)
                    .remove("palavras")
                    .apply()
            }

            palavraInicio = -1
            palavraFim = -1
            invalidate()
        }
    }

    private fun dp(v:Int):Int =
        (v*resources.displayMetrics.density).toInt()
}
