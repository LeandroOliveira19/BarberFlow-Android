package barberflow.cliente.ui.inicio

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import barberflow.cliente.dados.api.dto.ReviewDto
import barberflow.cliente.databinding.ItemReviewBinding

class ReviewsAdapter(private val lista: List<ReviewDto>) : RecyclerView.Adapter<ReviewsAdapter.ViewHolder>() {

    class ViewHolder(val b: ItemReviewBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val r = lista[pos]
        h.b.autor.text = r.autor
        h.b.estrelas.rating = r.estrelas.toFloat()
        h.b.texto.text = r.texto.orEmpty()
        h.b.texto.isVisible = !r.texto.isNullOrBlank()
    }

    override fun getItemCount() = lista.size
}
