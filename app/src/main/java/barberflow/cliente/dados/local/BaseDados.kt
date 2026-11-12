package barberflow.cliente.dados.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room: abstração sobre SQLite para dados estruturados privados da app. */
@Database(entities = [MarcacaoLocal::class], version = 3, exportSchema = false)
abstract class BaseDados : RoomDatabase() {
    abstract fun marcacoes(): MarcacaoDao

    companion object {
        /** v1 → v2: coluna "paraNome" (marcação feita para outra pessoa). Mantém as marcações e tokens já guardados. */
        val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE marcacoes ADD COLUMN paraNome TEXT")
            }
        }

        /** v2 → v3: serviço e barbeiro de cada marcação (para remarcar). As marcações antigas ficam com null até sincronizar. */
        val MIGRACAO_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE marcacoes ADD COLUMN servicoId INTEGER")
                db.execSQL("ALTER TABLE marcacoes ADD COLUMN barbeiroId INTEGER")
            }
        }

        @Volatile private var instancia: BaseDados? = null

        fun obter(ctx: Context): BaseDados = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(ctx.applicationContext, BaseDados::class.java, "barberflow.db")
                .addMigrations(MIGRACAO_1_2, MIGRACAO_2_3)
                .build().also { instancia = it }
        }
    }
}
