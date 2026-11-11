package barberflow.cliente.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatosTest {
    @Test fun preco_em_euros_pt() = assertEquals("12,50 €", formatarPreco(12.5))
    @Test fun preco_inteiro() = assertEquals("8,00 €", formatarPreco(8.0))
    @Test fun duracao_minutos() = assertEquals("30 min", formatarDuracao(30))
    @Test fun duracao_horas_e_minutos() = assertEquals("1 h 15 min", formatarDuracao(75))
    @Test fun duracao_horas_certas() = assertEquals("2 h", formatarDuracao(120))
    @Test fun instagram_arroba_vira_url() = assertEquals("https://instagram.com/andre", urlInstagram("@andre"))
    @Test fun instagram_url_mantem() = assertEquals("https://instagram.com/x", urlInstagram("https://instagram.com/x"))
}
