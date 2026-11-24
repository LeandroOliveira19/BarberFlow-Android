package barberflow.cliente.ui

import android.animation.ValueAnimator
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.animation.LinearInterpolator
import android.widget.TextView
import kotlin.math.roundToInt

/*
 * Efeito "palavra a palavra" (o mesmo do site e da gestão): cada palavra passa de transparente a visível,
 * uma a seguir à outra. As regras (onde está cada palavra, quanta transparência tem) são funções puras com teste JVM.
 */

/** Posição (início..fim, inclusivo) de cada palavra; uma palavra é tudo o que não é espaço. */
fun intervalosPalavras(texto: String): List<IntRange> =
    Regex("\\S+").findAll(texto).map { it.range }.toList()

/** Transparência (0–255) da palavra [indice] aos [tempoMs]: começa aos indice×atraso e demora [duracaoMs] a aparecer. */
fun alphaPalavra(indice: Int, tempoMs: Long, atrasoMs: Long, duracaoMs: Long): Int {
    val inicio = indice * atrasoMs
    val progresso = ((tempoMs - inicio).toDouble() / duracaoMs).coerceIn(0.0, 1.0)
    return (progresso * 255).roundToInt()
}

/**
 * Mostra [texto] no [TextView] palavra a palavra. Com as animações do sistema desligadas
 * (equivalente Android ao "reduzir movimento"), o texto aparece logo inteiro.
 * O TalkBack lê sempre a frase completa (o texto não muda, só a cor de cada palavra).
 */
fun TextView.animarPalavras(texto: CharSequence, atrasoMs: Long = 80, duracaoMs: Long = 500) {
    val palavras = intervalosPalavras(texto.toString())
    if (palavras.isEmpty() || !ValueAnimator.areAnimatorsEnabled()) {
        text = texto
        return
    }
    val corBase = currentTextColor
    val total = (palavras.size - 1) * atrasoMs + duracaoMs
    fun desenhar(tempo: Long) {
        val s = SpannableString(texto)
        palavras.forEachIndexed { i, r ->
            val alpha = alphaPalavra(i, tempo, atrasoMs, duracaoMs)
            val cor = (corBase and 0x00FFFFFF) or (alpha * ((corBase ushr 24) and 0xFF) / 255 shl 24)
            s.setSpan(ForegroundColorSpan(cor), r.first, r.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        text = s
    }
    desenhar(0)
    ValueAnimator.ofFloat(0f, total.toFloat()).apply {
        duration = total
        interpolator = LinearInterpolator()
        addUpdateListener { desenhar((it.animatedValue as Float).toLong()) }
        start()
    }
}
