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
    /** Para quem é a marcação (null = para o próprio cliente). Coluna nova na versão 2 da BD. */
    val paraNome: String? = null,
)
