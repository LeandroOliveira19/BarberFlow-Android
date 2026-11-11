package barberflow.cliente

import android.app.Application
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.tarefas.Notificacoes
import barberflow.cliente.tarefas.SincronizarWorker

class BarberFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repositorio.iniciar(this)
        Notificacoes.criarCanal(this)
        if (Repositorio.prefs.sessaoAtiva) SincronizarWorker.agendar(this)
    }
}
