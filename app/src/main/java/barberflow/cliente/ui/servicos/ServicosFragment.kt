package barberflow.cliente.ui.servicos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import barberflow.cliente.ui.MainActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import barberflow.cliente.dados.api.dto.ServicoDto
import barberflow.cliente.databinding.FragmentListaBinding
import barberflow.cliente.databinding.ItemServicoBinding
import androidx.recyclerview.widget.ConcatAdapter
import barberflow.cliente.ui.CabecalhoAdapter
import barberflow.cliente.ui.TipoServico
import barberflow.cliente.ui.animarEntrada
import barberflow.cliente.ui.aparecer
import barberflow.cliente.ui.tipoServico
import barberflow.cliente.ui.formatarDuracao
import barberflow.cliente.ui.formatarPreco
import barberflow.cliente.ui.mostrarErro
import kotlinx.coroutines.launch

/** Tab Serviços: RecyclerView + Adapter com o catálogo vindo da API (Retrofit GET). */
class ServicosFragment : Fragment(R.layout.fragment_lista) {
    private var _b: FragmentListaBinding? = null
    private val b get() = _b!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentListaBinding.bind(view)
        b.lista.layoutManager = LinearLayoutManager(requireContext())
        carregar()
    }

    private fun carregar() {
        val slug = Repositorio.prefs.slug ?: return
        b.progresso.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            val r = Repositorio.servicos(slug)
            if (_b == null) return@launch
            b.progresso.isVisible = false
            when (r) {
                is Resultado.Ok -> {
                    b.vazio.setText(R.string.servicos_vazio)
                    b.vazio.isVisible = r.dados.isEmpty()
                    val servicos = ServicosAdapter(r.dados) { s ->
                        // Tocar num serviço (ou em "Marcar") abre o tab Marcar com esse serviço já escolhido
                        Repositorio.preSelecaoServico = s.id
                        (activity as? MainActivity)?.irPara(R.id.marcarFragment)
                    }
                    b.lista.adapter = ConcatAdapter(CabecalhoAdapter(R.string.servicos_subtitulo), servicos)
                    b.lista.aparecer()
                }
                is Resultado.Erro -> mostrarErro(r.mensagem) { carregar() }
            }
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}

class ServicosAdapter(
    private val lista: List<ServicoDto>,
    private val aoTocar: (ServicoDto) -> Unit,
) : RecyclerView.Adapter<ServicosAdapter.ViewHolder>() {

    class ViewHolder(val b: ItemServicoBinding) : RecyclerView.ViewHolder(b.root)

    /** Só os cards que aparecem pela primeira vez entram com animação (ao voltar a fazer scroll não se repete). */
    private var ultimaAnimada = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemServicoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val s = lista[pos]
        h.b.nome.text = s.nome
        h.b.descricao.text = s.descricao.orEmpty()
        h.b.descricao.isVisible = !s.descricao.isNullOrBlank()
        h.b.duracao.text = formatarDuracao(s.duracaoMinutos)
        h.b.preco.text = formatarPreco(s.preco)
        h.b.icone.setImageResource(iconeServico(tipoServico(s.nome)))
        h.b.root.setOnClickListener { aoTocar(s) }
        h.b.botaoMarcar.setOnClickListener { aoTocar(s) }
        h.b.botaoMarcar.contentDescription = h.itemView.context.getString(R.string.servico_marcar) + " " + s.nome
        if (pos > ultimaAnimada) {
            ultimaAnimada = pos
            h.itemView.animarEntrada(pos)
        } else {
            h.itemView.alpha = 1f
            h.itemView.translationY = 0f
        }
    }

    /** Ícone de cada tipo de serviço (o tipo vem do nome, em tipoServico). */
    private fun iconeServico(tipo: TipoServico) = when (tipo) {
        TipoServico.CORTE -> R.drawable.ic_servicos
        TipoServico.BARBA -> R.drawable.ic_barba
        TipoServico.CORTE_E_BARBA -> R.drawable.ic_corte_barba
        TipoServico.TRATAMENTO -> R.drawable.ic_tratamento
        TipoServico.OUTRO -> R.drawable.ic_servicos
    }

    override fun getItemCount() = lista.size
}
