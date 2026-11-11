package barberflow.cliente.ui

import java.text.NumberFormat
import java.util.Locale

private val PT = Locale.forLanguageTag("pt-PT")

/** 12.5 → "12,50 €" */
fun formatarPreco(v: Double): String {
    val nf = NumberFormat.getNumberInstance(PT).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
    return "${nf.format(v)} €"
}

/** 30 → "30 min"; 75 → "1 h 15 min"; 120 → "2 h" */
fun formatarDuracao(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}

/** "@andre" → "https://instagram.com/andre"; URLs completos ficam iguais. */
fun urlInstagram(v: String): String {
    val t = v.trim()
    return if (t.startsWith("http://") || t.startsWith("https://")) t
    else "https://instagram.com/" + t.removePrefix("@")
}
