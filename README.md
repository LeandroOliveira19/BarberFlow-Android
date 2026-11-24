# BarberFlow — app Android (cliente)

App nativa em Kotlin para os clientes de uma barbearia BarberFlow: escolher a barbearia, ver serviços/equipa/reviews, marcar com código de confirmação por email, ver as marcações, cancelar/remarcar e receber lembretes.
Fala com a API JSON do backend PHP: `api/v1` no repositório web **[LeandroOliveira19/BarberFlow](https://github.com/LeandroOliveira19/BarberFlow)**.

**UC:** Programação Mobile · **ESAN — Universidade de Aveiro** · **Grupo 09**

## Para o professor

- **Correr:** abrir o projeto no Android Studio, variante **`uaDebug`** (usa o servidor da UA, não precisa de XAMPP) → ▶ Run. A barbearia já vem fixa (RL Barber).
- **"Login" do cliente:** a app não usa senha. O cliente identifica-se ao fazer a 1.ª marcação (nome, telemóvel, email) e confirma com um **código de 6 dígitos**. No servidor da UA o email não sai, por isso o código aparece no ecrã. A sessão fica guardada (SharedPreferences + Room) e o botão **Terminar sessão** no separador Marcações faz o logout.
- **Utilizador de teste:** qualquer nome/telemóvel/email válidos servem (ex.: `Professor Teste`, `912345678`, `professor@example.com`); a partir daí a app lembra-se dele. As contas com senha (gestor/barbeiro) são da área web, ver o README do repositório web.
- **Testes unitários:** `./gradlew testLocalDebugUnitTest` (55 testes).

## Como correr (emulador, contra a API local)

1. **XAMPP:** liga o **MySQL** no painel do XAMPP (o Apache não é preciso).
2. **API:** faz duplo clique em [`iniciar-api-local.bat`](iniciar-api-local.bat) e deixa a janela aberta.
   Testa no browser: <http://127.0.0.1:8080/projeto/projeto/api/v1/index.php?r=barbearias/rlbarber> → deve mostrar JSON.
3. **Android Studio:** *File → Open* → `C:\projetosAndroid\projeto`. Espera pelo *Gradle sync*.
4. Em *Build Variants* confirma **`localDebug`**, escolhe o emulador **Medium_Phone** e carrega em **▶ Run**.
5. Na app escreve o código **`rlbarber`**.
   No flavor `local` o código de confirmação aparece no ecrã (linha azul "modo desenvolvimento"), por isso não precisas de email real.

**Sem Android Studio:** `apk\BarberFlow-dev-emulador.apk` pode ser arrastado para a janela do emulador (instala sozinho).

### Flavors

| Flavor | Servidor | Notas |
|---|---|---|
| `local` | `http://10.0.2.2:8080/projeto/projeto/api/v1/` | 10.0.2.2 = o PC visto pelo emulador. Mostra o código de dev. |
| `usb` | `http://127.0.0.1:8080/projeto/projeto/api/v1/` | Telemóvel por cabo: `adb reverse tcp:8080 tcp:8080`. Mostra o código de dev. |
| `ua` | `https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g9/projeto/api/v1/` | Versão para partilhar (`apk\RLBarber.apk`), já fixa na RL Barber (`SLUG_FIXO`). Precisa da API publicada na UA (`deploy-ua\LER-PRIMEIRO.md`). Se o email não sair, o código aparece no ecrã (`api_codigo_se_email_falhar`). |

Para dar a app a outras pessoas, ver [COMO-INSTALAR-A-APP.md](COMO-INSTALAR-A-APP.md).

Para um **telemóvel físico** com o flavor `local`: põe o IP do PC no `BASE_URL` (app/build.gradle.kts) e em `app/src/local/res/xml/network_security_config.xml`, e arranca o `php -S 0.0.0.0:8080 ...`.

## Cancelar e remarcar

No separador Marcações, uma marcação confirmada mostra Cancelar e Remarcar até X horas antes
(X = "Cancelar/remarcar na app até" nas Definições da gestão; 24 h por omissão). Fora do prazo aparece
"Para alterar, contacta a barbearia".

- **Cancelar:** diálogo de confirmação → `POST marcacoes/{id}/cancelar` → Room atualizado e lembrete cancelado.
- **Remarcar:** abre o separador Marcar em modo remarcar (serviço fixo; escolhe barbeiro, dia e hora; os horários
  ignoram a própria marcação) → `POST marcacoes/{id}/remarcar` → Room atualizado e lembrete reagendado.
- A prova de que a marcação é do cliente é o token que a app já guarda; o servidor valida sempre o prazo.
- O Room está na versão 3 (`servicoId`, `barbeiroId`), com migração a partir da v2.

## Testes

```bash
./gradlew testLocalDebugUnitTest            # 67 testes JVM (DTOs/@SerializedName, erros, formatos, validação, fuso horário, prazo de cancelar/remarcar, efeito palavra a palavra, apresentação dos ecrãs)
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
| Animações (animate, ObjectAnimator, AnimatorSet, ValueAnimator, shared element) | `ui/Ui.kt`, `MarcarFragment` (slots), `VerificarFragment` (✓), Equipa → detalhe, `ui/Efeitos.kt` (palavra a palavra) |
| Vista própria (Canvas, RadialGradient) | `ui/GradienteAnimadoView.kt` (fumo dourado do Início, parado se as animações do sistema estiverem desligadas) |
| Tema, estilos e fontes em `res/font` | `res/values/themes.xml`, `res/values/colors.xml`, `res/font/` (o mesmo visual do site e da gestão) |
| i18n e qualificadores | `res/values` (PT), `res/values-en`, `res/values-land/integers.xml` |

## Visual e créditos

A app usa o mesmo visual do site (tema Clássico) e da gestão do BarberFlow:
- preto, branco-pérola (`#F5F5F7`) e dourado (`#CDA47C`);
- cantos de 24dp e botões em pílula;
- fumo dourado animado e texto palavra a palavra no Início.

Fontes incluídas em `app/src/main/res/font/`, ambas sob a [SIL Open Font License 1.1](https://openfontlicense.org), descarregadas do Google Fonts:
- **Inter**, de Rasmus Andersson;
- **Cinzel**, de Natanael Gama.
