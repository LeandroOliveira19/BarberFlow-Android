package barberflow.cliente.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import barberflow.cliente.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/** Abre um Intent implícito; se não houver app que o trate, avisa em vez de rebentar. */
fun Context.abrir(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.sem_app, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Snackbar de erro, com botão "Tentar de novo" opcional.
 * Revisão #3: o Snackbar desaparece com a view do Fragment e a ação nunca corre depois disso.
 */
fun Fragment.mostrarErro(mensagem: String, tentar: (() -> Unit)? = null) {
    val v = view ?: return
    val s = Snackbar.make(v, mensagem, if (tentar != null) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG)
    if (tentar != null) s.setAction(R.string.tentar_de_novo) { if (view != null) tentar() }
    viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, e ->
        if (e == Lifecycle.Event.ON_DESTROY) s.dismiss()
    })
    s.show()
}

/**
 * Corre `bloco` (navegação, diálogos) só quando o Fragment está visível (STARTED).
 * Revisão (crítico): navegar depois de onStop lança IllegalStateException; aqui espera-se pelo regresso
 * e, se a view for destruída entretanto, não se faz nada.
 */
fun Fragment.quandoVisivel(bloco: () -> Unit) {
    if (view == null) return
    viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.withStarted { bloco() } }
}

/** Fade-in de uma lista quando os dados chegam. */
fun View.aparecer() {
    if (alpha == 1f) return
    animate().alpha(1f).setDuration(300).start()
}

fun rotuloEstado(ctx: Context, estado: String): String = ctx.getString(
    when (estado) {
        "pendente_verificacao" -> R.string.estado_pendente_verificacao
        "confirmada" -> R.string.estado_confirmada
        "concluida" -> R.string.estado_concluida
        "cancelada" -> R.string.estado_cancelada
        else -> R.string.estado_expirada
    }
)

@ColorRes
fun corEstado(estado: String): Int = when (estado) {
    "confirmada" -> R.color.bf_sucesso
    "pendente_verificacao" -> R.color.bf_aviso
    "concluida" -> R.color.bf_info
    else -> R.color.bf_erro
}
