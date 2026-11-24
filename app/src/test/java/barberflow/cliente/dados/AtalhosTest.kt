package barberflow.cliente.dados

import barberflow.cliente.dados.local.MarcacaoLocal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Atalhos das marcações: contactar a barbearia, marcar outra vez e a próxima marcação no Início. */
class AtalhosTest {
    private val agora = "2026-11-24 10:00:00"

    private fun m(id: Int, inicio: String, estado: String = "confirmada", servico: Int? = 1, barbeiro: Int? = 2) =
        MarcacaoLocal(id, "t", "rlbarber", "Corte", "André", inicio, inicio, estado, servicoId = servico, barbeiroId = barbeiro)

    @Test fun contactar_so_em_confirmada_futura() {
        assertTrue(podeContactar(m(1, "2026-11-24 15:00:00"), agora))
        assertFalse(podeContactar(m(1, "2026-11-23 15:00:00"), agora))
        assertFalse(podeContactar(m(1, "2026-11-25 15:00:00", "cancelada"), agora))
        assertFalse(podeContactar(m(1, "2026-11-25 15:00:00", "concluida"), agora))
    }

    @Test fun marcar_outra_vez_so_nas_que_ja_passaram_ou_terminaram() {
        assertTrue(podeMarcarOutraVez(m(1, "2026-11-20 15:00:00", "concluida"), agora))
        assertTrue(podeMarcarOutraVez(m(1, "2026-11-26 15:00:00", "cancelada"), agora))
        assertTrue(podeMarcarOutraVez(m(1, "2026-11-20 15:00:00", "confirmada"), agora))
        assertFalse(podeMarcarOutraVez(m(1, "2026-11-26 15:00:00", "confirmada"), agora))
        // sem serviço ou barbeiro (marcação antiga por sincronizar) não dá para pré-preencher
        assertFalse(podeMarcarOutraVez(m(1, "2026-11-20 15:00:00", "concluida", servico = null), agora))
        assertFalse(podeMarcarOutraVez(m(1, "2026-11-20 15:00:00", "concluida", barbeiro = null), agora))
    }

    @Test fun proxima_e_a_confirmada_futura_mais_cedo() {
        val lista = listOf(
            m(1, "2026-11-28 10:00:00"),
            m(2, "2026-11-25 09:00:00", "cancelada"),
            m(3, "2026-11-26 16:30:00"),
            m(4, "2026-11-20 10:00:00"),
        )
        assertEquals(3, proximaMarcacao(lista, agora)?.id)
    }

    @Test fun sem_proxima_devolve_null() {
        assertNull(proximaMarcacao(emptyList(), agora))
        assertNull(proximaMarcacao(listOf(m(1, "2026-11-20 10:00:00")), agora))
    }

    @Test fun dias_ate_conta_dias_de_calendario() {
        assertEquals(0, diasAte("2026-11-24 23:30:00", agora))
        assertEquals(1, diasAte("2026-11-25 08:00:00", "2026-11-24 23:59:00"))
        assertEquals(6, diasAte("2026-11-30 10:00:00", agora))
    }

    @Test fun whatsapp_aceita_telemovel_portugues() {
        assertEquals("351912345678", numeroWhatsApp("912 345 678"))
        assertEquals("351912345678", numeroWhatsApp("+351 912345678"))
        assertEquals("351932345678", numeroWhatsApp("00351932345678"))
    }

    @Test fun whatsapp_recusa_fixo_vazio_e_marcador() {
        assertNull(numeroWhatsApp("234 567 890"))   // telefone fixo: não tem WhatsApp
        assertNull(numeroWhatsApp(""))
        assertNull(numeroWhatsApp(null))
        assertNull(numeroWhatsApp("9xxxxxxxx"))
        assertNull(numeroWhatsApp("91234567"))
    }
}
