package barberflow.cliente.dados

import barberflow.cliente.dados.local.MarcacaoLocal
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testes que fixam os achados da revisão final do Android. */
class RevisaoTest {

    /** Revisão #5: só uma marcação confirmada mantém o lembrete. */
    @Test fun lembrete_mantido_so_se_confirmada() {
        assertFalse(lembreteDeveSerCancelado("confirmada"))
        assertTrue(lembreteDeveSerCancelado("cancelada"))
        assertTrue(lembreteDeveSerCancelado("concluida"))
        assertTrue(lembreteDeveSerCancelado("expirada"))
    }

    /** Revisão #2: o resumo pendente pode ter sido apagado pelo alarme de expiração — nunca rebentar. */
    @Test fun resumo_nulo_nao_rebenta() {
        assertNull(resumoConfirmado(null, "tok"))
        assertNull(resumoConfirmado("", "tok"))
        assertNull(resumoConfirmado("lixo{", "tok"))
    }

    @Test fun resumo_valido_fica_confirmado_com_token() {
        val json = Gson().toJson(MarcacaoLocal(7, "", "rlbarber", "Corte", "André", "2026-11-13 10:00:00", "2026-11-13 10:30:00", "pendente_verificacao"))
        val m = resumoConfirmado(json, "abc")!!
        assertEquals("abc", m.token)
        assertEquals("confirmada", m.estado)
        assertEquals(7, m.id)
    }
}
