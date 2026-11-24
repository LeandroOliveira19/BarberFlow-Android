package barberflow.cliente.ui

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.annotation.StringRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import barberflow.cliente.databinding.ItemCabecalhoBinding

/** Subtítulo no topo de uma lista (junta-se ao adapter da lista com ConcatAdapter). */
class CabecalhoAdapter(@StringRes private val texto: Int) : RecyclerView.Adapter<CabecalhoAdapter.ViewHolder>() {
    class ViewHolder(val b: ItemCabecalhoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCabecalhoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) = h.b.texto.setText(texto)

    override fun getItemCount() = 1
}

/** Numa grelha, o cabeçalho (posição 0) ocupa a largura toda. */
fun GridLayoutManager.cabecalhoLarguraToda() {
    val colunas = spanCount
    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int) = if (position == 0) colunas else 1
    }
}

/**
 * Entrada de um card da lista: aparece e sobe um pouco, com um atraso pequeno por posição
 * (os cards entram um a um). Com as animações do sistema desligadas aparece logo.
 */
fun View.animarEntrada(posicao: Int) {
    animate().cancel()
    if (!ValueAnimator.areAnimatorsEnabled()) {
        alpha = 1f; translationY = 0f
        return
    }
    alpha = 0f
    translationY = 24 * resources.displayMetrics.density
    animate().alpha(1f).translationY(0f)
        .setStartDelay(posicao.coerceAtMost(8) * 45L)
        .setDuration(380)
        .setInterpolator(DecelerateInterpolator(1.6f))
        .start()
}
