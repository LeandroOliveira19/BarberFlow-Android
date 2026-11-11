package barberflow.cliente.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import barberflow.cliente.R
import barberflow.cliente.dados.Repositorio
import barberflow.cliente.databinding.ActivityMainBinding

/** Esqueleto da app: Toolbar + NavHostFragment (placeholder dos Fragments) + BottomNavigationView. */
class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var nav: NavController
    private lateinit var appBar: AppBarConfiguration

    private val tabs = setOf(
        R.id.inicioFragment, R.id.servicosFragment, R.id.equipaFragment, R.id.marcarFragment, R.id.marcacoesFragment,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repositorio.iniciar(this)
        if (Repositorio.prefs.slug == null) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        nav = (supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment).navController
        appBar = AppBarConfiguration(tabs)
        setupActionBarWithNavController(nav, appBar)
        binding.navInferior.setupWithNavController(nav)

        // No Início o título é o nome da barbearia
        nav.addOnDestinationChangedListener { _, destino, _ ->
            if (destino.id == R.id.inicioFragment) supportActionBar?.title = Repositorio.prefs.barbeariaNome
        }

        if (savedInstanceState == null) abrirTabDoIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        abrirTabDoIntent(intent)
    }

    /** Vindo de uma notificação: abre o tab pedido (ex.: Marcações). */
    private fun abrirTabDoIntent(i: Intent?) {
        val tab = i?.getIntExtra(EXTRA_ABRIR_TAB, 0) ?: 0
        if (tab in tabs) irPara(tab)
    }

    fun irPara(tabId: Int) {
        binding.navInferior.selectedItemId = tabId
    }

    override fun onSupportNavigateUp(): Boolean = nav.navigateUp(appBar) || super.onSupportNavigateUp()

    companion object {
        const val EXTRA_ABRIR_TAB = "abrir_tab"
    }
}
