package com.oliveira.joguinhosdamamae

object CwGen {
    private class E(val a: String, val clue: String)

    private class Board(val limR: Int, val limC: Int) {
        val g = Array(40) { CharArray(40) { '.' } }
        val hasA = Array(40) { BooleanArray(40) }
        val hasD = Array(40) { BooleanArray(40) }
        val words = ArrayList<CwWord>()
        var minR = 1000
        var maxR = -1
        var minC = 1000
        var maxC = -1
    }

    private const val N = 40
    private const val MID = 20

    private val bank = listOf(
        E("SOL", "Estrela que ilumina o dia"),
        E("LUA", "Brilha no céu durante a noite"),
        E("MAR", "Grande extensão de água salgada"),
        E("RIO", "Água doce que corre até o mar"),
        E("PAO", "Alimento feito de farinha, comum no café"),
        E("UVA", "Fruta pequena que dá o vinho"),
        E("AVO", "Mãe do seu pai ou da sua mãe"),
        E("REI", "Governa o reino"),
        E("CAO", "Fiel amigo que late"),
        E("OVO", "A galinha bota"),
        E("BOI", "Animal de chifres que dá carne"),
        E("PAZ", "Tranquilidade, sem guerra"),
        E("MAE", "Quem nos deu à luz"),
        E("PAI", "Chefe de família, o papai"),
        E("TIO", "Irmão do seu pai ou da sua mãe"),
        E("MAO", "Tem cinco dedos"),
        E("CEU", "Onde ficam as nuvens e as estrelas"),
        E("SAL", "Tempero branco da comida"),
        E("MEL", "Doce feito pelas abelhas"),
        E("CHA", "Bebida de ervas e água quente"),
        E("LUZ", "Clareia o ambiente"),
        E("SOFA", "Móvel macio para sentar"),
        E("GATO", "Bichano que mia"),
        E("FOGO", "Queima e dá calor"),
        E("FLOR", "Enfeita o jardim"),
        E("CASA", "Lugar onde moramos"),
        E("BOLO", "Doce de aniversário"),
        E("MESA", "Móvel onde fazemos as refeições"),
        E("SOPA", "Prato quente servido com colher"),
        E("AMOR", "Sentimento de carinho profundo"),
        E("NETO", "Filho do seu filho"),
        E("OLHO", "Usamos para ver"),
        E("BOCA", "Onde ficam os dentes e a língua"),
        E("CAMA", "Onde dormimos"),
        E("SONO", "Vontade de dormir"),
        E("AULA", "Lição dada pelo professor"),
        E("PATO", "Ave que nada e faz quá"),
        E("VACA", "Dá leite e muge"),
        E("TREM", "Anda sobre trilhos"),
        E("CRUZ", "Símbolo do cristianismo"),
        E("FOTO", "Imagem feita pela câmera"),
        E("ANEL", "Joia usada no dedo"),
        E("TETO", "Parte de cima de um cômodo"),
        E("CHAO", "Superfície onde pisamos dentro de casa"),
        E("FRIO", "Sensação de temperatura baixa"),
        E("DOCE", "Sabor do açúcar"),
        E("SUCO", "Bebida feita de fruta espremida"),
        E("AGUA", "Líquido que bebemos para matar a sede"),
        E("OURO", "Metal amarelo e precioso"),
        E("ROSA", "Flor perfumada com espinhos"),
        E("FACA", "Talher que corta"),
        E("COPO", "Serve para beber água"),
        E("CAFE", "Bebida quente e escura da manhã"),
        E("PERA", "Fruta de formato parecido com um sino"),
        E("LIVRO", "Tem páginas e histórias para ler"),
        E("PRAIA", "Areia e mar"),
        E("CHUVA", "Água que cai das nuvens"),
        E("VENTO", "Ar em movimento"),
        E("NUVEM", "Fica branca ou cinza no céu"),
        E("HORTA", "Plantação de verduras"),
        E("LEITE", "Bebida branca que vem da vaca"),
        E("ARROZ", "Grão branco que acompanha o feijão"),
        E("MILHO", "Espiga de grãos amarelos"),
        E("FESTA", "Comemoração com amigos e música"),
        E("BEIJO", "Carinho dado com os lábios"),
        E("AMIGO", "Companheiro de confiança"),
        E("FILHO", "Criança em relação aos pais"),
        E("IRMAO", "Filho dos mesmos pais que você"),
        E("PRIMO", "Filho do seu tio"),
        E("DENTE", "Mastiga a comida"),
        E("NARIZ", "Fica no meio do rosto e serve para cheirar"),
        E("BRACO", "Parte do corpo entre o ombro e a mão"),
        E("PERNA", "Parte do corpo entre o quadril e o pé"),
        E("SONHO", "Imagens que aparecem quando dormimos"),
        E("PROVA", "Teste de conhecimentos na escola"),
        E("CAMPO", "Terreno aberto, fora da cidade"),
        E("PORCO", "Animal que gosta de lama"),
        E("PEIXE", "Vive na água e tem nadadeiras"),
        E("CARRO", "Veículo de quatro rodas"),
        E("AVIAO", "Voa levando passageiros"),
        E("BARCO", "Navega sobre a água"),
        E("PONTE", "Liga as duas margens de um rio"),
        E("PRACA", "Espaço público com bancos e árvores"),
        E("SANTO", "Pessoa venerada pela igreja"),
        E("TERRA", "Planeta em que vivemos"),
        E("FOLHA", "Parte verde da planta"),
        E("FRUTA", "Alimento doce que nasce em árvores"),
        E("PRATO", "Onde servimos a comida"),
        E("GARFO", "Talher de pontas"),
        E("LINHA", "Fio usado para costurar"),
        E("BINGO", "Jogo de cartela com números sorteados"),
        E("CANTO", "Voz que entoa uma melodia"),
        E("DANCA", "Movimento do corpo no ritmo da música"),
        E("RADIO", "Aparelho que toca música e notícias"),
        E("CARTA", "Mensagem escrita enviada pelo correio"),
        E("BOLSA", "Acessório para carregar objetos"),
        E("PORTA", "Abre e fecha na entrada da casa"),
        E("CHAVE", "Abre a fechadura"),
        E("CALOR", "Sensação de temperatura alta"),
        E("NATAL", "Festa de 25 de dezembro"),
        E("PRATA", "Metal branco e brilhante"),
        E("FERRO", "Metal escuro e resistente"),
        E("VERAO", "Estação mais quente do ano"),
        E("TRICO", "Artesanato feito com duas agulhas"),
        E("FEIJAO", "Grão escuro cozido no caldo"),
        E("PIPOCA", "Lanche do cinema que estoura"),
        E("RISADA", "Som de quem acha graça"),
        E("ABRACO", "Carinho dado com os braços"),
        E("ORELHA", "Parte do corpo usada para ouvir"),
        E("CABELO", "Cresce na cabeça"),
        E("ESCOLA", "Lugar onde se aprende"),
        E("CAVALO", "Animal que corre e pode ser montado"),
        E("ABELHA", "Inseto que faz mel"),
        E("ONIBUS", "Transporte coletivo da cidade"),
        E("IGREJA", "Templo para rezar"),
        E("ORACAO", "Prece feita a Deus"),
        E("BENCAO", "Proteção pedida a Deus"),
        E("ARVORE", "Planta grande com tronco"),
        E("PANELA", "Usada no fogão para cozinhar"),
        E("COLHER", "Talher usado na sopa"),
        E("CROCHE", "Artesanato feito com uma agulha e linha"),
        E("NOVELA", "Folhetim da televisão"),
        E("MUSICA", "Arte dos sons"),
        E("VIOLAO", "Instrumento de seis cordas"),
        E("SAPATO", "Calçado para os pés"),
        E("CHAPEU", "Cobre a cabeça"),
        E("CAMISA", "Peça de roupa com botões"),
        E("JANELA", "Abertura com vidro na parede"),
        E("PASCOA", "Festa dos ovos de chocolate"),
        E("BANANA", "Fruta amarela e comprida"),
        E("JARDIM", "Lugar com flores e plantas"),
        E("QUEIJO", "Derivado do leite, amarelo ou branco"),
        E("ESTRELA", "Pontinho brilhante no céu à noite"),
        E("SORRISO", "Alegria que aparece no rosto"),
        E("SAUDADE", "Falta de quem está longe"),
        E("CARINHO", "Gesto de afeto"),
        E("FAMILIA", "Pais, filhos e irmãos"),
        E("CORACAO", "Bate no peito"),
        E("RELOGIO", "Marca as horas"),
        E("FAZENDA", "Propriedade rural"),
        E("GALINHA", "Ave que bota ovos"),
        E("PASSARO", "Ave que voa e canta"),
        E("FORMIGA", "Inseto pequeno que carrega folhas"),
        E("COZINHA", "Cômodo onde se prepara a comida"),
        E("VESTIDO", "Peça de roupa inteira, usada por mulheres"),
        E("INVERNO", "Estação mais fria do ano"),
        E("DOMINGO", "Dia de descanso da semana"),
        E("JANEIRO", "Primeiro mês do ano")
    )

    private fun inB(r: Int, c: Int) = r in 0 until N && c in 0 until N

    private fun empty(b: Board, r: Int, c: Int) = !inB(r, c) || b.g[r][c] == '.'

    private fun canPlace(b: Board, w: String, r: Int, c: Int, across: Boolean): Boolean {
        val dr = if (across) 0 else 1
        val dc = if (across) 1 else 0
        val er = r + dr * (w.length - 1)
        val ec = c + dc * (w.length - 1)
        if (!inB(r, c) || !inB(er, ec)) return false
        if (!empty(b, r - dr, c - dc) || !empty(b, er + dr, ec + dc)) return false
        val nr0 = minOf(b.minR, r)
        val nr1 = maxOf(b.maxR, er)
        val nc0 = minOf(b.minC, c)
        val nc1 = maxOf(b.maxC, ec)
        if (nr1 - nr0 + 1 > b.limR || nc1 - nc0 + 1 > b.limC) return false
        var cross = 0
        for (i in w.indices) {
            val rr = r + dr * i
            val cc = c + dc * i
            val ch = b.g[rr][cc]
            if (ch != '.') {
                if (ch != w[i]) return false
                if (across && b.hasA[rr][cc]) return false
                if (!across && b.hasD[rr][cc]) return false
                cross++
            } else {
                if (!empty(b, rr + dc, cc + dr) || !empty(b, rr - dc, cc - dr)) return false
            }
        }
        return cross >= 1
    }

    private fun put(b: Board, e: E, r: Int, c: Int, across: Boolean) {
        val dr = if (across) 0 else 1
        val dc = if (across) 1 else 0
        for (i in e.a.indices) {
            val rr = r + dr * i
            val cc = c + dc * i
            b.g[rr][cc] = e.a[i]
            if (across) b.hasA[rr][cc] = true else b.hasD[rr][cc] = true
        }
        b.minR = minOf(b.minR, r)
        b.maxR = maxOf(b.maxR, r + dr * (e.a.length - 1))
        b.minC = minOf(b.minC, c)
        b.maxC = maxOf(b.maxC, c + dc * (e.a.length - 1))
        b.words.add(CwWord(r, c, across, e.a, e.clue))
    }

    private fun tryPlace(b: Board, e: E): Boolean {
        for (p in b.words.shuffled()) {
            for (i in e.a.indices) for (j in p.a.indices) {
                if (e.a[i] != p.a[j]) continue
                val across = !p.across
                val r = if (p.across) p.r - i else p.r + j
                val c = if (p.across) p.col + j else p.col - i
                if (canPlace(b, e.a, r, c, across)) {
                    put(b, e, r, c, across)
                    return true
                }
            }
        }
        return false
    }

    private fun build(target: Int, maxLen: Int, limR: Int, limC: Int): List<CwWord> {
        val b = Board(limR, limC)
        val pool = bank.filter { it.a.length in 3..maxLen }.shuffled()
        val first = pool.firstOrNull { it.a.length >= 4 } ?: return emptyList()
        put(b, first, MID, MID - first.a.length / 2, true)
        val used = HashSet<String>()
        used.add(first.a)
        for (e in pool) {
            if (b.words.size >= target) break
            if (used.contains(e.a)) continue
            if (tryPlace(b, e)) used.add(e.a)
        }
        return b.words.map { CwWord(it.r - b.minR, it.col - b.minC, it.across, it.a, it.clue) }
    }

    fun generate(level: Int): List<CwWord> {
        val target = minOf(3 + level, 11)
        val maxLen = when {
            level < 3 -> 5
            level < 7 -> 6
            else -> 7
        }
        val limR = if (level < 3) 6 else if (level < 6) 8 else 10
        val limC = if (level < 3) 6 else if (level < 6) 7 else 8
        var best: List<CwWord> = emptyList()
        for (k in 0 until 80) {
            val res = build(target, maxLen, limR, limC)
            if (res.size > best.size) best = res
            if (best.size >= target) break
        }
        if (best.size < 3) {
            return listOf(
                CwWord(0, 0, true, "FLOR", "Enfeita o jardim"),
                CwWord(0, 0, false, "FOGO", "Queima e dá calor"),
                CwWord(0, 2, false, "OVAL", "Formato de ovo"),
                CwWord(2, 0, true, "GRAU", "Unidade de temperatura")
            )
        }
        return best
    }
}
