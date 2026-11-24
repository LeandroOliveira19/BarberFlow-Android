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
import androidx.appcompat.app.AppCompatActivity
import barberflow.cliente.BuildConfig
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import barberflow.cliente.dados.api.dto.BarbeariaDto
import barberflow.cliente.dados.diasAte
import barberflow.cliente.dados.proximaMarcacao
import barberflow.cliente.databinding.FragmentInicioBinding
import barberflow.cliente.tarefas.Eventos
import barberflow.cliente.tarefas.Notificacoes
import barberflow.cliente.tarefas.agoraApi
import barberflow.cliente.tarefas.dataCurta
import barberflow.cliente.tarefas.horaCurta
import barberflow.cliente.ui.MainActivity
import barberflow.cliente.ui.OnboardingActivity
import barberflow.cliente.ui.abrir
import barberflow.cliente.ui.animarPalavras
import barberflow.cliente.ui.diaEMes
import barberflow.cliente.ui.mostrarErro
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        // Nome da barbearia em Cinzel, a entrar palavra a palavra (como o topo do site)
        b.nomeBarbearia.animarPalavras(prefs.barbeariaNome.orEmpty())
        configurarSpinnerLembrete()
        // Num APK próprio de uma barbearia não faz sentido mudar de barbearia
        b.botaoMudarBarbearia.isVisible = BuildConfig.SLUG_FIXO.isBlank()
        b.botaoMudarBarbearia.setOnClickListener { mudarBarbearia() }
        b.botaoAtivarNotificacoes.setOnClickListener { ativarNotificacoes() }
        b.cartaoProxima.setOnClickListener { (activity as? MainActivity)?.irPara(R.id.marcacoesFragment) }
        atualizarSaudacao()
        carregar()
    }

    override fun onStart() {
        super.onStart()
        // BroadcastReceiver em runtime: a saudação muda logo que a sessão muda noutro ecrã
        receiver = Eventos.registar(requireContext()) { tipo ->
            if (tipo == Eventos.SESSAO) atualizarSaudacao()
            mostrarProxima()
        }
        atualizarAvisoNotificacoes()
        mostrarProxima()
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
        prefs.barbeariaNome = d.nome
        (activity as? AppCompatActivity)?.supportActionBar?.title = d.nome
        if (b.nomeBarbearia.text.toString() != d.nome) b.nomeBarbearia.animarPalavras(d.nome)
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

    /** Cartão "A tua próxima marcação": hoje, amanhã ou a data, com o serviço e o barbeiro. */
    private fun mostrarProxima() {
        val slug = prefs.slug ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val agora = agoraApi()
            val m = proximaMarcacao(Repositorio.marcacoesLocais(slug), agora)
            if (_b == null) return@launch
            b.cartaoProxima.isVisible = m != null
            if (m == null) return@launch
            val (dia, mes) = diaEMes(m.inicio, resources.configuration.locales[0])
            b.proximaDia.text = dia
            b.proximaMes.text = mes
            val hora = horaCurta(m.inicio)
            b.proximaQuando.text = when (diasAte(m.inicio, agora)) {
                0L -> getString(R.string.inicio_proxima_hoje, hora)
                1L -> getString(R.string.inicio_proxima_amanha, hora)
                in 2L..6L -> getString(R.string.inicio_proxima_dia, diaDaSemana(m.inicio), hora)
                else -> getString(R.string.inicio_proxima_dia, dataCurta(m.inicio), hora)
            }
            b.proximaServico.text = getString(R.string.marcacao_com, m.servicoNome, m.barbeiroNome)
        }
    }

    /** "2026-11-26 09:15:00" → "Quinta-feira" (na língua do telemóvel). */
    private fun diaDaSemana(inicio: String): String {
        val locale = resources.configuration.locales[0]
        return LocalDate.parse(inicio.take(10)).format(DateTimeFormatter.ofPattern("EEEE", locale))
            .replaceFirstChar { it.titlecase(locale) }
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
        val texto = getString(R.string.inicio_ola, nome?.substringBefore(' ').orEmpty())
        if (b.saudacao.isVisible && b.saudacao.text.toString() != texto) b.saudacao.animarPalavras(texto) else b.saudacao.text = texto
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
