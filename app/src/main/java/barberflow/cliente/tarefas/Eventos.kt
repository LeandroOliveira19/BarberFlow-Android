package barberflow.cliente.tarefas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * Broadcast local da app: quando algo muda (sessão, marcação nova, código expirado, sincronização)
 * emite-se um evento e os Fragments abertos atualizam-se sozinhos (BroadcastReceiver registado em runtime).
 */
object Eventos {
    const val ACAO = "barberflow.cliente.EVENTO"
    const val EXTRA_TIPO = "tipo"

    const val SESSAO = "sessao"
    const val MARCACAO = "marcacao"
    const val CODIGO_EXPIRADO = "codigo_expirado"
    const val SINCRONIZADO = "sincronizado"

    fun emitir(ctx: Context, tipo: String) {
        ctx.sendBroadcast(Intent(ACAO).setPackage(ctx.packageName).putExtra(EXTRA_TIPO, tipo))
    }

    fun registar(ctx: Context, aoReceber: (String) -> Unit): BroadcastReceiver {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { aoReceber(i.getStringExtra(EXTRA_TIPO).orEmpty()) }
        }
        ContextCompat.registerReceiver(ctx, r, IntentFilter(ACAO), ContextCompat.RECEIVER_NOT_EXPORTED)
        return r
    }

    fun remover(ctx: Context, r: BroadcastReceiver?) {
        if (r == null) return
        try { ctx.unregisterReceiver(r) } catch (_: IllegalArgumentException) { }
    }
}
