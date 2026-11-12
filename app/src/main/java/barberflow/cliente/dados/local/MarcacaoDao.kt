package barberflow.cliente.dados.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MarcacaoDao {
    @Upsert
    suspend fun upsert(m: MarcacaoLocal)

    @Query("SELECT * FROM marcacoes WHERE slug = :slug ORDER BY inicio DESC")
    suspend fun listarPorSlug(slug: String): List<MarcacaoLocal>

    /** `agora` no formato da API ("yyyy-MM-dd HH:mm:ss"): a ordenação de texto coincide com a cronológica. */
    @Query("SELECT * FROM marcacoes WHERE estado = 'confirmada' AND inicio > :agora")
    suspend fun futurasConfirmadas(agora: String): List<MarcacaoLocal>

    @Query("SELECT * FROM marcacoes")
    suspend fun todas(): List<MarcacaoLocal>

    @Query("SELECT * FROM marcacoes WHERE id = :id")
    suspend fun obter(id: Int): MarcacaoLocal?

    @Query("UPDATE marcacoes SET estado = :estado WHERE id = :id")
    suspend fun atualizarEstado(id: Int, estado: String)

    @Query("UPDATE marcacoes SET inicio = :inicio, fim = :fim, barbeiroId = :barbeiroId, barbeiroNome = :barbeiroNome WHERE id = :id")
    suspend fun atualizarHorario(id: Int, inicio: String, fim: String, barbeiroId: Int, barbeiroNome: String)

    @Query("UPDATE marcacoes SET servicoId = :servicoId, barbeiroId = :barbeiroId WHERE id = :id")
    suspend fun atualizarIds(id: Int, servicoId: Int?, barbeiroId: Int?)

    @Query("DELETE FROM marcacoes")
    suspend fun apagarTodas()
}
