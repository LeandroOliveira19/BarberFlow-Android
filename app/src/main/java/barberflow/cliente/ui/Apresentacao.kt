package barberflow.cliente.ui

import java.text.Normalizer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/*
 * Regras de apresentação dos ecrãs (funções puras, testadas em ApresentacaoTest):
 * o tipo de um serviço (para escolher o ícone), as iniciais de um barbeiro sem foto e o bloco de data de uma marcação.
 */

/** Tipo de serviço, deduzido do nome; cada tipo tem o seu ícone na lista de Serviços. */
enum class TipoServico { CORTE, BARBA, CORTE_E_BARBA, TRATAMENTO, OUTRO }

private fun semAcentos(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

fun tipoServico(nome: String): TipoServico {
    val n = semAcentos(nome)
    val corte = listOf("corte", "cabelo", "degrade", "fade").any { it in n }
    val barba = listOf("barba", "barbo").any { it in n }
    return when {
        corte && barba -> TipoServico.CORTE_E_BARBA
        barba -> TipoServico.BARBA
        corte -> TipoServico.CORTE
        listOf("alisamento", "tratamento", "coloracao", "pintura", "hidrata", "madeixa", "pigment").any { it in n } -> TipoServico.TRATAMENTO
        else -> TipoServico.OUTRO
    }
}

/** "Luis Paulo" → "LP"; "André Pinto Costa" → "AC" (primeiro e último nome); "Rui" → "R". */
fun iniciais(nome: String): String {
    val partes = nome.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (partes.isEmpty()) return ""
    val primeira = partes.first().first().toString()
    val ultima = if (partes.size > 1) partes.last().first().toString() else ""
    return (primeira + ultima).uppercase()
}

/** Dia do mês e mês abreviado em maiúsculas para o bloco de data ("2026-11-28 14:30:00" → "28" e "NOV"). */
fun diaEMes(inicio: String, locale: Locale): Pair<String, String> = try {
    val data = LocalDateTime.parse(inicio.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val mes = DateTimeFormatter.ofPattern("MMM", locale).format(data).filter { it.isLetter() }.uppercase(locale)
    data.dayOfMonth.toString() to mes
} catch (_: DateTimeParseException) {
    "" to ""
}
