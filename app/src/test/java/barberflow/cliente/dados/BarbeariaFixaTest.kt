package barberflow.cliente.dados

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** APK próprio de uma barbearia: o slug vem do build e ganha sempre ao que estiver guardado. */
class BarbeariaFixaTest {
    @Test fun apk_da_barbearia_usa_o_slug_do_build() = assertEquals("rlbarber", slugEfetivo("rlbarber", null))
    @Test fun slug_do_build_ganha_ao_guardado() = assertEquals("rlbarber", slugEfetivo("rlbarber", "outra"))
    @Test fun app_generica_usa_o_guardado() = assertEquals("outra", slugEfetivo("", "outra"))
    @Test fun app_generica_sem_nada_pergunta() = assertNull(slugEfetivo("", null))
    @Test fun espacos_contam_como_vazio() = assertNull(slugEfetivo("  ", null))
}
