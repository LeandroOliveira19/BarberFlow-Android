package barberflow.cliente.tarefas

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import java.util.concurrent.TimeUnit

/**
 * WorkManager: de 6 em 6 horas (com rede) pergunta à API o estado das marcações.
 * Se a barbearia cancelou uma marcação, avisa o cliente e cancela o lembrete.
 */
class SincronizarWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        Repositorio.iniciar(applicationContext)
        if (!Repositorio.prefs.sessaoAtiva) return Result.success()
        return when (val r = Repositorio.sincronizar()) {
            is Resultado.Erro -> Result.retry()
            is Resultado.Ok -> {
                for (m in r.dados) {
                    // os lembretes já foram cancelados dentro de Repositorio.sincronizar()
                    if (m.estado == "cancelada") {
                        Notificacoes.mostrar(
                            applicationContext, 800_000 + m.id,
                            applicationContext.getString(R.string.notif_cancelada_titulo),
                            applicationContext.getString(R.string.notif_cancelada_texto, dataCurta(m.inicio) + " " + horaCurta(m.inicio)),
                            R.id.marcacoesFragment,
                        )
                    }
                }
                Eventos.emitir(applicationContext, Eventos.SINCRONIZADO)
                Result.success()
            }
        }
    }

    companion object {
        const val NOME = "sincronizar"

        fun agendar(ctx: Context) {
            val pedido = PeriodicWorkRequestBuilder<SincronizarWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(NOME, ExistingPeriodicWorkPolicy.KEEP, pedido)
        }

        fun cancelar(ctx: Context) = WorkManager.getInstance(ctx).cancelUniqueWork(NOME)
    }
}
