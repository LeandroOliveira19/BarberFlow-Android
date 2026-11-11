package barberflow.cliente.tarefas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/* BroadcastReceivers ESTÁTICOS (declarados no AndroidManifest.xml). */

/** Disparado pelo AlarmManager antes da marcação. */
class LembreteReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val id = i.getIntExtra(Lembretes.EXTRA_ID, 0)
        val minutos = i.getIntExtra(Lembretes.EXTRA_MINUTOS, 60)
        val inicio = i.getStringExtra(Lembretes.EXTRA_INICIO) ?: return
        Notificacoes.mostrar(
            ctx, id,
            ctx.getString(R.string.notif_lembrete_titulo, minutos),
            ctx.getString(R.string.notif_lembrete_texto, i.getStringExtra(Lembretes.EXTRA_SERVICO), i.getStringExtra(Lembretes.EXTRA_BARBEIRO), horaCurta(inicio)),
            R.id.marcacoesFragment,
        )
    }
}

/** Alarme único: o código de verificação expirou sem ser confirmado. */
class CodigoExpiradoReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        Repositorio.iniciar(ctx)
        val prefs = Repositorio.prefs
        val id = i.getIntExtra(Lembretes.EXTRA_ID, -1)
        if (id == -1 || prefs.pendenteMarcacaoId != id) return
        prefs.limparPendente()
        Notificacoes.mostrar(ctx, 900_000 + id, ctx.getString(R.string.notif_expirou_titulo), ctx.getString(R.string.notif_expirou_texto), R.id.marcarFragment)
        Eventos.emitir(ctx, Eventos.CODIGO_EXPIRADO)
    }
}

/** Os alarmes perdem-se ao reiniciar o telemóvel: voltamos a agendá-los a partir do Room. */
class ArrancoReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        if (i.action != Intent.ACTION_BOOT_COMPLETED) return
        Repositorio.iniciar(ctx)
        val fim = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val minutos = Repositorio.prefs.lembreteMinutos
                Repositorio.futurasConfirmadas().forEach { Lembretes.agendarLembrete(ctx, it, minutos) }
            } finally {
                fim.finish()
            }
        }
    }
}
