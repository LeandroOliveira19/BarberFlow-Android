package barberflow.cliente.tarefas

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import barberflow.cliente.dados.local.MarcacaoLocal

/**
 * AlarmManager: lembrete exato antes da marcação e alarme único para a expiração do código.
 * Se o sistema não permitir alarmes exatos, usa setWindow (janela de 10 min).
 */
object Lembretes {
    private const val PEDIDO_EXPIRACAO = -1
    const val EXTRA_ID = "id"
    const val EXTRA_SERVICO = "servico"
    const val EXTRA_BARBEIRO = "barbeiro"
    const val EXTRA_INICIO = "inicio"
    const val EXTRA_MINUTOS = "minutos"

    private fun am(ctx: Context) = ctx.getSystemService(AlarmManager::class.java)

    fun podeAlarmeExato(ctx: Context): Boolean = Build.VERSION.SDK_INT < 31 || am(ctx).canScheduleExactAlarms()

    private fun piLembrete(ctx: Context, id: Int, intent: Intent? = null): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, id, intent ?: Intent(ctx, LembreteReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Agenda o lembrete `minutosAntes` da hora da marcação. Devolve false se essa hora já passou. */
    fun agendarLembrete(ctx: Context, m: MarcacaoLocal, minutosAntes: Int): Boolean {
        val t = instanteLembrete(m.inicio, minutosAntes)
        if (t <= System.currentTimeMillis()) return false
        val intent = Intent(ctx, LembreteReceiver::class.java)
            .putExtra(EXTRA_ID, m.id)
            .putExtra(EXTRA_SERVICO, m.servicoNome)
            .putExtra(EXTRA_BARBEIRO, m.barbeiroNome)
            .putExtra(EXTRA_INICIO, m.inicio)
            .putExtra(EXTRA_MINUTOS, minutosAntes)
        val pi = piLembrete(ctx, m.id, intent)
        if (podeAlarmeExato(ctx)) {
            am(ctx).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        } else {
            am(ctx).setWindow(AlarmManager.RTC_WAKEUP, t, 10 * 60_000L, pi)
        }
        return true
    }

    fun cancelarLembrete(ctx: Context, id: Int) = am(ctx).cancel(piLembrete(ctx, id))

    private fun piExpiracao(ctx: Context, id: Int = -1): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, PEDIDO_EXPIRACAO,
            Intent(ctx, CodigoExpiradoReceiver::class.java).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Alarme de execução única na hora em que o código expira (valor vindo da API). */
    fun agendarExpiracao(ctx: Context, marcacaoId: Int, expiraEm: String) {
        am(ctx).set(AlarmManager.RTC_WAKEUP, paraEpochMillis(expiraEm), piExpiracao(ctx, marcacaoId))
    }

    fun cancelarExpiracao(ctx: Context) = am(ctx).cancel(piExpiracao(ctx))
}
