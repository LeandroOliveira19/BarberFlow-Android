package barberflow.cliente.ui.equipa

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import barberflow.cliente.dados.api.dto.BarbeiroDto
import barberflow.cliente.databinding.FragmentListaBinding
import barberflow.cliente.databinding.ItemBarbeiroBinding
import android.widget.TextView
import androidx.recyclerview.widget.ConcatAdapter
import barberflow.cliente.ui.CabecalhoAdapter
import barberflow.cliente.ui.animarEntrada
import barberflow.cliente.ui.aparecer
import barberflow.cliente.ui.cabecalhoLarguraToda
import barberflow.cliente.ui.iniciais
import barberflow.cliente.ui.mostrarErro
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlinx.coroutines.launch

/** Tab Equipa: grelha (GridLayoutManager) com as fotos carregadas pelo Glide. */
class EquipaFragment : Fragment(R.layout.fragment_lista) {
    private var _b: FragmentListaBinding? = null
    private val b get() = _b!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentListaBinding.bind(view)
        b.lista.layoutManager = GridLayoutManager(requireContext(), resources.getInteger(R.integer.colunas_equipa))
            .apply { cabecalhoLarguraToda() }
        carregar()
    }

    private fun carregar() {
        val slug = Repositorio.prefs.slug ?: return
        b.progresso.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            val r = Repositorio.equipa(slug)
            if (_b == null) return@launch
            b.progresso.isVisible = false
            when (r) {
                is Resultado.Ok -> {
                    b.vazio.setText(R.string.equipa_vazia)
                    b.vazio.isVisible = r.dados.isEmpty()
                    val equipa = EquipaAdapter(r.dados) { barbeiro, foto -> abrirDetalhe(barbeiro, foto) }
                    b.lista.adapter = ConcatAdapter(CabecalhoAdapter(R.string.equipa_subtitulo), equipa)
                    b.lista.aparecer()
                }
                is Resultado.Erro -> mostrarErro(r.mensagem) { carregar() }
            }
        }
    }

    /** Navega para o detalhe com uma transição de elemento partilhado na foto. */
    private fun abrirDetalhe(barbeiro: BarbeiroDto, foto: ImageView) {
        val extras = FragmentNavigatorExtras(foto to "foto_${barbeiro.id}")
        findNavController().navigate(
            R.id.action_equipa_para_detalhe,
            bundleOf("barbeiro" to Gson().toJson(barbeiro)),
            null,
            extras,
        )
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}

class EquipaAdapter(
    private val lista: List<BarbeiroDto>,
    private val aoTocar: (BarbeiroDto, ImageView) -> Unit,
) : RecyclerView.Adapter<EquipaAdapter.ViewHolder>() {

    class ViewHolder(val b: ItemBarbeiroBinding) : RecyclerView.ViewHolder(b.root)

    private var ultimaAnimada = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemBarbeiroBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val x = lista[pos]
        h.b.nome.text = x.nome
        h.b.especialidades.text = x.especialidades.orEmpty()
        h.b.especialidades.isVisible = !x.especialidades.isNullOrBlank()
        h.b.foto.transitionName = "foto_${x.id}"
        carregarFotoOuIniciais(h.b.foto, h.b.iniciais, x)
        h.b.root.setOnClickListener { aoTocar(x, h.b.foto) }
        if (pos > ultimaAnimada) {
            ultimaAnimada = pos
            h.itemView.animarEntrada(pos)
        } else {
            h.itemView.alpha = 1f
            h.itemView.translationY = 0f
        }
    }

    override fun getItemCount() = lista.size
}

/** Foto do barbeiro pelo Glide; sem foto, as iniciais em Cinzel sobre o degradê dourado do fundo. */
fun carregarFotoOuIniciais(img: ImageView, iniciaisView: TextView, barbeiro: BarbeiroDto) {
    val semFoto = barbeiro.fotoUrl.isNullOrBlank()
    iniciaisView.isVisible = semFoto
    iniciaisView.text = iniciais(barbeiro.nome)
    if (semFoto) {
        Glide.with(img).clear(img)
        img.setImageDrawable(null)
    } else {
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(img).load(barbeiro.fotoUrl).centerCrop().into(img)
    }
}

/** Glide para URLs; sem foto, imagem estática com setImageResource. */
fun carregarFoto(img: ImageView, url: String?) {
    if (url.isNullOrBlank()) {
        Glide.with(img).clear(img)
        img.scaleType = ImageView.ScaleType.CENTER_INSIDE
        img.setImageResource(R.drawable.ic_barbeiro)
    } else {
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(img).load(url).placeholder(R.drawable.ic_barbeiro).error(R.drawable.ic_barbeiro).centerCrop().into(img)
    }
}
