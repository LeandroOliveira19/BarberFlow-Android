package barberflow.cliente.tarefas

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** As datas da API são "yyyy-MM-dd HH:mm:ss" na hora de Lisboa, seja qual for o fuso do telemóvel. */
val LISBOA: ZoneId = ZoneId.of("Europe/Lisbon")
private val FORMATO_API: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun paraEpochMillis(dataHoraApi: String): Long =
    LocalDateTime.parse(dataHoraApi, FORMATO_API).atZone(LISBOA).toInstant().toEpochMilli()

fun instanteLembrete(inicio: String, minutosAntes: Int): Long =
    paraEpochMillis(inicio) - minutosAntes * 60_000L

/** "agora" no formato da API (para comparar com as datas guardadas no Room). */
fun agoraApi(): String = ZonedDateTime.now(LISBOA).format(FORMATO_API)

fun horaCurta(dataHoraApi: String): String = dataHoraApi.substring(11, 16)

/** "2026-11-12 10:00:00" → "12/11/2026" */
fun dataCurta(dataHoraApi: String): String =
    LocalDateTime.parse(dataHoraApi, FORMATO_API).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
