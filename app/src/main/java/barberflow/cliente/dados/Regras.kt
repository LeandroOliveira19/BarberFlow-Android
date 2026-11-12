package barberflow.cliente.dados

import barberflow.cliente.dados.local.MarcacaoLocal
import barberflow.cliente.tarefas.paraEpochMillis
import com.google.gson.Gson

/**
 * Barbearia a usar: num APK próprio de uma barbearia (BuildConfig.SLUG_FIXO) é sempre essa;
 * na app genérica é a que o cliente escolheu (null = ainda tem de escolher).
 */
fun slugEfetivo(slugFixo: String, slugGuardado: String?): String? =
    slugFixo.trim().ifEmpty { null } ?: slugGuardado?.trim()?.ifEmpty { null }

/**
 * O cliente pode cancelar/remarcar se a marcação estiver confirmada e começar daqui a pelo menos `horas`.
 * Serve só para mostrar os botões: o servidor volta a validar. Datas no formato da API (hora de Lisboa).
 */
fun podeAlterar(estado: String, inicio: String, agora: String, horas: Int): Boolean {
    if (estado != "confirmada") return false
    return try {
        paraEpochMillis(inicio) - paraEpochMillis(agora) >= horas * 3_600_000L
    } catch (_: Exception) {
        false
    }
}

/** Remarcar precisa também do serviço e do barbeiro (marcações antigas só os têm depois de sincronizar). */
fun podeRemarcar(m: MarcacaoLocal, agora: String, horas: Int): Boolean =
    podeAlterar(m.estado, m.inicio, agora, horas) && m.servicoId != null && m.barbeiroId != null

/** Só uma marcação confirmada mantém o lembrete; cancelada/concluída/expirada já não precisam dele. */
fun lembreteDeveSerCancelado(estado: String): Boolean = estado != "confirmada"

/**
 * Constrói a marcação confirmada a partir do resumo guardado nas prefs e do token da API.
 * Devolve null se o resumo não existir ou estiver corrompido (ex.: apagado pelo alarme de expiração).
 */
fun resumoConfirmado(json: String?, token: String): MarcacaoLocal? {
    if (json.isNullOrBlank()) return null
    return try {
        Gson().fromJson(json, MarcacaoLocal::class.java)?.copy(token = token, estado = "confirmada")
    } catch (_: Exception) {
        null
    }
}
