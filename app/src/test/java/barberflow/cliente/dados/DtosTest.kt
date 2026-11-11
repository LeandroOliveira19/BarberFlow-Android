package barberflow.cliente.dados

import barberflow.cliente.dados.api.dto.BarbeariaDto
import barberflow.cliente.dados.api.dto.BarbeiroDto
import barberflow.cliente.dados.api.dto.MarcacaoDto
import barberflow.cliente.dados.api.dto.NovaMarcacaoPedido
import barberflow.cliente.dados.api.dto.NovaMarcacaoResposta
import barberflow.cliente.dados.api.dto.ServicoDto
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** JSON copiado das respostas reais da API v1 (snake_case → camelCase via @SerializedName). */
class DtosTest {
    private val gson = Gson()

    @Test fun servico_mapeia_snake_case() {
        val s = gson.fromJson("""{"id":5,"nome":"Corte","descricao":null,"duracao_minutos":30,"preco":12.5}""", ServicoDto::class.java)
        assertEquals(30, s.duracaoMinutos)
        assertEquals(12.5, s.preco, 0.001)
        assertNull(s.descricao)
    }

    @Test fun barbearia_real() {
        val json = """{"id":1,"nome":"RLBarber","slug":"rlbarber","morada":"Aveiro, Portugal","email_contacto":"geral@rlbarber.pt",
            "telemovel_contacto":"9xxxxxxxx","tema_cor":"#cda47c","tema_fundo":"#0a0a0a","dias_funcionamento":[1,2,3,4,5,6],
            "hora_abertura":"09:00","hora_fecho":"20:00","max_dias":30,"codigo_validade_min":15}"""
        val b = gson.fromJson(json, BarbeariaDto::class.java)
        assertEquals("geral@rlbarber.pt", b.emailContacto)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), b.diasFuncionamento)
        assertEquals(30, b.maxDias)
        assertEquals(15, b.codigoValidadeMin)
    }

    @Test fun barbeiro_real() {
        val b = gson.fromJson("""{"id":2,"nome":"André Costa","foto_url":null,"bio":"Bio","especialidades":"Degradês e Barba","instagram":"https://instagram.com/andre_barber"}""", BarbeiroDto::class.java)
        assertEquals("Degradês e Barba", b.especialidades)
        assertNull(b.fotoUrl)
    }

    @Test fun nova_marcacao_resposta_real() {
        val r = gson.fromJson("""{"marcacao_id":53,"estado":"pendente_verificacao","codigo_expira_em":"2026-11-11 14:47:22","email_enviado":false,"token_pendente":"ab","codigo_dev":"503745"}""", NovaMarcacaoResposta::class.java)
        assertEquals(53, r.marcacaoId)
        assertEquals("ab", r.tokenPendente)
        assertEquals("503745", r.codigoDev)
    }

    @Test fun marcacao_real() {
        val m = gson.fromJson("""{"id":7,"estado":"confirmada","inicio":"2026-11-12 10:00:00","fim":"2026-11-12 10:30:00","servico_nome":"Corte","barbeiro_nome":"André","barbearia_slug":"rlbarber","barbearia_nome":"RLBarber"}""", MarcacaoDto::class.java)
        assertEquals("Corte", m.servicoNome)
        assertEquals("rlbarber", m.barbeariaSlug)
    }

    @Test fun pedido_serializa_em_snake_case() {
        val json = gson.toJson(NovaMarcacaoPedido(5, 2, "2026-11-12", "10:00", "Ana", "912345678", "a@b.pt"))
        assertTrue(json.contains("\"servico_id\":5"))
        assertTrue(json.contains("\"barbeiro_id\":2"))
    }
}
