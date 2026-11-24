package barberflow.cliente.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Efeito "palavra a palavra": onde começa e acaba cada palavra e a transparência de cada uma ao longo do tempo. */
class EfeitosTest {

    @Test fun palavras_separadas_por_espacos() {
        assertEquals(listOf(0..2, 4..9, 11..14), intervalosPalavras("Boa tarde, Rui."))
    }

    @Test fun espacos_repetidos_e_nas_pontas_sao_ignorados() {
        assertEquals(listOf(2..4, 8..10), intervalosPalavras("  Olá   Rui  "))
    }

    @Test fun texto_vazio_nao_tem_palavras() {
        assertEquals(emptyList<IntRange>(), intervalosPalavras(""))
        assertEquals(emptyList<IntRange>(), intervalosPalavras("   "))
    }

    @Test fun acentos_e_pontuacao_ficam_na_palavra() {
        assertEquals(listOf(0..5, 7..13), intervalosPalavras("Ótimo, João!!!"))
    }

    @Test fun antes_do_atraso_a_palavra_esta_invisivel() {
        assertEquals(0, alphaPalavra(indice = 2, tempoMs = 100, atrasoMs = 80, duracaoMs = 500))
    }

    @Test fun no_fim_da_duracao_a_palavra_esta_visivel() {
        assertEquals(255, alphaPalavra(indice = 0, tempoMs = 500, atrasoMs = 80, duracaoMs = 500))
        assertEquals(255, alphaPalavra(indice = 3, tempoMs = 10_000, atrasoMs = 80, duracaoMs = 500))
    }

    @Test fun a_meio_da_duracao_a_palavra_esta_a_meio() {
        // palavra 1 começa aos 80 ms; aos 330 ms já passou metade dos 500 ms
        assertEquals(128, alphaPalavra(indice = 1, tempoMs = 330, atrasoMs = 80, duracaoMs = 500))
    }
}
