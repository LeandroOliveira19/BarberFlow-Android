package barberflow.cliente.ui.marcar

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import barberflow.cliente.dados.api.dto.BarbeariaDto
import barberflow.cliente.dados.api.dto.BarbeiroDto
import barberflow.cliente.dados.api.dto.ServicoDto
import barberflow.cliente.databinding.FragmentMarcarBinding
import barberflow.cliente.databinding.ItemSlotBinding
import barberflow.cliente.tarefas.LISBOA
import barberflow.cliente.ui.equipa.carregarFoto
import barberflow.cliente.ui.formatarDuracao
import barberflow.cliente.ui.formatarPreco
import barberflow.cliente.ui.mostrarErro
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { s ->
            s.getString("dia")?.let { dia = LocalDate.parse(it) }
            hora = s.getString("hora")
        }
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        dia?.let { out.putString("dia", it.toString()) }
        hora?.let { out.putString("hora", it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentMarcarBinding.bind(view)
        b.listaSlots.layoutManager = GridLayoutManager(requireContext(), resources.getInteger(R.integer.colunas_slots))
        b.botaoDia.setOnClickListener { escolherDia() }
        b.botaoContinuar.setOnClickListener { continuar() }
        carregar()
    }

    private fun carregar() {
        val slug = Repositorio.prefs.slug ?: return
        b.progresso.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            val rb = async { Repositorio.barbeariaAtual() }
            val rs = async { Repositorio.servicos(slug) }
            val re = async { Repositorio.equipa(slug) }
            val (b1, s1, e1) = Triple(rb.await(), rs.await(), re.await())
            if (_b == null) return@launch
            b.progresso.isVisible = false
            val erro = listOf(b1, s1, e1).filterIsInstance<Resultado.Erro>().firstOrNull()
            if (erro != null) { mostrarErro(erro.mensagem) { carregar() }; return@launch }
            barbearia = (b1 as Resultado.Ok).dados
            servicos = (s1 as Resultado.Ok).dados
            barbeiros = (e1 as Resultado.Ok).dados
            preencherSpinners()
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

        // Pré-seleção vinda de Serviços ou do detalhe do barbeiro
        val args = arguments
        servicos.indexOfFirst { it.id == args?.getInt("servico_id", -1) }.takeIf { it >= 0 }?.let { b.spinnerServico.setSelection(it) }
        barbeiros.indexOfFirst { it.id == args?.getInt("barbeiro_id", -1) }.takeIf { it >= 0 }?.let { b.spinnerBarbeiro.setSelection(it) }

        val aoMudar = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (p === b.spinnerBarbeiro) carregarFoto(b.fotoBarbeiro, barbeiros.getOrNull(pos)?.fotoUrl)
                if (dia != null) carregarSlots()
            }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }
        b.spinnerServico.onItemSelectedListener = aoMudar
        b.spinnerBarbeiro.onItemSelectedListener = aoMudar
        carregarFoto(b.fotoBarbeiro, barbeiros.getOrNull(b.spinnerBarbeiro.selectedItemPosition)?.fotoUrl)
    }

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
            dia = escolhido
            hora = null
            atualizarBotaoDia()
            carregarSlots()
        }, atual.year, atual.monthValue - 1, atual.dayOfMonth)
        dlg.datePicker.minDate = hoje.atStartOfDay(LISBOA).toInstant().toEpochMilli()
        dlg.datePicker.maxDate = hoje.plusDays(bb.maxDias.toLong()).atStartOfDay(LISBOA).toInstant().toEpochMilli()
        dlg.show()
    }

    private fun atualizarBotaoDia() {
        val d = dia ?: return
        b.botaoDia.text = d.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-PT")))
            .replaceFirstChar { it.uppercase() }
    }

    private fun carregarSlots() {
        val slug = Repositorio.prefs.slug ?: return
        val s = servicos.getOrNull(b.spinnerServico.selectedItemPosition) ?: return
        val x = barbeiros.getOrNull(b.spinnerBarbeiro.selectedItemPosition) ?: return
        val d = dia ?: return
        b.progresso.isVisible = true
        b.listaSlots.adapter = null
        b.semHorarios.isVisible = false
        atualizarContinuar()
        viewLifecycleOwner.lifecycleScope.launch {
            val r = Repositorio.disponibilidade(slug, s.id, x.id, d.toString())
            if (_b == null) return@launch
            b.progresso.isVisible = false
            when (r) {
                is Resultado.Ok -> {
                    if (hora !in r.dados.slots) hora = null
                    b.semHorarios.setText(R.string.marcar_sem_horarios)
                    b.semHorarios.isVisible = r.dados.slots.isEmpty()
                    b.listaSlots.adapter = SlotsAdapter(r.dados.slots, hora) { h -> hora = h; atualizarContinuar() }
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
        findNavController().navigate(
            R.id.action_marcar_para_dados,
            bundleOf(
                "servico_id" to s.id, "servico_nome" to s.nome, "duracao" to s.duracaoMinutos,
                "barbeiro_id" to x.id, "barbeiro_nome" to x.nome,
                "data" to d.toString(), "hora" to h,
            ),
        )
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}

/** Grelha de horários; o selecionado fica dourado e "pulsa" (ObjectAnimator). */
class SlotsAdapter(
    private val slots: List<String>,
    private var selecionado: String?,
    private val aoEscolher: (String) -> Unit,
) : RecyclerView.Adapter<SlotsAdapter.ViewHolder>() {

    class ViewHolder(val b: ItemSlotBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val hora = slots[pos]
        val ativo = hora == selecionado
        h.b.hora.text = hora
        h.b.hora.isSelected = ativo
        h.b.hora.setTextColor(h.itemView.context.getColor(if (ativo) R.color.bf_preto else R.color.bf_texto))
        h.b.root.setOnClickListener {
            val anterior = slots.indexOf(selecionado)
            selecionado = hora
            if (anterior >= 0) notifyItemChanged(anterior)
            notifyItemChanged(h.bindingAdapterPosition)
            pulsar(h.b.root)
            aoEscolher(hora)
        }
    }

    private fun pulsar(v: View) {
        ObjectAnimator.ofPropertyValuesHolder(
            v,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f, 1f),
        ).setDuration(220).start()
    }

    override fun getItemCount() = slots.size
}
