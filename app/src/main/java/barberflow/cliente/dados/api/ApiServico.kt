package barberflow.cliente.dados.api

import barberflow.cliente.dados.api.dto.BarbeariaDto
import barberflow.cliente.dados.api.dto.BarbeiroDto
import barberflow.cliente.dados.api.dto.CodigoPedido
import barberflow.cliente.dados.api.dto.DisponibilidadeDto
import barberflow.cliente.dados.api.dto.MarcacaoDto
import barberflow.cliente.dados.api.dto.NovaMarcacaoPedido
import barberflow.cliente.dados.api.dto.NovaMarcacaoResposta
import barberflow.cliente.dados.api.dto.ReenvioPedido
import barberflow.cliente.dados.api.dto.RemarcarPedido
import barberflow.cliente.dados.api.dto.TokenPedido
import barberflow.cliente.dados.api.dto.ReenvioResposta
import barberflow.cliente.dados.api.dto.ReviewDto
import barberflow.cliente.dados.api.dto.ServicoDto
import barberflow.cliente.dados.api.dto.VerificacaoResposta
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Endpoints da API v1. O servidor tem um router único (index.php?r=<rota>),
 * por isso a rota vai sempre no parâmetro "r" e nunca no caminho.
 */
interface ApiServico {
    @GET("index.php")
    suspend fun barbearia(@Query("r") r: String): Response<BarbeariaDto>

    @GET("index.php")
    suspend fun servicos(@Query("r") r: String): Response<List<ServicoDto>>

    @GET("index.php")
    suspend fun equipa(@Query("r") r: String): Response<List<BarbeiroDto>>

    @GET("index.php")
    suspend fun reviews(@Query("r") r: String): Response<List<ReviewDto>>

    @GET("index.php")
    suspend fun disponibilidade(
        @Query("r") r: String,
        @Query("servico_id") servicoId: Int,
        @Query("barbeiro_id") barbeiroId: Int,
        @Query("data") data: String,
    ): Response<DisponibilidadeDto>

    @POST("index.php")
    suspend fun criarMarcacao(@Query("r") r: String, @Body pedido: NovaMarcacaoPedido): Response<NovaMarcacaoResposta>

    @POST("index.php")
    suspend fun verificar(@Query("r") r: String, @Body pedido: CodigoPedido): Response<VerificacaoResposta>

    @POST("index.php")
    suspend fun reenviar(@Query("r") r: String, @Body pedido: ReenvioPedido): Response<ReenvioResposta>

    @GET("index.php")
    suspend fun marcacoes(@Query("r") r: String, @Query("tokens") tokens: String): Response<List<MarcacaoDto>>

    @POST("index.php")
    suspend fun cancelar(@Query("r") r: String, @Body pedido: TokenPedido): Response<MarcacaoDto>

    @POST("index.php")
    suspend fun remarcar(@Query("r") r: String, @Body pedido: RemarcarPedido): Response<MarcacaoDto>
}
