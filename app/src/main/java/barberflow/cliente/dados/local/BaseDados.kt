package barberflow.cliente.dados.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room: abstração sobre SQLite para dados estruturados privados da app. */
@Database(entities = [MarcacaoLocal::class], version = 2, exportSchema = false)
abstract class BaseDados : RoomDatabase() {
    abstract fun marcacoes(): MarcacaoDao

    companion object {
        /** v1 → v2: coluna "paraNome" (marcação feita para outra pessoa). Mantém as marcações e tokens já guardados. */
        val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE marcacoes ADD COLUMN paraNome TEXT")
            }
        }

        @Volatile private var instancia: BaseDados? = null

        fun obter(ctx: Context): BaseDados = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(ctx.applicationContext, BaseDados::class.java, "barberflow.db")
                .addMigrations(MIGRACAO_1_2)
                .build().also { instancia = it }
        }
    }
}
