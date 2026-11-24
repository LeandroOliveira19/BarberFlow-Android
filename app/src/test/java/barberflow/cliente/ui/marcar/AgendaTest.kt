package barberflow.cliente.ui.marcar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Carrossel de dias do Marcar e horários separados em manhã e tarde. */
class AgendaTest {
    private val segunda = LocalDate.of(2026, 11, 23)   // segunda-feira

    @Test fun carrossel_vai_de_hoje_ate_ao_ultimo_dia_permitido() {
        val dias = diasCarrossel(segunda, maxDias = 14, abertos = listOf(1, 2, 3, 4, 5, 6))
        assertEquals(15, dias.size)
        assertEquals(segunda, dias.first().data)
        assertEquals(segunda.plusDays(14), dias.last().data)
    }

    @Test fun domingo_fechado_fica_desligado() {
        val dias = diasCarrossel(segunda, maxDias = 7, abertos = listOf(1, 2, 3, 4, 5, 6))
        val domingo = dias.first { it.data.dayOfWeek.value == 7 }
        assertFalse(domingo.aberto)
        assertTrue(dias.first().aberto)
    }

    @Test fun sem_dias_permitidos_mostra_so_hoje() {
        assertEquals(1, diasCarrossel(segunda, maxDias = 0, abertos = listOf(1)).size)
        assertEquals(1, diasCarrossel(segunda, maxDias = -3, abertos = listOf(1)).size)
    }

    @Test fun horarios_separados_em_manha_e_tarde() {
        val (manha, tarde) = separarPorPeriodo(listOf("09:00", "11:45", "12:00", "14:30", "19:15"))
        assertEquals(listOf("09:00", "11:45"), manha)
        assertEquals(listOf("12:00", "14:30", "19:15"), tarde)
    }

    @Test fun horarios_so_da_tarde() {
        val (manha, tarde) = separarPorPeriodo(listOf("15:00"))
        assertEquals(emptyList<String>(), manha)
        assertEquals(listOf("15:00"), tarde)
    }
}
