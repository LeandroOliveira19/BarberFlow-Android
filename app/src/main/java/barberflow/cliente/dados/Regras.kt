package barberflow.cliente.dados

import barberflow.cliente.dados.local.MarcacaoLocal
import com.google.gson.Gson

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
