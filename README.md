# BarberFlow — app Android (cliente)

App nativa em Kotlin para os clientes de uma barbearia BarberFlow: escolher a barbearia, ver serviços/equipa/reviews, marcar com código de confirmação por email, ver as marcações e receber lembretes.
Fala com a API JSON do backend PHP (`api/v1` no repositório web, branch `api-v1`).

## Como correr (emulador, contra a API local)

1. **XAMPP:** liga o **MySQL** no painel do XAMPP (o Apache não é preciso).
2. **API:** faz duplo clique em [`iniciar-api-local.bat`](iniciar-api-local.bat) e deixa a janela aberta.
   Testa no browser: <http://127.0.0.1:8080/projeto/projeto/api/v1/index.php?r=barbearias/rlbarber> → deve mostrar JSON.
3. **Android Studio:** *File → Open* → `C:\projetosAndroid\projeto`. Espera pelo *Gradle sync*.
4. Em *Build Variants* confirma **`localDebug`**, escolhe o emulador **Medium_Phone** e carrega em **▶ Run**.
5. Na app escreve o código **`rlbarber`**.
   No flavor `local` o código de confirmação aparece no ecrã (linha azul "modo desenvolvimento"), por isso não precisas de email real.

**Sem Android Studio:** `apk\BarberFlow-local.apk` pode ser arrastado para a janela do emulador (instala sozinho).

### Flavors

| Flavor | Servidor | Notas |
|---|---|---|
| `local` | `http://10.0.2.2:8080/projeto/projeto/api/v1/` | 10.0.2.2 = o PC visto pelo emulador. Mostra o código de dev. |
| `usb` | `http://127.0.0.1:8080/projeto/projeto/api/v1/` | Telemóvel por cabo: `adb reverse tcp:8080 tcp:8080`. Mostra o código de dev. |
| `ua` | `https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g9/projeto/api/v1/` | Versão para partilhar (`apk\BarberFlow.apk`). Precisa da API publicada na UA (`deploy-ua\LER-PRIMEIRO.md`). Email real. |

Para dar a app a outras pessoas, ver [COMO-INSTALAR-A-APP.md](COMO-INSTALAR-A-APP.md).

Para um **telemóvel físico** com o flavor `local`: põe o IP do PC no `BASE_URL` (app/build.gradle.kts) e em `app/src/local/res/xml/network_security_config.xml`, e arranca o `php -S 0.0.0.0:8080 ...`.

## Testes

```bash
./gradlew testLocalDebugUnitTest            # 32 testes JVM (DTOs/@SerializedName, erros, formatos, validação, fuso horário)
./gradlew connectedLocalDebugAndroidTest    # 3 testes do Room (precisa do emulador ligado)
```

## Conceitos da UC → onde estão

| Conceito | Ficheiro |
|---|---|
| ViewBinding (Activity `by lazy` / Fragment `bind`) | `ui/MainActivity.kt`, todos os `*Fragment.kt` |
| BottomNavigationView + Fragments + NavController | `ui/MainActivity.kt`, `res/navigation/nav_graph.xml`, `res/menu/menu_inferior.xml` |
| Intent explícito / implícito (DIAL, SENDTO, VIEW, deep link) | `ui/OnboardingActivity.kt`, `ui/inicio/InicioFragment.kt`, `ui/equipa/BarbeiroDetalheFragment.kt` |
| RecyclerView + Adapter + ViewHolder (Linear e Grid) | `ui/servicos`, `ui/equipa`, `ui/marcar/MarcarFragment.kt` (SlotsAdapter), `ui/marcacoes` |
| Spinner + onItemSelectedListener + setImageResource | `ui/marcar/MarcarFragment.kt`, `ui/inicio/InicioFragment.kt` |
| DatePickerDialog | `ui/marcar/MarcarFragment.kt` |
| Retrofit + Gson + `@SerializedName` + `suspend`/`lifecycleScope` | `dados/api/*`, `dados/Repositorio.kt` |
| Glide | `ui/equipa/EquipaFragment.kt` (`carregarFoto`) |
| ConnectivityManager | `dados/Rede.kt` |
| SharedPreferences | `dados/Preferencias.kt` |
| Room | `dados/local/*` |
| BroadcastReceiver estático (manifesto) | `tarefas/Receivers.kt` (lembrete, código expirado, BOOT_COMPLETED) |
| BroadcastReceiver em runtime + broadcast local | `tarefas/Eventos.kt`, `InicioFragment`, `MarcacoesFragment`, `VerificarFragment` |
| AlarmManager (exato / setWindow / execução única) | `tarefas/Lembretes.kt` |
| WorkManager (periódico, Constraints, unique work) | `tarefas/SincronizarWorker.kt` |
| Notificações (canal, PendingIntent IMMUTABLE, POST_NOTIFICATIONS runtime) | `tarefas/Notificacoes.kt`, `ui/marcar/VerificarFragment.kt` |
| Animações (animate, ObjectAnimator, AnimatorSet, shared element) | `ui/Ui.kt`, `MarcarFragment` (slots), `VerificarFragment` (✓), Equipa → detalhe |
| i18n e qualificadores | `res/values` (PT), `res/values-en`, `res/values-land/integers.xml` |
