package barberflow.cliente.dados

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** ConnectivityManager: verifica se há uma rede ativa com acesso à internet. */
fun temInternet(ctx: Context): Boolean {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
