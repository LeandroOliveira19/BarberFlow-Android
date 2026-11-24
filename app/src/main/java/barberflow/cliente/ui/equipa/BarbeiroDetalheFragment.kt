package barberflow.cliente.ui.equipa

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.ui.MainActivity
import barberflow.cliente.dados.api.dto.BarbeiroDto
import barberflow.cliente.databinding.FragmentBarbeiroDetalheBinding
import barberflow.cliente.ui.abrir
import barberflow.cliente.ui.urlInstagram
import com.google.android.material.chip.Chip
import com.google.gson.Gson

/** Perfil público do barbeiro (foto, especialidades, bio, Instagram). */
class BarbeiroDetalheFragment : Fragment(R.layout.fragment_barbeiro_detalhe) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentBarbeiroDetalheBinding.bind(view)
        val x = Gson().fromJson(requireArguments().getString("barbeiro"), BarbeiroDto::class.java)
        // O nome já aparece grande por cima da foto: a barra de cima fica só com a seta de voltar
        (activity as? AppCompatActivity)?.supportActionBar?.title = ""

        b.foto.transitionName = "foto_${x.id}"
        carregarFotoOuIniciais(b.foto, b.iniciais, x)
        b.nome.text = x.nome
        // Especialidades em pílulas douradas (separadas por vírgulas no perfil)
        x.especialidades.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { esp ->
            b.chipsEspecialidades.addView(Chip(requireContext(), null, R.attr.chipEspecialidadeStyle).apply { text = esp })
        }
        b.chipsEspecialidades.isVisible = b.chipsEspecialidades.childCount > 0
        b.bio.text = x.bio.orEmpty()
        b.cartaoBio.isVisible = !x.bio.isNullOrBlank()

        b.botaoInstagram.isVisible = !x.instagram.isNullOrBlank()
        b.botaoInstagram.setOnClickListener {
            requireContext().abrir(Intent(Intent.ACTION_VIEW, Uri.parse(urlInstagram(x.instagram!!))))
        }
        b.botaoMarcar.text = getString(R.string.detalhe_marcar_com, x.nome.substringBefore(' '))
        b.botaoMarcar.setOnClickListener {
            // Abre o tab Marcar (e não um Marcar dentro do tab Equipa) com este barbeiro escolhido
            Repositorio.preSelecaoBarbeiro = x.id
            (activity as? MainActivity)?.irPara(R.id.marcarFragment)
        }
    }
}
