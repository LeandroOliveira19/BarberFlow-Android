package barberflow.cliente.dados

import android.content.Context
import androidx.core.content.edit

/**
 * Estado simples da app guardado em SharedPreferences ("barberflow_prefs").
 * Cada propriedade lê com um valor por omissão e escreve com apply().
 */
class Preferencias(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences(NOME, Context.MODE_PRIVATE)

    var slug: String?
        get() = sp.getString("slug", null)
        set(v) = sp.edit { putString("slug", v) }

    var barbeariaNome: String?
        get() = sp.getString("barbearia_nome", null)
        set(v) = sp.edit { putString("barbearia_nome", v) }

    var clienteNome: String?
        get() = sp.getString("cliente_nome", null)
        set(v) = sp.edit { putString("cliente_nome", v) }

    var clienteTelemovel: String?
        get() = sp.getString("cliente_telemovel", null)
        set(v) = sp.edit { putString("cliente_telemovel", v) }

    var clienteEmail: String?
        get() = sp.getString("cliente_email", null)
        set(v) = sp.edit { putString("cliente_email", v) }

    var sessaoAtiva: Boolean
        get() = sp.getBoolean("sessao_ativa", false)
        set(v) = sp.edit { putBoolean("sessao_ativa", v) }

    var lembreteMinutos: Int
        get() = sp.getInt("lembrete_minutos", 60)
        set(v) = sp.edit { putInt("lembrete_minutos", v) }

    var pendenteMarcacaoId: Int
        get() = sp.getInt("pendente_marcacao_id", -1)
        set(v) = sp.edit { putInt("pendente_marcacao_id", v) }

    var pendenteCodigoExpiraEm: String?
        get() = sp.getString("pendente_codigo_expira_em", null)
        set(v) = sp.edit { putString("pendente_codigo_expira_em", v) }

    var pendenteToken: String?
        get() = sp.getString("pendente_token", null)
        set(v) = sp.edit { putString("pendente_token", v) }

    /** Resumo da marcação pendente (para guardar no Room quando for confirmada). */
    var pendenteResumo: String?
        get() = sp.getString("pendente_resumo", null)
        set(v) = sp.edit { putString("pendente_resumo", v) }

    fun limparPendente() = sp.edit {
        putInt("pendente_marcacao_id", -1)
        remove("pendente_codigo_expira_em")
        remove("pendente_token")
        remove("pendente_resumo")
    }

    fun limparCliente() = sp.edit {
        remove("cliente_nome"); remove("cliente_telemovel"); remove("cliente_email")
        putBoolean("sessao_ativa", false)
    }

    fun limparBarbearia() = sp.edit { remove("slug"); remove("barbearia_nome") }

    companion object { const val NOME = "barberflow_prefs" }
}
