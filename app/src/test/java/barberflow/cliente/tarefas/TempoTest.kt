package barberflow.cliente.tarefas

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

/** Review Focus 1: o dispositivo pode estar noutro fuso — as horas da API são sempre de Lisboa. */
class TempoTest {
    private lateinit var original: TimeZone

    @Before fun fusoUtc() { original = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("UTC")) }
    @After fun repor() = TimeZone.setDefault(original)

    @Test fun verao_e_utc_mais_1() =
        assertEquals(Instant.parse("2026-07-15T09:00:00Z").toEpochMilli(), paraEpochMillis("2026-07-15 10:00:00"))

    @Test fun inverno_e_utc() =
        assertEquals(Instant.parse("2026-12-15T10:00:00Z").toEpochMilli(), paraEpochMillis("2026-12-15 10:00:00"))

    @Test fun lembrete_60_min_antes() =
        assertEquals(Instant.parse("2026-07-15T08:00:00Z").toEpochMilli(), instanteLembrete("2026-07-15 10:00:00", 60))

    @Test fun formato_hora_curta() = assertEquals("10:00", horaCurta("2026-07-15 10:00:00"))
}
