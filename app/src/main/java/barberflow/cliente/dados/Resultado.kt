package barberflow.cliente.dados

import barberflow.cliente.dados.api.dto.ErroCorpo
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

/** Resultado de uma operação de dados: ou temos os dados, ou uma mensagem de erro para mostrar. */
sealed class Resultado<out T> {
    data class Ok<T>(val dados: T) : Resultado<T>()
    data class Erro(
        val mensagem: String,
        val codigo: String? = null,
        val tentativasRestantes: Int? = null,
    ) : Resultado<Nothing>()
}

private val gson = Gson()

/** Lê o corpo de erro da API ({"erro":{codigo,mensagem}}); se vier HTML ou vazio, devolve uma mensagem genérica. */
fun lerErro(status: Int, corpo: String?): Resultado.Erro {
    val generico = Resultado.Erro("Ocorreu um erro no servidor ($status). Tenta de novo.")
    if (corpo.isNullOrBlank()) return generico
    return try {
        val erro = gson.fromJson(corpo, ErroCorpo::class.java)?.erro ?: return generico
        val msg = erro.mensagem?.takeIf { it.isNotBlank() } ?: return generico
        Resultado.Erro(msg, erro.codigo, erro.tentativasRestantes)
    } catch (_: Exception) {
        generico
    }
}

/** Executa um pedido Retrofit e converte tudo (sucesso, erro HTTP, falha de rede) num Resultado. */
suspend fun <T> chamar(bloco: suspend () -> Response<T>): Resultado<T> = try {
    val resp = bloco()
    val corpo = resp.body()
    if (resp.isSuccessful && corpo != null) Resultado.Ok(corpo)
    else lerErro(resp.code(), resp.errorBody()?.string())
} catch (_: IOException) {
    Resultado.Erro("Não foi possível contactar o servidor.")
} catch (_: Exception) {
    // ex.: o servidor respondeu 200 com HTML (Gson não consegue ler)
    Resultado.Erro("Resposta inesperada do servidor. Tenta de novo.")
}
