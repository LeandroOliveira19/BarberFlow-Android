package barberflow.cliente.ui.marcar

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import barberflow.cliente.R
import barberflow.cliente.databinding.ItemDiaBinding
import barberflow.cliente.databinding.ItemOutraDataBinding
import barberflow.cliente.databinding.ItemPeriodoBinding
import barberflow.cliente.databinding.ItemSlotBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Carrossel de dias do Marcar: um cartão por dia (os fechados ficam desligados) e, no fim,
 * "Outra data", que abre o DatePickerDialog.
 */
class DiasAdapter(
    private val dias: List<DiaCarrossel>,
    private var selecionado: LocalDate?,
    private val aoEscolher: (LocalDate) -> Unit,
    private val aoOutraData: () -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class DiaHolder(val b: ItemDiaBinding) : RecyclerView.ViewHolder(b.root)
    class OutraHolder(val b: ItemOutraDataBinding) : RecyclerView.ViewHolder(b.root)

    override fun getItemCount() = dias.size + 1
    override fun getItemViewType(position: Int) = if (position < dias.size) TIPO_DIA else TIPO_OUTRA

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_DIA) DiaHolder(ItemDiaBinding.inflate(inf, parent, false))
        else OutraHolder(ItemOutraDataBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
        if (h is OutraHolder) { h.b.root.setOnClickListener { aoOutraData() }; return }
        val b = (h as DiaHolder).b
        val d = dias[pos]
        val ctx = b.root.context
        val locale = ctx.resources.configuration.locales[0]
        val ativo = d.data == selecionado
        b.semana.text = curto(d.data, "EEE", locale).take(3)
        b.numero.text = d.data.dayOfMonth.toString()
        b.mes.text = curto(d.data, "MMM", locale)
        b.raiz.isSelected = ativo
        b.raiz.isEnabled = d.aberto
        b.raiz.alpha = if (d.aberto) 1f else 0.35f
        val corForte = ctx.getColor(if (ativo) R.color.bf_preto else R.color.bf_texto)
        val corFraca = ctx.getColor(if (ativo) R.color.bf_preto else R.color.bf_texto_secundario)
        b.numero.setTextColor(corForte)
        b.semana.setTextColor(corFraca)
        b.mes.setTextColor(corFraca)
        val extenso = d.data.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
        b.raiz.contentDescription = if (d.aberto) extenso else ctx.getString(R.string.marcar_dia_fechado, extenso)
        b.raiz.setOnClickListener { if (d.aberto) aoEscolher(d.data) }
    }

    /** Marca [data] como escolhida; devolve a posição dela (ou -1 se não estiver no carrossel). */
    fun selecionar(data: LocalDate?): Int {
        val anterior = dias.indexOfFirst { it.data == selecionado }
        selecionado = data
        val nova = dias.indexOfFirst { it.data == data }
        if (anterior >= 0) notifyItemChanged(anterior)
        if (nova >= 0) notifyItemChanged(nova)
        return nova
    }

    private fun curto(d: LocalDate, padrao: String, locale: Locale) =
        DateTimeFormatter.ofPattern(padrao, locale).format(d).filter { it.isLetter() }.uppercase(locale)

    private companion object {
        const val TIPO_DIA = 0
        const val TIPO_OUTRA = 1
    }
}

/** Grelha de horários em "Manhã" e "Tarde"; o escolhido fica dourado e "pulsa" (ObjectAnimator). */
class SlotsAdapter(
    slots: List<String>,
    private var selecionado: String?,
    private val aoEscolher: (String) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface Linha
    private data class Periodo(@StringRes val titulo: Int) : Linha
    private data class Hora(val hora: String) : Linha

    private val linhas: List<Linha> = separarPorPeriodo(slots).let { (manha, tarde) ->
        buildList {
            if (manha.isNotEmpty()) { add(Periodo(R.string.marcar_manha)); manha.forEach { add(Hora(it)) } }
            if (tarde.isNotEmpty()) { add(Periodo(R.string.marcar_tarde)); tarde.forEach { add(Hora(it)) } }
        }
    }

    class PeriodoHolder(val b: ItemPeriodoBinding) : RecyclerView.ViewHolder(b.root)
    class HoraHolder(val b: ItemSlotBinding) : RecyclerView.ViewHolder(b.root)

    /** Os títulos dos períodos ocupam a linha inteira da grelha. */
    fun configurarGrelha(grelha: GridLayoutManager) {
        val colunas = grelha.spanCount
        grelha.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = if (linhas.getOrNull(position) is Periodo) colunas else 1
        }
    }

    override fun getItemCount() = linhas.size
    override fun getItemViewType(position: Int) = if (linhas[position] is Periodo) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == 0) PeriodoHolder(ItemPeriodoBinding.inflate(inf, parent, false))
        else HoraHolder(ItemSlotBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
        when (val l = linhas[pos]) {
            is Periodo -> (h as PeriodoHolder).b.titulo.setText(l.titulo)
            is Hora -> {
                val b = (h as HoraHolder).b
                val ativo = l.hora == selecionado
                b.hora.text = l.hora
                b.hora.isSelected = ativo
                b.hora.setTextColor(b.root.context.getColor(if (ativo) R.color.bf_preto else R.color.bf_texto))
                b.root.setOnClickListener {
                    val anterior = linhas.indexOfFirst { it is Hora && it.hora == selecionado }
                    selecionado = l.hora
                    if (anterior >= 0) notifyItemChanged(anterior)
                    notifyItemChanged(h.bindingAdapterPosition)
                    pulsar(b.root)
                    aoEscolher(l.hora)
                }
            }
        }
    }

    private fun pulsar(v: View) {
        ObjectAnimator.ofPropertyValuesHolder(
            v,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f, 1f),
        ).setDuration(220).start()
    }
}
