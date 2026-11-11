package barberflow.cliente.tarefas

import android.content.Context
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.local.MarcacaoLocal

/** Junta, num só sítio, o que acontece em background em cada momento do ciclo de uma marcação. */
object Agendador {
    /** Marcação criada: alarme único para quando o código expirar. */
    fun aoCriar(ctx: Context, id: Int, expiraEm: String?) {
        if (expiraEm != null) Lembretes.agendarExpiracao(ctx, id, expiraEm)
    }

    /** Marcação confirmada: cancela a expiração, agenda o lembrete e a sincronização periódica. */
    fun aoConfirmar(ctx: Context, m: MarcacaoLocal) {
        Lembretes.cancelarExpiracao(ctx)
        Lembretes.agendarLembrete(ctx, m, Repositorio.prefs.lembreteMinutos)
        SincronizarWorker.agendar(ctx)
    }

    /** Sessão terminada: cancela tudo o que estava agendado. */
    suspend fun aoTerminarSessao(ctx: Context) {
        Repositorio.dao.todas().forEach { Lembretes.cancelarLembrete(ctx, it.id) }
        Lembretes.cancelarExpiracao(ctx)
        SincronizarWorker.cancelar(ctx)
    }
}
