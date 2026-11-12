package barberflow.cliente.dados

import android.content.Context
import barberflow.cliente.dados.api.ApiCliente
import barberflow.cliente.dados.api.ApiServico
import barberflow.cliente.dados.api.dto.BarbeariaDto
import barberflow.cliente.dados.api.dto.BarbeiroDto
import barberflow.cliente.dados.api.dto.CodigoPedido
import barberflow.cliente.dados.api.dto.DisponibilidadeDto
import barberflow.cliente.dados.api.dto.NovaMarcacaoPedido
import barberflow.cliente.dados.api.dto.NovaMarcacaoResposta
import barberflow.cliente.dados.api.dto.ReenvioPedido
import barberflow.cliente.dados.api.dto.ReenvioResposta
import barberflow.cliente.dados.api.dto.RemarcarPedido
import barberflow.cliente.dados.api.dto.TokenPedido
import barberflow.cliente.dados.api.dto.ReviewDto
import barberflow.cliente.dados.api.dto.ServicoDto
import barberflow.cliente.dados.api.dto.VerificacaoResposta
import barberflow.cliente.dados.local.BaseDados
import barberflow.cliente.dados.local.MarcacaoDao
import barberflow.cliente.dados.local.MarcacaoLocal
import barberflow.cliente.tarefas.Lembretes
import barberflow.cliente.tarefas.agoraApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Ponto único de acesso a dados: junta a API (Retrofit), o Room e as SharedPreferences.
 * Todas as funções devolvem Resultado, para os ecrãs só terem de mostrar dados ou a mensagem de erro.
 */
object Repositorio {
    private lateinit var app: Context
    private val api: ApiServico get() = ApiCliente.servico
    lateinit var prefs: Preferencias private set
    lateinit var dao: MarcacaoDao private set

    /**
     * Scope da app para pedidos que MUDAM dados no servidor (criar/verificar marcação, terminar sessão):
     * rodar o ecrã ou mudar de tab não os cancela a meio, e o resultado é guardado antes de tocar na UI.
     */
    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Pré-seleção para o tab Marcar quando se vem de Serviços ou do detalhe de um barbeiro. */
    var preSelecaoServico: Int? = null
    var preSelecaoBarbeiro: Int? = null

    /** Marcação a remarcar: passada de Marcações para o tab Marcar (mesmo padrão da pré-seleção). */
    var paraRemarcar: MarcacaoLocal? = null

    fun iniciar(ctx: Context) {
        if (::app.isInitialized) return
        app = ctx.applicationContext
        prefs = Preferencias(app)
        dao = BaseDados.obter(app).marcacoes()
    }

    private suspend fun <T> pedido(bloco: suspend () -> retrofit2.Response<T>): Resultado<T> =
        if (!temInternet(app)) Resultado.Erro("Sem ligação à internet.") else chamar(bloco)

    suspend fun barbearia(slug: String): Resultado<BarbeariaDto> = pedido { api.barbearia("barbearias/$slug") }

    /** Dados da barbearia escolhida, guardados em memória enquanto a app está aberta. */
    private var barbeariaCache: BarbeariaDto? = null

    suspend fun barbeariaAtual(): Resultado<BarbeariaDto> {
        val slug = prefs.slug ?: return Resultado.Erro("Escolhe uma barbearia.")
        barbeariaCache?.takeIf { it.slug == slug }?.let { return Resultado.Ok(it) }
        return barbearia(slug).also { if (it is Resultado.Ok) barbeariaCache = it.dados }
    }

    fun esquecerBarbearia() { barbeariaCache = null; prefs.limparBarbearia() }
    suspend fun servicos(slug: String): Resultado<List<ServicoDto>> = pedido { api.servicos("barbearias/$slug/servicos") }
    suspend fun equipa(slug: String): Resultado<List<BarbeiroDto>> = pedido { api.equipa("barbearias/$slug/equipa") }
    suspend fun reviews(slug: String): Resultado<List<ReviewDto>> = pedido { api.reviews("barbearias/$slug/reviews") }

    suspend fun disponibilidade(slug: String, servicoId: Int, barbeiroId: Int, data: String): Resultado<DisponibilidadeDto> =
        pedido { api.disponibilidade("barbearias/$slug/disponibilidade", servicoId, barbeiroId, data) }

    suspend fun criarMarcacao(slug: String, p: NovaMarcacaoPedido): Resultado<NovaMarcacaoResposta> =
        pedido { api.criarMarcacao("barbearias/$slug/marcacoes", p) }

    suspend fun verificar(id: Int, codigo: String, tokenPendente: String): Resultado<VerificacaoResposta> =
        pedido { api.verificar("marcacoes/$id/verificar", CodigoPedido(codigo, tokenPendente)) }

    suspend fun reenviar(id: Int, tokenPendente: String): Resultado<ReenvioResposta> =
        pedido { api.reenviar("marcacoes/$id/reenviar", ReenvioPedido(tokenPendente)) }

    suspend fun guardarConfirmada(m: MarcacaoLocal) = dao.upsert(m)

    suspend fun marcacoesLocais(slug: String): List<MarcacaoLocal> = dao.listarPorSlug(slug)

    /**
     * Pergunta à API o estado atual de todas as marcações guardadas e atualiza o Room.
     * Devolve as que MUDARAM de estado (ex.: cancelada pela barbearia).
     */
    suspend fun sincronizar(): Resultado<List<MarcacaoLocal>> {
        val locais = dao.todas()
        if (locais.isEmpty()) return Resultado.Ok(emptyList())
        val tokens = locais.joinToString(",") { "${it.id}:${it.token}" }
        return when (val r = pedido { api.marcacoes("marcacoes", tokens) }) {
            is Resultado.Erro -> r
            is Resultado.Ok -> {
                val porId = r.dados.associateBy { it.id }
                val mudadas = mutableListOf<MarcacaoLocal>()
                for (l in locais) {
                    val remoto = porId[l.id] ?: continue
                    // v3: marcações guardadas antes de existir o serviço/barbeiro ficam com os ids (para remarcar)
                    if (remoto.servicoId != null && (l.servicoId != remoto.servicoId || l.barbeiroId != remoto.barbeiroId)) {
                        dao.atualizarIds(l.id, remoto.servicoId, remoto.barbeiroId)
                    }
                    if (remoto.estado != l.estado) {
                        dao.atualizarEstado(l.id, remoto.estado)
                        // Revisão #5: quem atualiza o estado também cancela o lembrete (pull-to-refresh ou worker)
                        if (lembreteDeveSerCancelado(remoto.estado)) Lembretes.cancelarLembrete(app, l.id)
                        mudadas += l.copy(estado = remoto.estado)
                    }
                }
                Resultado.Ok(mudadas)
            }
        }
    }

    suspend fun futurasConfirmadas(): List<MarcacaoLocal> = dao.futurasConfirmadas(agoraApi())

    /** Cancela na API; em sucesso grava o estado e cancela o lembrete. */
    suspend fun cancelar(m: MarcacaoLocal): Resultado<MarcacaoLocal> =
        when (val r = pedido { api.cancelar("marcacoes/${m.id}/cancelar", TokenPedido(m.token)) }) {
            is Resultado.Erro -> r.also(::aoErroDeAlteracao)
            is Resultado.Ok -> {
                dao.atualizarEstado(m.id, r.dados.estado)
                Lembretes.cancelarLembrete(app, m.id)
                Resultado.Ok(m.copy(estado = r.dados.estado))
            }
        }

    /** O prazo da barbearia mudou entretanto: esquece os dados em memória para a lista voltar a ler o prazo. */
    private fun aoErroDeAlteracao(e: Resultado.Erro) {
        if (e.codigo == "fora_do_prazo") barbeariaCache = null
    }

    /** Remarca na API (mesmo serviço); em sucesso grava o novo horário e reagenda o lembrete. */
    suspend fun remarcar(m: MarcacaoLocal, barbeiroId: Int, data: String, hora: String): Resultado<MarcacaoLocal> =
        when (val r = pedido { api.remarcar("marcacoes/${m.id}/remarcar", RemarcarPedido(m.token, barbeiroId, data, hora)) }) {
            is Resultado.Erro -> r.also(::aoErroDeAlteracao)
            is Resultado.Ok -> {
                val d = r.dados
                val nova = m.copy(inicio = d.inicio, fim = d.fim, barbeiroId = d.barbeiroId ?: barbeiroId,
                    barbeiroNome = d.barbeiroNome, estado = d.estado)
                dao.atualizarHorario(m.id, nova.inicio, nova.fim, nova.barbeiroId ?: barbeiroId, nova.barbeiroNome)
                Lembretes.cancelarLembrete(app, m.id)
                Lembretes.agendarLembrete(app, nova, prefs.lembreteMinutos)
                Resultado.Ok(nova)
            }
        }
}
