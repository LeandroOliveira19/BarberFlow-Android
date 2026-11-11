package barberflow.cliente.ui.inicio

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import barberflow.cliente.dados.api.dto.BarbeariaDto
import barberflow.cliente.databinding.FragmentInicioBinding
import barberflow.cliente.tarefas.Eventos
import barberflow.cliente.tarefas.Notificacoes
import barberflow.cliente.ui.OnboardingActivity
import barberflow.cliente.ui.abrir
import barberflow.cliente.ui.mostrarErro
import kotlinx.coroutines.launch

/** Tab Início: dados e contactos da barbearia, preferência de lembrete e reviews do Google (só leitura). */
class InicioFragment : Fragment(R.layout.fragment_inicio) {
    private var _b: FragmentInicioBinding? = null
    private val b get() = _b!!
    private var receiver: BroadcastReceiver? = null
    private val prefs get() = Repositorio.prefs

    private val pedirPermissao = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        atualizarAvisoNotificacoes()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentInicioBinding.bind(view)
        b.listaReviews.layoutManager = LinearLayoutManager(requireContext())
        b.nomeBarbearia.text = prefs.barbeariaNome
        configurarSpinnerLembrete()
        b.botaoMudarBarbearia.setOnClickListener { mudarBarbearia() }
        b.botaoAtivarNotificacoes.setOnClickListener { ativarNotificacoes() }
        atualizarSaudacao()
        carregar()
    }

    override fun onStart() {
        super.onStart()
        // BroadcastReceiver em runtime: a saudação muda logo que a sessão muda noutro ecrã
        receiver = Eventos.registar(requireContext()) { tipo -> if (tipo == Eventos.SESSAO) atualizarSaudacao() }
        atualizarAvisoNotificacoes()
    }

    override fun onStop() {
        Eventos.remover(requireContext(), receiver)
        receiver = null
        super.onStop()
    }

    private fun carregar() {
        b.progresso.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            when (val r = Repositorio.barbeariaAtual()) {
                is Resultado.Ok -> mostrarBarbearia(r.dados)
                is Resultado.Erro -> mostrarErro(r.mensagem) { carregar() }
            }
            val slug = prefs.slug ?: return@launch
            val rv = Repositorio.reviews(slug)
            if (_b == null) return@launch
            b.progresso.isVisible = false
            val lista = (rv as? Resultado.Ok)?.dados.orEmpty()
            b.semReviews.isVisible = lista.isEmpty()
            b.listaReviews.adapter = ReviewsAdapter(lista)
        }
    }

    private fun mostrarBarbearia(d: BarbeariaDto) {
        if (_b == null) return
        b.nomeBarbearia.text = d.nome
        b.morada.text = d.morada.orEmpty()
        b.morada.isVisible = !d.morada.isNullOrBlank()
        if (d.horaAbertura != null && d.horaFecho != null) {
            b.horario.text = getString(R.string.inicio_horario, rotuloDias(d.diasFuncionamento), d.horaAbertura, d.horaFecho)
        }
        // Intents implícitos para as apps do sistema
        b.botaoLigar.isVisible = !d.telemovelContacto.isNullOrBlank()
        b.botaoLigar.setOnClickListener {
            requireContext().abrir(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + d.telemovelContacto)))
        }
        b.botaoEmail.isVisible = !d.emailContacto.isNullOrBlank()
        b.botaoEmail.setOnClickListener {
            requireContext().abrir(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + d.emailContacto)))
        }
        b.botaoMapa.isVisible = !d.morada.isNullOrBlank()
        b.botaoMapa.setOnClickListener {
            requireContext().abrir(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(d.nome + ", " + d.morada))))
        }
    }

    private fun rotuloDias(dias: List<Int>): String {
        val nomes = resources.getStringArray(R.array.dias_iso)
        val ord = dias.sorted()
        if (ord.isEmpty()) return ""
        val consecutivos = ord.last() - ord.first() == ord.size - 1
        return if (consecutivos && ord.size >= 3) "${nomes[ord.first() - 1]}–${nomes[ord.last() - 1]}"
        else ord.joinToString(", ") { nomes[it - 1] }
    }

    /** Spinner com onItemSelectedListener: guarda a antecedência do lembrete em SharedPreferences. */
    private fun configurarSpinnerLembrete() {
        val minutos = resources.getIntArray(R.array.lembrete_minutos)
        b.spinnerLembrete.adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.lembrete_opcoes, android.R.layout.simple_spinner_dropdown_item,
        )
        b.spinnerLembrete.setSelection(minutos.indexOf(prefs.lembreteMinutos).coerceAtLeast(0))
        b.spinnerLembrete.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                prefs.lembreteMinutos = minutos[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }
    }

    private fun atualizarSaudacao() {
        if (_b == null) return
        val nome = prefs.clienteNome
        b.saudacao.isVisible = prefs.sessaoAtiva && !nome.isNullOrBlank()
        b.saudacao.text = getString(R.string.inicio_ola, nome?.substringBefore(' ').orEmpty())
    }

    private fun atualizarAvisoNotificacoes() {
        if (_b == null) return
        b.avisoNotificacoes.isVisible = prefs.sessaoAtiva && !Notificacoes.podeNotificar(requireContext())
    }

    private fun ativarNotificacoes() {
        if (Build.VERSION.SDK_INT >= 33 && !Notificacoes.temPermissao(requireContext()) &&
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Já recusada antes (ou canal desligado): só nas definições do sistema
            requireContext().abrir(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            )
        }
    }

    private fun mudarBarbearia() {
        Repositorio.esquecerBarbearia()
        startActivity(
            Intent(requireContext(), OnboardingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}
