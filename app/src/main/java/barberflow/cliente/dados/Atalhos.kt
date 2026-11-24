package barberflow.cliente.dados

import barberflow.cliente.dados.local.MarcacaoLocal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * Atalhos das marcações (só para mostrar botões; nada disto altera dados no servidor).
 * As datas vêm no formato da API ("2026-11-24 15:00:00", hora de Lisboa), que se compara como texto.
 */

/** Contactar a barbearia faz sentido numa marcação confirmada que ainda não aconteceu. */
fun podeContactar(m: MarcacaoLocal, agora: String): Boolean = m.estado == "confirmada" && m.inicio > agora

/** "Marcar outra vez": numa marcação que já passou ou terminou, desde que se saiba o serviço e o barbeiro. */
fun podeMarcarOutraVez(m: MarcacaoLocal, agora: String): Boolean =
    m.servicoId != null && m.barbeiroId != null && !podeContactar(m, agora)

/** A próxima marcação do cliente (para o cartão do Início), ou null se não houver nenhuma. */
fun proximaMarcacao(lista: List<MarcacaoLocal>, agora: String): MarcacaoLocal? =
    lista.filter { podeContactar(it, agora) }.minByOrNull { it.inicio }

/** Dias de calendário entre hoje e a marcação: 0 = hoje, 1 = amanhã. */
fun diasAte(inicio: String, agora: String): Long =
    ChronoUnit.DAYS.between(LocalDate.parse(agora.take(10)), LocalDate.parse(inicio.take(10)))

/**
 * Número para o WhatsApp (wa.me) a partir do telemóvel da barbearia: "912 345 678" → "351912345678".
 * Só telemóveis portugueses (começam por 9); um fixo ou um valor inválido devolve null e o botão não aparece.
 */
fun numeroWhatsApp(telefone: String?): String? {
    val digitos = telefone.orEmpty().filter { it.isDigit() || it == '+' }
    val nacional = digitos.removePrefix("+351").removePrefix("00351")
    return if (Regex("^9\\d{8}$").matches(nacional)) "351$nacional" else null
}
