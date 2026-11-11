package barberflow.cliente.ui.servicos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import barberflow.cliente.dados.api.dto.ServicoDto
import barberflow.cliente.databinding.FragmentListaBinding
import barberflow.cliente.databinding.ItemServicoBinding
import barberflow.cliente.ui.aparecer
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
                    b.lista.adapter = ServicosAdapter(r.dados) { s ->
                        // Tocar num serviço abre o Marcar com esse serviço já escolhido
                        findNavController().navigate(R.id.marcarFragment, bundleOf("servico_id" to s.id))
                    }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemServicoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val s = lista[pos]
        val ctx = h.itemView.context
        h.b.nome.text = s.nome
        h.b.descricao.text = s.descricao.orEmpty()
        h.b.descricao.isVisible = !s.descricao.isNullOrBlank()
        h.b.duracaoPreco.text = ctx.getString(R.string.servico_duracao_preco, formatarDuracao(s.duracaoMinutos), formatarPreco(s.preco))
        h.b.root.setOnClickListener { aoTocar(s) }
    }

    override fun getItemCount() = lista.size
}
