package barberflow.cliente.ui.marcar

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import barberflow.cliente.BuildConfig
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import barberflow.cliente.dados.api.dto.NovaMarcacaoPedido
import barberflow.cliente.dados.local.MarcacaoLocal
import barberflow.cliente.databinding.FragmentDadosClienteBinding
import barberflow.cliente.tarefas.Agendador
import barberflow.cliente.ui.mostrarErro
import barberflow.cliente.ui.quandoVisivel
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Passo 2: nome, telemóvel e email (pré-preenchidos das SharedPreferences) → POST da marcação. */
class DadosClienteFragment : Fragment(R.layout.fragment_dados_cliente) {
    private var _b: FragmentDadosClienteBinding? = null
    private val b get() = _b!!
    private val prefs get() = Repositorio.prefs

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentDadosClienteBinding.bind(view)
        val a = requireArguments()
        val data = LocalDate.parse(a.getString("data"))
        b.resumo.text = getString(
            R.string.marcar_resumo,
            a.getString("servico_nome"), a.getString("barbeiro_nome"),
            data.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-PT"))),
            a.getString("hora"),
        )
        if (savedInstanceState == null) {
            b.inputNome.setText(prefs.clienteNome)
            b.inputTelemovel.setText(prefs.clienteTelemovel)
            b.inputEmail.setText(prefs.clienteEmail)
        }
        b.botaoConfirmar.setOnClickListener { confirmar() }
    }

    private fun confirmar() {
        val nome = b.inputNome.text?.toString()?.trim().orEmpty()
        val tel = b.inputTelemovel.text?.toString()?.trim().orEmpty()
        val email = b.inputEmail.text?.toString()?.trim().orEmpty()
        b.campoNome.error = if (nomeValido(nome)) null else getString(R.string.erro_nome)
        b.campoTelemovel.error = if (telemovelValido(tel)) null else getString(R.string.erro_telemovel)
        b.campoEmail.error = if (emailValido(email)) null else getString(R.string.erro_email)
        if (b.campoNome.error != null || b.campoTelemovel.error != null || b.campoEmail.error != null) return

        val a = requireArguments()
        val slug = prefs.slug ?: return
        val ctx = requireContext().applicationContext
        val pedido = NovaMarcacaoPedido(
            a.getInt("servico_id"), a.getInt("barbeiro_id"), a.getString("data")!!, a.getString("hora")!!, nome, tel, email,
        )
        aCarregar(true)

        // Revisão #1: o POST corre no scope da app e o resultado é guardado ANTES de tocar na UI.
        // Rodar o ecrã ou sair da app a meio já não deixa uma marcação "órfã" no servidor.
        val pedidoJob = Repositorio.escopo.async {
            val r = Repositorio.criarMarcacao(slug, pedido)
            if (r is Resultado.Ok) {
                prefs.clienteNome = nome
                prefs.clienteTelemovel = tel
                prefs.clienteEmail = email
                prefs.pendenteMarcacaoId = r.dados.marcacaoId
                prefs.pendenteCodigoExpiraEm = r.dados.codigoExpiraEm
                prefs.pendenteToken = r.dados.tokenPendente
                prefs.pendenteResumo = Gson().toJson(resumo(a, r.dados.marcacaoId, slug))
                Agendador.aoCriar(ctx, r.dados.marcacaoId, r.dados.codigoExpiraEm)
            }
            r
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val r = pedidoJob.await()
            if (_b != null) aCarregar(false)
            quandoVisivel {
                when (r) {
                    is Resultado.Ok -> findNavController().navigate(
                        R.id.action_dados_para_verificar,
                        bundleOf(
                            "marcacao_id" to r.dados.marcacaoId,
                            "codigo_dev" to if (BuildConfig.MOSTRAR_CODIGO_DEV) r.dados.codigoDev else null,
                            "email_enviado" to r.dados.emailEnviado,
                        ),
                    )
                    is Resultado.Erro -> if (r.codigo == "slot_ocupado") {
                        // Volta ao passo anterior, que recarrega os horários
                        Toast.makeText(requireContext(), R.string.slot_ocupado_recarregado, Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    } else {
                        mostrarErro(r.mensagem)
                    }
                }
            }
        }
    }

    /** Resumo guardado até à confirmação (o token final só chega depois de verificar o código). */
    private fun resumo(a: Bundle, id: Int, slug: String): MarcacaoLocal {
        val inicio = LocalDate.parse(a.getString("data")).atTime(LocalTime.parse(a.getString("hora")))
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return MarcacaoLocal(
            id = id, token = "", slug = slug,
            servicoNome = a.getString("servico_nome").orEmpty(),
            barbeiroNome = a.getString("barbeiro_nome").orEmpty(),
            inicio = inicio.format(fmt),
            fim = inicio.plusMinutes(a.getInt("duracao").toLong()).format(fmt),
            estado = "pendente_verificacao",
        )
    }

    private fun aCarregar(sim: Boolean) {
        b.progresso.isVisible = sim
        b.botaoConfirmar.isEnabled = !sim
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}
