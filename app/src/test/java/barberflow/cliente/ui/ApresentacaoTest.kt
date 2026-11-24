package barberflow.cliente.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** Regras de apresentação dos ecrãs: tipo de serviço (ícone), iniciais do barbeiro e bloco de data da marcação. */
class ApresentacaoTest {

    @Test fun tipo_de_servico_pelo_nome() {
        assertEquals(TipoServico.CORTE, tipoServico("Corte Cabelo Normal"))
        assertEquals(TipoServico.CORTE, tipoServico("Corte Cabelo Degradê"))
        assertEquals(TipoServico.BARBA, tipoServico("Barba"))
        assertEquals(TipoServico.BARBA, tipoServico("Barboterapia"))
        assertEquals(TipoServico.CORTE_E_BARBA, tipoServico("Corte Cabelo Degradê + Barba"))
        assertEquals(TipoServico.CORTE_E_BARBA, tipoServico("Corte Cabelo Normal + Barboterapia"))
        assertEquals(TipoServico.TRATAMENTO, tipoServico("Alisamento"))
        assertEquals(TipoServico.TRATAMENTO, tipoServico("Coloração"))
        assertEquals(TipoServico.OUTRO, tipoServico("Pacote Noivo"))
    }

    @Test fun tipo_de_servico_ignora_maiusculas_e_acentos() {
        assertEquals(TipoServico.CORTE_E_BARBA, tipoServico("CORTE + BARBA"))
        assertEquals(TipoServico.TRATAMENTO, tipoServico("coloracao"))
    }

    @Test fun iniciais_do_nome() {
        assertEquals("LP", iniciais("Luis Paulo"))
        assertEquals("AC", iniciais("André Pinto Costa"))
        assertEquals("R", iniciais("Rui"))
        assertEquals("É", iniciais("  élio  "))
        assertEquals("", iniciais("   "))
    }

    @Test fun bloco_de_data_da_marcacao() {
        assertEquals("28" to "NOV", diaEMes("2026-11-28 14:30:00", Locale("pt", "PT")))
        assertEquals("5" to "JAN", diaEMes("2027-01-05 09:00:00", Locale.ENGLISH))
    }

    @Test fun bloco_de_data_com_texto_invalido() {
        assertEquals("" to "", diaEMes("ontem", Locale("pt", "PT")))
    }
}
