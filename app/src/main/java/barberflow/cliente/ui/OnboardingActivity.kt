package barberflow.cliente.ui

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import barberflow.cliente.BuildConfig
import barberflow.cliente.R
import barberflow.cliente.dados.slugEfetivo
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.dados.Resultado
import barberflow.cliente.databinding.ActivityOnboardingBinding
import kotlinx.coroutines.launch

/**
 * Ecrã de entrada: o cliente indica o código (slug) da barbearia, que fica guardado em SharedPreferences.
 * Também abre por link/QR: barberflow://b/rlbarber (Intent implícito ACTION_VIEW).
 */
class OnboardingActivity : AppCompatActivity() {
    private val binding by lazy { ActivityOnboardingBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repositorio.iniciar(this)

        // APK próprio de uma barbearia (ex.: RLBarber): não pergunta nada, entra logo
        val fixo = slugEfetivo(BuildConfig.SLUG_FIXO, null)
        if (fixo != null) {
            if (Repositorio.prefs.slug != fixo) {
                Repositorio.prefs.slug = fixo
                Repositorio.prefs.barbeariaNome = getString(R.string.app_name)
            }
            abrirMain()
            return
        }

        val slugLink = slugDoLink(intent)
        if (slugLink == null && Repositorio.prefs.slug != null) {
            abrirMain()
            return
        }

        setContentView(binding.root)
        binding.logo.animate().alpha(1f).setDuration(600).start()
        binding.botaoEntrar.setOnClickListener { entrar() }
        binding.inputSlug.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_GO) { entrar(); true } else false
        }
        if (slugLink != null) {
            binding.inputSlug.setText(slugLink)
            entrar()
        }
    }

    private fun slugDoLink(i: Intent?): String? {
        val uri = i?.data ?: return null
        if (uri.scheme != "barberflow" || uri.host != "b") return null
        return uri.pathSegments.firstOrNull()?.lowercase()
    }

    private fun entrar() {
        val slug = binding.inputSlug.text?.toString()?.trim()?.lowercase().orEmpty()
        if (slug.isEmpty()) {
            binding.campoSlug.error = getString(R.string.onb_slug_vazio)
            return
        }
        binding.campoSlug.error = null
        aCarregar(true)
        lifecycleScope.launch {
            when (val r = Repositorio.barbearia(slug)) {
                is Resultado.Ok -> {
                    Repositorio.prefs.slug = r.dados.slug
                    Repositorio.prefs.barbeariaNome = r.dados.nome
                    abrirMain()
                }
                is Resultado.Erro -> {
                    aCarregar(false)
                    binding.campoSlug.error = r.mensagem
                }
            }
        }
    }

    private fun aCarregar(sim: Boolean) {
        binding.progresso.isVisible = sim
        binding.botaoEntrar.isEnabled = !sim
    }

    /** Intent explícito para a MainActivity; o onboarding sai do back stack. */
    private fun abrirMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
