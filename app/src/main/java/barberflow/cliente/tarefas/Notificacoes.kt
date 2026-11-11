package barberflow.cliente.tarefas

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import barberflow.cliente.R
import barberflow.cliente.ui.MainActivity

object Notificacoes {
    const val CANAL = "marcacoes"

    /** O canal é obrigatório a partir da API 26 (o minSdk da app). */
    fun criarCanal(ctx: Context) {
        val canal = NotificationChannel(CANAL, ctx.getString(R.string.canal_marcacoes), NotificationManager.IMPORTANCE_HIGH)
            .apply { description = ctx.getString(R.string.canal_marcacoes_desc) }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun temPermissao(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun podeNotificar(ctx: Context): Boolean =
        temPermissao(ctx) && NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    /** Mostra uma notificação que, ao tocar, abre a MainActivity no tab indicado. */
    fun mostrar(ctx: Context, id: Int, titulo: String, texto: String, abrirTab: Int) {
        if (!podeNotificar(ctx)) return
        val abrir = Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ABRIR_TAB, abrirTab)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // FLAG_IMMUTABLE é obrigatória a partir da API 31
        val pi = PendingIntent.getActivity(ctx, id, abrir, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(ContextCompat.getColor(ctx, R.color.bf_dourado))
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) { /* permissão retirada entretanto */ }
    }
}
