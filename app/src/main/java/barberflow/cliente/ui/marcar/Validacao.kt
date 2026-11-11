package barberflow.cliente.ui.marcar

/*
 * Validação só de FORMATO, para dar feedback imediato. As regras de negócio
 * (e a validação definitiva) ficam no servidor.
 */

/** Telemóvel/telefone português: aceita espaços, +351 e 00351 (igual à regra da API). */
fun telemovelValido(s: String): Boolean {
    var d = s.filter { it.isDigit() }
    if (d.length == 12 && d.startsWith("351")) d = d.substring(3)
    if (d.length == 14 && d.startsWith("00351")) d = d.substring(5)
    return Regex("^[239]\\d{8}$").matches(d)
}

fun emailValido(s: String): Boolean = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(s.trim())

fun nomeValido(s: String): Boolean = s.trim().length in 2..120
