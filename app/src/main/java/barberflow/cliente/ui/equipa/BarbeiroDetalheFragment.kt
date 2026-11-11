package barberflow.cliente.ui.equipa

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import barberflow.cliente.R
import barberflow.cliente.dados.api.dto.BarbeiroDto
import barberflow.cliente.databinding.FragmentBarbeiroDetalheBinding
import barberflow.cliente.ui.abrir
import barberflow.cliente.ui.urlInstagram
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
        (activity as? AppCompatActivity)?.supportActionBar?.title = x.nome

        b.foto.transitionName = "foto_${x.id}"
        carregarFoto(b.foto, x.fotoUrl)
        b.nome.text = x.nome
        b.especialidades.text = x.especialidades.orEmpty()
        b.tituloEspecialidades.isVisible = !x.especialidades.isNullOrBlank()
        b.especialidades.isVisible = !x.especialidades.isNullOrBlank()
        b.bio.text = x.bio.orEmpty()
        b.tituloBio.isVisible = !x.bio.isNullOrBlank()
        b.bio.isVisible = !x.bio.isNullOrBlank()

        b.botaoInstagram.isVisible = !x.instagram.isNullOrBlank()
        b.botaoInstagram.setOnClickListener {
            requireContext().abrir(Intent(Intent.ACTION_VIEW, Uri.parse(urlInstagram(x.instagram!!))))
        }
        b.botaoMarcar.text = getString(R.string.detalhe_marcar_com, x.nome.substringBefore(' '))
        b.botaoMarcar.setOnClickListener {
            findNavController().navigate(R.id.marcarFragment, bundleOf("barbeiro_id" to x.id))
        }
    }
}
