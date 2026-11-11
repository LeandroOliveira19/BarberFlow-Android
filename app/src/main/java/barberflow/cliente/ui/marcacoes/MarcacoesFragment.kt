package barberflow.cliente.ui.marcacoes

import android.content.BroadcastReceiver
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import barberflow.cliente.dados.local.MarcacaoLocal
import barberflow.cliente.databinding.FragmentMarcacoesBinding
import barberflow.cliente.databinding.ItemMarcacaoBinding
import barberflow.cliente.tarefas.Agendador
import barberflow.cliente.tarefas.Eventos
import barberflow.cliente.tarefas.dataCurta
import barberflow.cliente.tarefas.horaCurta
import barberflow.cliente.ui.MainActivity
import barberflow.cliente.ui.corEstado
import barberflow.cliente.ui.mostrarErro
import barberflow.cliente.ui.rotuloEstado
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Tab "As minhas marcações": lista do Room (funciona offline), puxar para atualizar pela API,
 * e o cartão de sessão (SharedPreference "sessao_ativa" → ImageView + texto do botão).
 */
class MarcacoesFragment : Fragment(R.layout.fragment_marcacoes) {
    private var _b: FragmentMarcacoesBinding? = null
    private val b get() = _b!!
    private val prefs get() = Repositorio.prefs
    private var receiver: BroadcastReceiver? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentMarcacoesBinding.bind(view)
        b.lista.layoutManager = LinearLayoutManager(requireContext())
        b.refresh.setColorSchemeResources(R.color.bf_dourado)
        b.refresh.setOnRefreshListener { sincronizar() }
        b.botaoSessao.setOnClickListener { alternarSessao() }
    }

    override fun onStart() {
        super.onStart()
        receiver = Eventos.registar(requireContext()) { recarregar() }
        recarregar()
    }

    override fun onStop() {
        Eventos.remover(requireContext(), receiver)
        receiver = null
        super.onStop()
    }

    private fun recarregar() {
        atualizarSessao()
        val slug = prefs.slug ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val lista = Repositorio.marcacoesLocais(slug)
            if (_b == null) return@launch
            b.vazio.isVisible = lista.isEmpty()
            b.lista.adapter = MarcacoesAdapter(lista)
        }
    }

    private fun sincronizar() {
        viewLifecycleOwner.lifecycleScope.launch {
            val r = Repositorio.sincronizar()
            if (_b == null) return@launch
            b.refresh.isRefreshing = false
            if (r is Resultado.Erro) mostrarErro(r.mensagem)
            recarregar()
        }
    }

    private fun atualizarSessao() {
        if (_b == null) return
        if (prefs.sessaoAtiva) {
            b.iconeSessao.setImageResource(R.drawable.ic_sessao_on)
            b.textoSessao.text = getString(R.string.sessao_ativa_texto, prefs.clienteNome.orEmpty())
            b.botaoSessao.setText(R.string.sessao_terminar)
        } else {
            b.iconeSessao.setImageResource(R.drawable.ic_sessao_off)
            b.textoSessao.setText(R.string.sessao_inativa_texto)
            b.botaoSessao.setText(R.string.sessao_identificar)
        }
    }

    private fun alternarSessao() {
        if (!prefs.sessaoAtiva) {
            (activity as? MainActivity)?.irPara(R.id.marcarFragment)
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sessao_confirmar_titulo)
            .setMessage(R.string.sessao_confirmar_texto)
            .setPositiveButton(R.string.sessao_terminar) { _, _ -> terminarSessao() }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun terminarSessao() {
        val ctx = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            Agendador.aoTerminarSessao(ctx)
            Repositorio.dao.apagarTodas()
            prefs.limparCliente()
            prefs.limparPendente()
            Eventos.emitir(ctx, Eventos.SESSAO)   // o Início e este ecrã atualizam-se pelo receiver
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}

class MarcacoesAdapter(private val lista: List<MarcacaoLocal>) : RecyclerView.Adapter<MarcacoesAdapter.ViewHolder>() {

    class ViewHolder(val b: ItemMarcacaoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemMarcacaoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val m = lista[pos]
        val ctx = h.itemView.context
        h.b.quando.text = ctx.getString(R.string.marcacao_quando, dataCurta(m.inicio), horaCurta(m.inicio))
        h.b.servicoBarbeiro.text = ctx.getString(R.string.marcacao_com, m.servicoNome, m.barbeiroNome)
        h.b.estado.text = rotuloEstado(ctx, m.estado)
        val cor = ctx.getColor(corEstado(m.estado))
        h.b.estado.setTextColor(cor)
        (h.b.estado.background.mutate() as? GradientDrawable)?.setStroke((1 * ctx.resources.displayMetrics.density).toInt(), cor)
        h.b.root.alpha = if (m.estado == "cancelada" || m.estado == "expirada") 0.55f else 1f
    }

    override fun getItemCount() = lista.size
}
