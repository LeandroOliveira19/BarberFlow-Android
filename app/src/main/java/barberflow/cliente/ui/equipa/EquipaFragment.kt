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
import barberflow.cliente.ui.aparecer
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
                    b.lista.adapter = EquipaAdapter(r.dados) { barbeiro, foto -> abrirDetalhe(barbeiro, foto) }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemBarbeiroBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val x = lista[pos]
        h.b.nome.text = x.nome
        h.b.especialidades.text = x.especialidades.orEmpty()
        h.b.foto.transitionName = "foto_${x.id}"
        carregarFoto(h.b.foto, x.fotoUrl)
        h.b.root.setOnClickListener { aoTocar(x, h.b.foto) }
    }

    override fun getItemCount() = lista.size
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
