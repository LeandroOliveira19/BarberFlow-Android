package barberflow.cliente.ui.marcar

import java.time.LocalDate

/*
 * Regras do Marcar (funções puras, testadas em AgendaTest): os dias do carrossel e os horários
 * separados em manhã e tarde.
 */

/** Um dia do carrossel: [aberto] = a barbearia funciona nesse dia da semana. */
data class DiaCarrossel(val data: LocalDate, val aberto: Boolean)

/** De [hoje] até [maxDias] dias depois (no mínimo, só hoje); [abertos] usa 1 = segunda … 7 = domingo. */
fun diasCarrossel(hoje: LocalDate, maxDias: Int, abertos: List<Int>): List<DiaCarrossel> =
    (0..maxDias.coerceAtLeast(0)).map { i ->
        val d = hoje.plusDays(i.toLong())
        DiaCarrossel(d, d.dayOfWeek.value in abertos)
    }

/** Horários antes das 12:00 são da manhã; os outros, da tarde. Os horários vêm no formato "HH:mm". */
fun separarPorPeriodo(slots: List<String>): Pair<List<String>, List<String>> = slots.partition { it < "12:00" }
