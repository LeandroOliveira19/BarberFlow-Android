package barberflow.cliente.dados.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Room: abstração sobre SQLite para dados estruturados privados da app. */
@Database(entities = [MarcacaoLocal::class], version = 1, exportSchema = false)
abstract class BaseDados : RoomDatabase() {
    abstract fun marcacoes(): MarcacaoDao

    companion object {
        @Volatile private var instancia: BaseDados? = null

        fun obter(ctx: Context): BaseDados = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(ctx.applicationContext, BaseDados::class.java, "barberflow.db")
                .build().also { instancia = it }
        }
    }
}
