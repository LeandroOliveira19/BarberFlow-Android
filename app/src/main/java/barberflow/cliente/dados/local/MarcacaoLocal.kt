package barberflow.cliente.dados.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Marcação feita neste dispositivo (permite ver a lista offline). O id é o da API. */
@Entity(tableName = "marcacoes")
data class MarcacaoLocal(
    @PrimaryKey val id: Int,
    val token: String,
    val slug: String,
    val servicoNome: String,
    val barbeiroNome: String,
    val inicio: String,
    val fim: String,
    val estado: String,
)
