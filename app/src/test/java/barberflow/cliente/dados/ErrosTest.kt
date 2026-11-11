package barberflow.cliente.dados

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrosTest {
    @Test fun erro_api_le_mensagem_e_tentativas() {
        val e = lerErro(422, """{"erro":{"codigo":"codigo_errado","mensagem":"Código incorreto.","tentativas_restantes":3}}""")
        assertEquals("codigo_errado", e.codigo)
        assertEquals("Código incorreto.", e.mensagem)
        assertEquals(3, e.tentativasRestantes)
    }

    /** Review Focus 3: o IIS/PHP pode devolver HTML — nunca rebentar. */
    @Test fun erro_html_nao_rebenta() {
        val e = lerErro(500, "<html><body>IIS 500</body></html>")
        assertNull(e.codigo)
        assertTrue(e.mensagem.contains("500"))
    }

    @Test fun erro_corpo_vazio_nao_rebenta() {
        assertNull(lerErro(502, null).codigo)
        assertNull(lerErro(502, "").codigo)
    }

    @Test fun erro_json_sem_campo_erro() {
        assertNull(lerErro(404, """{"outra":"coisa"}""").codigo)
    }
}
