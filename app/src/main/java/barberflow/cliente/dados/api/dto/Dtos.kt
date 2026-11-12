package barberflow.cliente.dados.api.dto

import com.google.gson.annotations.SerializedName

/*
 * Objetos de transferência da API v1. O JSON usa snake_case; o Kotlin usa camelCase.
 * @SerializedName faz o mapeamento entre os dois (conceito leccionado na UC).
 */

data class BarbeariaDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nome") val nome: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("morada") val morada: String?,
    @SerializedName("email_contacto") val emailContacto: String?,
    @SerializedName("telemovel_contacto") val telemovelContacto: String?,
    @SerializedName("tema_cor") val temaCor: String,
    @SerializedName("tema_fundo") val temaFundo: String,
    @SerializedName("dias_funcionamento") val diasFuncionamento: List<Int>,
    @SerializedName("hora_abertura") val horaAbertura: String?,
    @SerializedName("hora_fecho") val horaFecho: String?,
    @SerializedName("max_dias") val maxDias: Int,
    @SerializedName("codigo_validade_min") val codigoValidadeMin: Int,
    /** Até quantas horas antes o cliente pode cancelar/remarcar (null = servidor antigo). */
    @SerializedName("antecedencia_alteracao_horas") val antecedenciaAlteracaoHoras: Int? = null,
) {
    fun antecedenciaHoras(): Int = antecedenciaAlteracaoHoras ?: 24
}

data class ServicoDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nome") val nome: String,
    @SerializedName("descricao") val descricao: String?,
    @SerializedName("duracao_minutos") val duracaoMinutos: Int,
    @SerializedName("preco") val preco: Double,
)

data class BarbeiroDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nome") val nome: String,
    @SerializedName("foto_url") val fotoUrl: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("especialidades") val especialidades: String?,
    @SerializedName("instagram") val instagram: String?,
)

data class ReviewDto(
    @SerializedName("autor") val autor: String,
    @SerializedName("foto_url") val fotoUrl: String?,
    @SerializedName("estrelas") val estrelas: Int,
    @SerializedName("texto") val texto: String?,
    @SerializedName("data") val data: String?,
)

data class DisponibilidadeDto(
    @SerializedName("data") val data: String,
    @SerializedName("slots") val slots: List<String>,
)

data class NovaMarcacaoPedido(
    @SerializedName("servico_id") val servicoId: Int,
    @SerializedName("barbeiro_id") val barbeiroId: Int,
    @SerializedName("data") val data: String,
    @SerializedName("hora") val hora: String,
    @SerializedName("nome") val nome: String,
    @SerializedName("telemovel") val telemovel: String,
    @SerializedName("email") val email: String,
    /** Para quem é a marcação (null = para quem marca; o Gson omite o campo). */
    @SerializedName("para_nome") val paraNome: String? = null,
)

data class NovaMarcacaoResposta(
    @SerializedName("marcacao_id") val marcacaoId: Int,
    @SerializedName("estado") val estado: String,
    @SerializedName("codigo_expira_em") val codigoExpiraEm: String?,
    @SerializedName("email_enviado") val emailEnviado: Boolean,
    @SerializedName("token_pendente") val tokenPendente: String,
    @SerializedName("codigo_dev") val codigoDev: String?,
)

data class CodigoPedido(
    @SerializedName("codigo") val codigo: String,
    @SerializedName("token_pendente") val tokenPendente: String,
)

data class ReenvioPedido(
    @SerializedName("token_pendente") val tokenPendente: String,
)

data class VerificacaoResposta(
    @SerializedName("marcacao_id") val marcacaoId: Int,
    @SerializedName("estado") val estado: String,
    @SerializedName("token") val token: String,
)

data class ReenvioResposta(
    @SerializedName("codigo_expira_em") val codigoExpiraEm: String,
    @SerializedName("email_enviado") val emailEnviado: Boolean,
    @SerializedName("codigo_dev") val codigoDev: String?,
)

data class MarcacaoDto(
    @SerializedName("id") val id: Int,
    @SerializedName("estado") val estado: String,
    @SerializedName("inicio") val inicio: String,
    @SerializedName("fim") val fim: String,
    @SerializedName("servico_nome") val servicoNome: String,
    @SerializedName("barbeiro_nome") val barbeiroNome: String,
    @SerializedName("barbearia_slug") val barbeariaSlug: String,
    @SerializedName("barbearia_nome") val barbeariaNome: String,
    @SerializedName("para_nome") val paraNome: String? = null,
    /** Precisos para remarcar (o serviço fica o mesmo). Null num servidor antigo. */
    @SerializedName("servico_id") val servicoId: Int? = null,
    @SerializedName("barbeiro_id") val barbeiroId: Int? = null,
)

/** Cancelar: o token da marcação prova que é do cliente. */
data class TokenPedido(@SerializedName("token") val token: String)

data class RemarcarPedido(
    @SerializedName("token") val token: String,
    @SerializedName("barbeiro_id") val barbeiroId: Int,
    @SerializedName("data") val data: String,
    @SerializedName("hora") val hora: String,
)

data class ErroCorpo(@SerializedName("erro") val erro: ErroDto?)

data class ErroDto(
    @SerializedName("codigo") val codigo: String?,
    @SerializedName("mensagem") val mensagem: String?,
    @SerializedName("tentativas_restantes") val tentativasRestantes: Int?,
)
