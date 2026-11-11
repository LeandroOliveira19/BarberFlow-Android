package barberflow.cliente.ui.marcar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacaoTest {
    @Test fun telemovel_com_espacos() = assertTrue(telemovelValido("912 345 678"))
    @Test fun telemovel_com_351() = assertTrue(telemovelValido("+351912345678"))
    @Test fun telemovel_com_00351() = assertTrue(telemovelValido("00351 912345678"))
    @Test fun telemovel_fixo() = assertTrue(telemovelValido("234123456"))
    @Test fun telemovel_comeca_por_8() = assertFalse(telemovelValido("812345678"))
    @Test fun telemovel_curto() = assertFalse(telemovelValido("91234"))
    @Test fun email_valido() = assertTrue(emailValido("a@b.pt"))
    @Test fun email_sem_dominio() = assertFalse(emailValido("a@b"))
    @Test fun email_com_espaco() = assertFalse(emailValido("a b@c.pt"))
    @Test fun nome_curto() = assertFalse(nomeValido(" A "))
    @Test fun nome_ok() = assertTrue(nomeValido("Ana"))
    @Test fun para_nome_vazio_e_o_proprio() = assertEquals(null, paraNomeEfetivo("   ", "Ana"))
    @Test fun para_nome_igual_ao_proprio_e_o_proprio() = assertEquals(null, paraNomeEfetivo(" ana ", "Ana"))
    @Test fun para_nome_normaliza_espacos() = assertEquals("Tiago Silva", paraNomeEfetivo("  Tiago   Silva ", "Ana"))
}
