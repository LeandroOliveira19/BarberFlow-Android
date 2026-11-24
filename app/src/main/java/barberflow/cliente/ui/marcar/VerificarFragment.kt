package barberflow.cliente.ui.marcar

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.CalendarContract
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import barberflow.cliente.dados.local.MarcacaoLocal
import barberflow.cliente.dados.resumoConfirmado
import barberflow.cliente.databinding.FragmentVerificarBinding
import barberflow.cliente.tarefas.Agendador
import barberflow.cliente.tarefas.Eventos
import barberflow.cliente.tarefas.Lembretes
import barberflow.cliente.tarefas.Notificacoes
import barberflow.cliente.tarefas.dataCurta
import barberflow.cliente.tarefas.horaCurta
import barberflow.cliente.tarefas.paraEpochMillis
import barberflow.cliente.ui.MainActivity
import barberflow.cliente.ui.abrir
import barberflow.cliente.ui.mostrarErro
import barberflow.cliente.ui.quandoVisivel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/** Passo 3: o cliente escreve o código recebido por email; a API devolve o token da marcação. */
class VerificarFragment : Fragment(R.layout.fragment_verificar) {
    private var _b: FragmentVerificarBinding? = null
    private val b get() = _b!!
    private val prefs get() = Repositorio.prefs
    private var receiver: BroadcastReceiver? = null
    private var contador: CountDownTimer? = null
    private var marcacaoId = -1

    /** Permissão POST_NOTIFICATIONS (API 33+) pedida em runtime depois da primeira confirmação. */
    private val pedirPermissao = registerForActivityResult(ActivityResultContracts.RequestPermission()) { irParaMarcacoes() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentVerificarBinding.bind(view)
        // Review Focus 2: se o ecrã rodar ou a app for morta, a marcação vem das prefs e não só dos argumentos
        val doArgumento = arguments?.getInt("marcacao_id", -1) ?: -1
        marcacaoId = doArgumento.takeIf { it != -1 } ?: prefs.pendenteMarcacaoId
        if (marcacaoId == -1 || prefs.pendenteMarcacaoId != marcacaoId || prefs.pendenteToken == null) {
            semPendente(doArgumento)
            return
        }

        val enviado = arguments?.getBoolean("email_enviado", true) ?: true
        b.info.text = if (enviado) getString(R.string.verificar_info, prefs.clienteEmail.orEmpty())
        else getString(R.string.verificar_email_falhou)
        mostrarCodigoDev(arguments?.getString("codigo_dev"))
        iniciarContador()

        b.botaoConfirmar.setOnClickListener { verificar() }
        b.botaoReenviar.setOnClickListener { reenviar() }
        b.botaoAbrirEmail.setOnClickListener {
            requireContext().abrir(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Já não há pendente: se a marcação deste ecrã ficou confirmada (ex.: rodou durante o ✓), vai para Marcações. */
    private fun semPendente(id: Int) {
        b.conteudo.isVisible = false
        viewLifecycleOwner.lifecycleScope.launch {
            val confirmada = id != -1 && Repositorio.dao.obter(id)?.estado == "confirmada"
            if (confirmada) irParaMarcacoes()
            else {
                mostrarErro(getString(R.string.verificar_sem_pendente))
                voltarAoMarcar()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // O AlarmManager avisa quando o código expira → voltamos ao Marcar
        receiver = Eventos.registar(requireContext()) { tipo ->
            if (tipo == Eventos.CODIGO_EXPIRADO && _b != null && !b.sucesso.isVisible) {
                mostrarErro(getString(R.string.verificar_expirou))
                voltarAoMarcar()
            }
        }
    }

    override fun onStop() {
        Eventos.remover(requireContext(), receiver)
        receiver = null
        super.onStop()
    }

    private fun mostrarCodigoDev(codigo: String?) {
        // A política está no servidor: só envia o código em desenvolvimento ou quando o email falhou
        b.codigoDev.isVisible = codigo != null
        b.codigoDev.text = getString(R.string.verificar_codigo_dev, codigo.orEmpty())
    }

    private fun iniciarContador() {
        contador?.cancel()
        val expira = prefs.pendenteCodigoExpiraEm ?: return
        val falta = paraEpochMillis(expira) - System.currentTimeMillis()
        if (falta <= 0) { b.contador.setText(R.string.verificar_expirou); return }
        contador = object : CountDownTimer(falta, 1000) {
            override fun onTick(ms: Long) {
                if (_b == null) return
                val s = ms / 1000
                b.contador.text = getString(R.string.verificar_expira_em, "%d:%02d".format(s / 60, s % 60))
            }
            override fun onFinish() { _b?.contador?.setText(R.string.verificar_expirou) }
        }.start()
    }

    private fun verificar() {
        val codigo = b.inputCodigo.text?.toString()?.filter { it.isDigit() }.orEmpty()
        if (codigo.length != 6) { b.campoCodigo.error = getString(R.string.verificar_codigo_incompleto); return }
        b.campoCodigo.error = null
        val token = prefs.pendenteToken ?: return voltarAoMarcar()
        // Revisão #2: o resumo é copiado ANTES do pedido (o alarme de expiração pode apagá-lo entretanto)
        val resumoJson = prefs.pendenteResumo ?: return voltarAoMarcar()
        val ctx = requireContext().applicationContext
        val id = marcacaoId
        aCarregar(true)

        // Revisão #1: verificar + guardar correm no scope da app; rodar o ecrã já não perde a confirmação
        val job = Repositorio.escopo.async {
            val r = Repositorio.verificar(id, codigo, token)
            when {
                r is Resultado.Ok -> resumoConfirmado(resumoJson, r.dados.token)?.let { m ->
                    Repositorio.guardarConfirmada(m)
                    prefs.sessaoAtiva = true
                    prefs.limparPendente()
                    Agendador.aoConfirmar(ctx, m)
                    Eventos.emitir(ctx, Eventos.MARCACAO)
                    Eventos.emitir(ctx, Eventos.SESSAO)
                }
                r is Resultado.Erro && r.codigo in TERMINAIS -> {
                    Lembretes.cancelarExpiracao(ctx)
                    prefs.limparPendente()
                }
            }
            r
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val r = job.await()
            if (_b == null) return@launch
            aCarregar(false)
            when (r) {
                is Resultado.Ok -> { contador?.cancel(); animarSucesso() }
                is Resultado.Erro -> when (r.codigo) {
                    "codigo_errado" -> b.campoCodigo.error =
                        getString(R.string.verificar_restam, r.mensagem, r.tentativasRestantes ?: 0)
                    in TERMINAIS -> { mostrarErro(r.mensagem); voltarAoMarcar() }
                    else -> b.campoCodigo.error = r.mensagem
                }
            }
        }
    }

    private fun reenviar() {
        val token = prefs.pendenteToken ?: return voltarAoMarcar()
        aCarregar(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val r = Repositorio.reenviar(marcacaoId, token)
            if (_b == null) return@launch
            aCarregar(false)
            when (r) {
                is Resultado.Ok -> {
                    prefs.pendenteCodigoExpiraEm = r.dados.codigoExpiraEm
                    Lembretes.agendarExpiracao(requireContext(), marcacaoId, r.dados.codigoExpiraEm)
                    iniciarContador()
                    mostrarCodigoDev(r.dados.codigoDev)
                    mostrarErro(getString(if (r.dados.emailEnviado) R.string.verificar_reenviado else R.string.verificar_email_falhou))
                }
                is Resultado.Erro -> mostrarErro(r.mensagem)
            }
        }
    }

    /**
     * AnimatorSet: o ✓ aparece com fade + escala. O ecrã fica com o resumo da marcação, "Adicionar ao calendário"
     * (Intent implícito) e "Ver as minhas marcações" (que pede a permissão de notificações e segue).
     */
    private fun animarSucesso() {
        b.sucesso.alpha = 0f
        b.sucesso.isVisible = true
        b.iconeSucesso.scaleX = 0.3f
        b.iconeSucesso.scaleY = 0.3f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(b.sucesso, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(b.iconeSucesso, View.SCALE_X, 0.3f, 1f),
                ObjectAnimator.ofFloat(b.iconeSucesso, View.SCALE_Y, 0.3f, 1f),
            )
            duration = 450
            start()
        }
        b.botaoVerMarcacoes.setOnClickListener { pedirPermissaoOuSeguir() }
        b.botaoCalendario.isVisible = false
        viewLifecycleOwner.lifecycleScope.launch {
            val m = Repositorio.dao.obter(marcacaoId) ?: return@launch
            if (_b == null) return@launch
            b.resumoSucesso.text = getString(R.string.verificar_resumo, m.servicoNome, m.barbeiroNome, dataCurta(m.inicio), horaCurta(m.inicio))
            b.botaoCalendario.isVisible = true
            b.botaoCalendario.setOnClickListener { adicionarAoCalendario(m) }
        }
    }

    /** Intent implícito: abre a app de calendário com o evento já preenchido (o utilizador confirma lá). */
    private fun adicionarAoCalendario(m: MarcacaoLocal) {
        val barbearia = prefs.barbeariaNome.orEmpty()
        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, paraEpochMillis(m.inicio))
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, paraEpochMillis(m.fim))
            .putExtra(CalendarContract.Events.TITLE, getString(R.string.verificar_evento_titulo, m.servicoNome, barbearia))
            .putExtra(CalendarContract.Events.DESCRIPTION, getString(R.string.marcacao_com, m.servicoNome, m.barbeiroNome))
        requireContext().abrir(intent)
    }

    private fun pedirPermissaoOuSeguir() {
        val ctx = requireContext()
        if (Build.VERSION.SDK_INT < 33 || Notificacoes.temPermissao(ctx)) { irParaMarcacoes(); return }
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.perm_titulo)
                .setMessage(R.string.perm_texto)
                .setPositiveButton(R.string.perm_permitir) { _, _ -> pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS) }
                .setNegativeButton(R.string.agora_nao) { _, _ -> irParaMarcacoes() }
                .setCancelable(false)
                .show()
        } else {
            pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun irParaMarcacoes() = quandoVisivel {
        findNavController().popBackStack(R.id.marcarFragment, false)
        (activity as? MainActivity)?.irPara(R.id.marcacoesFragment)
    }

    private fun voltarAoMarcar() = quandoVisivel {
        findNavController().popBackStack(R.id.marcarFragment, false)
    }

    private fun aCarregar(sim: Boolean) {
        b.progresso.isVisible = sim
        b.botaoConfirmar.isEnabled = !sim
        b.botaoReenviar.isEnabled = !sim
    }

    override fun onDestroyView() {
        contador?.cancel()
        _b = null
        super.onDestroyView()
    }

    private companion object {
        /** Erros depois dos quais a marcação pendente deixa de existir. */
        val TERMINAIS = setOf("expirada", "token_invalido", "ja_confirmada")
    }
}
