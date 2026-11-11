package barberflow.cliente.dados

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import barberflow.cliente.dados.local.BaseDados
import barberflow.cliente.dados.local.MarcacaoLocal
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarcacaoDaoTest {
    private lateinit var bd: BaseDados

    @Before fun criar() {
        bd = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, BaseDados::class.java).build()
    }

    @After fun fechar() = bd.close()

    private fun m(id: Int, slug: String = "rlbarber", estado: String = "confirmada", inicio: String = "2026-12-01 10:00:00") =
        MarcacaoLocal(id, "tok$id", slug, "Corte", "André", inicio, inicio, estado)

    @Test fun upsert_substitui_pelo_id() = runBlocking {
        val dao = bd.marcacoes()
        dao.upsert(m(1, estado = "pendente_verificacao"))
        dao.upsert(m(1, estado = "confirmada"))
        assertEquals(1, dao.todas().size)
        assertEquals("confirmada", dao.todas()[0].estado)
    }

    /** Review Focus 4: mudar de barbearia mostra só as marcações do slug atual. */
    @Test fun listar_filtra_por_slug() = runBlocking {
        val dao = bd.marcacoes()
        dao.upsert(m(1)); dao.upsert(m(2)); dao.upsert(m(3, slug = "outra"))
        assertEquals(2, dao.listarPorSlug("rlbarber").size)
        assertEquals(1, dao.listarPorSlug("outra").size)
    }

    @Test fun futuras_exclui_pendentes_e_passadas() = runBlocking {
        val dao = bd.marcacoes()
        dao.upsert(m(1, inicio = "2025-01-01 10:00:00"))
        dao.upsert(m(2, estado = "pendente_verificacao"))
        dao.upsert(m(3))
        assertEquals(listOf(3), dao.futurasConfirmadas("2026-01-01 00:00:00").map { it.id })
    }
}
