package barberflow.cliente.dados

import barberflow.cliente.dados.api.dto.BarbeariaDto
import barberflow.cliente.dados.api.dto.MarcacaoDto
import barberflow.cliente.dados.local.MarcacaoLocal
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cancelar e remarcar na app: regra do prazo (só para mostrar os botões; o servidor volta a validar). */
class AlterarTest {
    private val agora = "2026-11-12 10:00:00"

    @Test fun no_limite_exato_ainda_pode() {
        assertTrue(podeAlterar("confirmada", "2026-11-13 10:00:00", agora, 24))
    }

    @Test fun um_minuto_depois_do_limite_ja_nao_pode() {
        assertFalse(podeAlterar("confirmada", "2026-11-13 09:59:00", agora, 24))
    }

    @Test fun so_confirmadas() {
        assertFalse(podeAlterar("pendente_verificacao", "2026-11-20 10:00:00", agora, 24))
        assertFalse(podeAlterar("cancelada", "2026-11-20 10:00:00", agora, 24))
        assertFalse(podeAlterar("concluida", "2026-11-20 10:00:00", agora, 24))
    }

    @Test fun antecedencia_zero_permite_ate_ao_inicio() {
        assertTrue(podeAlterar("confirmada", "2026-11-12 10:30:00", agora, 0))
        assertFalse(podeAlterar("confirmada", "2026-11-12 09:30:00", agora, 0))
    }

    @Test fun data_mal_formada_nao_rebenta() {
        assertFalse(podeAlterar("confirmada", "lixo", agora, 24))
    }

    @Test fun remarcar_exige_os_ids_vindos_da_sincronizacao() {
        val m = MarcacaoLocal(1, "tok", "rlbarber", "Corte", "André", "2026-11-20 10:00:00", "2026-11-20 10:30:00", "confirmada")
        assertFalse(podeRemarcar(m, agora, 24))
        assertTrue(podeRemarcar(m.copy(servicoId = 5, barbeiroId = 2), agora, 24))
    }

    @Test fun marcacao_dto_le_os_ids_novos() {
        val json = """{"id":1,"estado":"confirmada","inicio":"2026-11-20 10:00:00","fim":"2026-11-20 10:30:00",
            "servico_id":5,"servico_nome":"Corte","barbeiro_id":2,"barbeiro_nome":"André",
            "barbearia_slug":"rlbarber","barbearia_nome":"RLBarber","para_nome":null}"""
        val dto = Gson().fromJson(json, MarcacaoDto::class.java)
        assertEquals(5, dto.servicoId)
        assertEquals(2, dto.barbeiroId)
    }

    /** Revisão final #3: a resposta do remarcar perdeu-se → a sincronização tem de trazer o novo horário. */
    @Test fun sincronizacao_deteta_horario_mudado_no_servidor() {
        val local = MarcacaoLocal(1, "tok", "rlbarber", "Corte", "André", "2026-11-20 10:00:00", "2026-11-20 10:30:00", "confirmada",
            servicoId = 5, barbeiroId = 2)
        val igual = MarcacaoDto(1, "confirmada", "2026-11-20 10:00:00", "2026-11-20 10:30:00", "Corte", "André", "rlbarber", "RLBarber",
            servicoId = 5, barbeiroId = 2)
        assertFalse(horarioMudou(local, igual))
        assertTrue(horarioMudou(local, igual.copy(inicio = "2026-11-21 11:00:00", fim = "2026-11-21 11:30:00")))
        assertTrue(horarioMudou(local, igual.copy(barbeiroNome = "Bruno", barbeiroId = 3)))
    }

    @Test fun servidor_antigo_sem_ids_nem_antecedencia() {
        val m = Gson().fromJson("""{"id":1,"estado":"confirmada","inicio":"x","fim":"y","servico_nome":"C","barbeiro_nome":"A",
            "barbearia_slug":"r","barbearia_nome":"R"}""", MarcacaoDto::class.java)
        assertNull(m.servicoId)
        assertNull(m.barbeiroId)
        val b = Gson().fromJson("""{"id":1,"nome":"R","slug":"r","tema_cor":"#000","tema_fundo":"#000",
            "dias_funcionamento":[1],"max_dias":30,"codigo_validade_min":15}""", BarbeariaDto::class.java)
        assertNull(b.antecedenciaAlteracaoHoras)
        assertEquals(24, b.antecedenciaHoras())
    }
}
