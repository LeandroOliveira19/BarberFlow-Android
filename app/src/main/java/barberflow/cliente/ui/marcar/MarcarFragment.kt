package barberflow.cliente.ui.marcar

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import barberflow.cliente.dados.api.dto.BarbeariaDto
import barberflow.cliente.dados.api.dto.BarbeiroDto
import barberflow.cliente.dados.api.dto.ServicoDto
import barberflow.cliente.dados.local.MarcacaoLocal
import barberflow.cliente.databinding.FragmentMarcarBinding
import barberflow.cliente.tarefas.Eventos
import barberflow.cliente.tarefas.LISBOA
import barberflow.cliente.tarefas.dataCurta
import barberflow.cliente.tarefas.horaCurta
import barberflow.cliente.ui.MainActivity
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import barberflow.cliente.ui.equipa.carregarFoto
import barberflow.cliente.ui.formatarDuracao
import barberflow.cliente.ui.formatarPreco
import barberflow.cliente.ui.mostrarErro
import barberflow.cliente.tarefas.paraEpochMillis
import barberflow.cliente.ui.quandoVisivel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Passo 1 da marcação: Spinner de serviço e de barbeiro, DatePickerDialog para o dia
 * e RecyclerView em grelha com os horários livres (calculados pelo servidor).
 */
class MarcarFragment : Fragment(R.layout.fragment_marcar) {
    private var _b: FragmentMarcarBinding? = null
    private val b get() = _b!!

    private var barbearia: BarbeariaDto? = null
    private var servicos: List<ServicoDto> = emptyList()
    private var barbeiros: List<BarbeiroDto> = emptyList()
    private var dia: LocalDate? = null
    private var hora: String? = null
    private var slotsJob: Job? = null
    private var chaveCarregada: String? = null
    private var diasAdapter: DiasAdapter? = null

    /** Modo remarcar: a marcação confirmada que se está a mudar (null = marcação nova). */
    private var remarcando: MarcacaoLocal? = null
    private var remarcarId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { s ->
            s.getString("dia")?.let { dia = LocalDate.parse(it) }
            hora = s.getString("hora")
            remarcarId = s.getInt("remarcar_id", -1)
        }
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        dia?.let { out.putString("dia", it.toString()) }
        hora?.let { out.putString("hora", it) }
        out.putInt("remarcar_id", remarcando?.id ?: remarcarId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentMarcarBinding.bind(view)
        b.listaSlots.layoutManager = GridLayoutManager(requireContext(), resources.getInteger(R.integer.colunas_slots))
        b.listaDias.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.botaoContinuar.setOnClickListener { continuar() }
        b.botaoDesistir.setOnClickListener { sairDeRemarcar(irParaMarcacoes = true) }
        chaveCarregada = null
        // Vindo do botão Remarcar (Marcações): entra em modo remarcar com o dia e a hora por escolher
        Repositorio.paraRemarcar?.let { m ->
            Repositorio.paraRemarcar = null
            remarcando = m; remarcarId = m.id; dia = null; hora = null
        }
        if (remarcarId <= 0 && retomarPendente()) return
        carregar()
    }

    private fun mostrarModo() {
        val m = remarcando
        b.cartaoRemarcar.isVisible = m != null
        b.spinnerServico.isEnabled = m == null   // ao remarcar, o serviço fica o mesmo
        b.botaoContinuar.setText(if (m != null) R.string.remarcar_confirmar else R.string.marcar_continuar)
        if (m != null) b.avisoRemarcar.text = getString(R.string.remarcar_aviso, m.servicoNome, dataCurta(m.inicio), horaCurta(m.inicio))
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(if (m != null) R.string.remarcar_titulo else R.string.nav_marcar)
    }

    private fun sairDeRemarcar(irParaMarcacoes: Boolean) {
        remarcando = null; remarcarId = -1; dia = null; hora = null
        if (_b == null) return
        b.textoDia.setText(R.string.marcar_escolher_dia)
        diasAdapter?.selecionar(null)
        b.listaSlots.adapter = null
        mostrarModo()
        atualizarContinuar()
        if (irParaMarcacoes) (activity as? MainActivity)?.irPara(R.id.marcacoesFragment)
    }

    /**
     * Se há uma marcação criada mas ainda por confirmar (ex.: a app foi fechada a meio), volta ao ecrã do código.
     * Devolve true se navegou.
     */
    private fun retomarPendente(): Boolean {
        val p = Repositorio.prefs
        val id = p.pendenteMarcacaoId
        val expira = p.pendenteCodigoExpiraEm
        if (id == -1 || p.pendenteToken == null || expira == null) return false
        if (paraEpochMillis(expira) <= System.currentTimeMillis()) { p.limparPendente(); return false }
        quandoVisivel { findNavController().navigate(R.id.verificarFragment, bundleOf("marcacao_id" to id)) }
        return true
    }

    private fun carregar() {
        val slug = Repositorio.prefs.slug ?: return
        b.progresso.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            val rb = async { Repositorio.barbeariaAtual() }
            val rs = async { Repositorio.servicos(slug) }
            val re = async { Repositorio.equipa(slug) }
            val (b1, s1, e1) = Triple(rb.await(), rs.await(), re.await())
            // Depois de rodar o ecrã só sobra o id: volta a ler a marcação do Room
            if (remarcando == null && remarcarId > 0) {
                remarcando = Repositorio.dao.obter(remarcarId)?.takeIf { it.servicoId != null }
                if (remarcando == null) remarcarId = -1
            }
            if (_b == null) return@launch
            mostrarModo()
            b.progresso.isVisible = false
            val erro = listOf(b1, s1, e1).filterIsInstance<Resultado.Erro>().firstOrNull()
            if (erro != null) { mostrarErro(erro.mensagem) { carregar() }; return@launch }
            barbearia = (b1 as Resultado.Ok).dados
            servicos = (s1 as Resultado.Ok).dados
            barbeiros = (e1 as Resultado.Ok).dados
            preencherSpinners()
            montarCarrossel()
            atualizarBotaoDia()
            if (dia != null) carregarSlots()
        }
    }

    private fun preencherSpinners() {
        val ctx = requireContext()
        b.spinnerServico.adapter = ArrayAdapter(
            ctx, android.R.layout.simple_spinner_dropdown_item,
            servicos.map { "${it.nome} · ${formatarDuracao(it.duracaoMinutos)} · ${formatarPreco(it.preco)}" },
        )
        b.spinnerBarbeiro.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, barbeiros.map { it.nome })

        // Pré-seleção vinda de Serviços ou do detalhe do barbeiro (revisão #4: chega pelo tab, não por argumentos)
        val svPre = remarcando?.servicoId ?: Repositorio.preSelecaoServico ?: arguments?.getInt("servico_id", -1)
        val bbPre = remarcando?.barbeiroId ?: Repositorio.preSelecaoBarbeiro ?: arguments?.getInt("barbeiro_id", -1)
        Repositorio.preSelecaoServico = null
        Repositorio.preSelecaoBarbeiro = null
        servicos.indexOfFirst { it.id == svPre }.takeIf { it >= 0 }?.let { b.spinnerServico.setSelection(it) }
        barbeiros.indexOfFirst { it.id == bbPre }.takeIf { it >= 0 }?.let { b.spinnerBarbeiro.setSelection(it) }

        val aoMudar = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (p === b.spinnerBarbeiro) carregarFoto(b.fotoBarbeiro, barbeiros.getOrNull(pos)?.fotoUrl)
                // Revisão #6: o Spinner também dispara no primeiro layout — só recarrega se a escolha mudou
                if (dia != null && chaveAtual() != chaveCarregada) carregarSlots()
            }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }
        b.spinnerServico.onItemSelectedListener = aoMudar
        b.spinnerBarbeiro.onItemSelectedListener = aoMudar
        carregarFoto(b.fotoBarbeiro, barbeiros.getOrNull(b.spinnerBarbeiro.selectedItemPosition)?.fotoUrl)
    }

    /** Carrossel com todos os dias em que se pode marcar (de hoje até max_dias); os fechados ficam desligados. */
    private fun montarCarrossel() {
        val bb = barbearia ?: return
        val adapter = DiasAdapter(
            diasCarrossel(LocalDate.now(LISBOA), bb.maxDias, bb.diasFuncionamento), dia,
            aoEscolher = { escolherData(it) },
            aoOutraData = { escolherDia() },
        )
        diasAdapter = adapter
        b.listaDias.adapter = adapter
        adapter.selecionar(dia).takeIf { it >= 0 }?.let { b.listaDias.scrollToPosition(it) }
    }

    private fun escolherData(escolhido: LocalDate) {
        dia = escolhido
        hora = null
        diasAdapter?.selecionar(escolhido)?.takeIf { it >= 0 }?.let { b.listaDias.smoothScrollToPosition(it) }
        atualizarBotaoDia()
        carregarSlots()
    }

    /** "Outra data" no fim do carrossel: o DatePickerDialog, limitado aos dias em que se pode marcar. */
    private fun escolherDia() {
        val bb = barbearia ?: return
        val hoje = LocalDate.now(LISBOA)
        val atual = dia ?: hoje
        val dlg = DatePickerDialog(requireContext(), { _, a, m, d ->
            val escolhido = LocalDate.of(a, m + 1, d)
            if (escolhido.dayOfWeek.value !in bb.diasFuncionamento) {
                mostrarErro(getString(R.string.marcar_fechado))
                return@DatePickerDialog
            }
            escolherData(escolhido)
        }, atual.year, atual.monthValue - 1, atual.dayOfMonth)
        dlg.datePicker.minDate = hoje.atStartOfDay(LISBOA).toInstant().toEpochMilli()
        dlg.datePicker.maxDate = hoje.plusDays(bb.maxDias.toLong()).atStartOfDay(LISBOA).toInstant().toEpochMilli()
        dlg.show()
    }

    private fun atualizarBotaoDia() {
        val d = dia ?: return
        b.textoDia.text = d.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-PT")))
            .replaceFirstChar { it.uppercase() }
    }

    /** Identifica o pedido de horários (serviço, barbeiro, dia) para não repetir nem misturar respostas. */
    private fun chaveAtual(): String? {
        val s = servicos.getOrNull(b.spinnerServico.selectedItemPosition) ?: return null
        val x = barbeiros.getOrNull(b.spinnerBarbeiro.selectedItemPosition) ?: return null
        return "${s.id}|${x.id}|${dia ?: return null}"
    }

    private fun carregarSlots() {
        val slug = Repositorio.prefs.slug ?: return
        val s = servicos.getOrNull(b.spinnerServico.selectedItemPosition) ?: return
        val x = barbeiros.getOrNull(b.spinnerBarbeiro.selectedItemPosition) ?: return
        val d = dia ?: return
        chaveCarregada = chaveAtual()
        slotsJob?.cancel()   // revisão #6: só a resposta do último pedido conta
        b.progresso.isVisible = true
        b.listaSlots.adapter = null
        b.semHorarios.isVisible = false
        atualizarContinuar()
        slotsJob = viewLifecycleOwner.lifecycleScope.launch {
            val r = Repositorio.disponibilidade(slug, s.id, x.id, d.toString(), remarcando)
            if (_b == null) return@launch
            b.progresso.isVisible = false
            when (r) {
                is Resultado.Ok -> {
                    if (hora !in r.dados.slots) hora = null
                    b.semHorarios.setText(R.string.marcar_sem_horarios)
                    b.semHorarios.isVisible = r.dados.slots.isEmpty()
                    val slots = SlotsAdapter(r.dados.slots, hora) { h -> hora = h; atualizarContinuar() }
                    slots.configurarGrelha(b.listaSlots.layoutManager as GridLayoutManager)
                    b.listaSlots.adapter = slots
                    atualizarContinuar()
                }
                is Resultado.Erro -> mostrarErro(r.mensagem) { carregarSlots() }
            }
        }
    }

    private fun atualizarContinuar() {
        if (_b != null) b.botaoContinuar.isEnabled = hora != null && dia != null
    }

    private fun continuar() {
        val s = servicos.getOrNull(b.spinnerServico.selectedItemPosition) ?: return
        val x = barbeiros.getOrNull(b.spinnerBarbeiro.selectedItemPosition) ?: return
        val d = dia ?: return
        val h = hora ?: return
        remarcando?.let { m -> confirmarRemarcacao(m, x.id, d, h); return }
        findNavController().navigate(
            R.id.action_marcar_para_dados,
            bundleOf(
                "servico_id" to s.id, "servico_nome" to s.nome, "duracao" to s.duracaoMinutos,
                "barbeiro_id" to x.id, "barbeiro_nome" to x.nome,
                "data" to d.toString(), "hora" to h,
            ),
        )
    }

    /** Modo remarcar: muda a mesma marcação (sem dados do cliente nem código) e volta às Marcações. */
    private fun confirmarRemarcacao(m: MarcacaoLocal, barbeiroId: Int, d: LocalDate, h: String) {
        val ctx = requireContext().applicationContext
        b.botaoContinuar.isEnabled = false
        // No scope da app: rodar o ecrã a meio não deixa o Room nem o lembrete por atualizar
        Repositorio.escopo.launch {
            val r = Repositorio.remarcar(m, barbeiroId, d.toString(), h)
            Eventos.emitir(ctx, Eventos.MARCACAO)
            if (_b == null) return@launch
            when (r) {
                is Resultado.Ok -> {
                    Snackbar.make(b.root, getString(R.string.alterar_remarcada, dataCurta(r.dados.inicio), horaCurta(r.dados.inicio)),
                        Snackbar.LENGTH_LONG).show()
                    sairDeRemarcar(irParaMarcacoes = true)
                }
                is Resultado.Erro -> {
                    mostrarErro(r.mensagem)
                    when (r.codigo) {
                        // Já não dá para alterar: sincroniza e volta à lista
                        "fora_do_prazo", "estado_invalido" -> { Repositorio.sincronizar(); sairDeRemarcar(irParaMarcacoes = true) }
                        "slot_ocupado" -> { hora = null; carregarSlots() }
                        else -> atualizarContinuar()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        diasAdapter = null
        _b = null
        super.onDestroyView()
    }
}

