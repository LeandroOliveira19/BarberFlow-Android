# Como instalar a app RLBarber (Android)

Ficheiro a enviar: **`apk\RLBarber.apk`** (por WhatsApp, email ou Google Drive).

## Passos para quem vai testar

1. Abre a mensagem e toca no ficheiro **RLBarber.apk** (se preciso, descarrega-o primeiro).
2. O telemóvel avisa que não pode instalar apps desta origem → toca em **Definições** → ativa **Permitir desta origem** → volta atrás.
3. Toca em **Instalar**. Se aparecer um aviso do Play Protect, toca em **Instalar mesmo assim** (é normal em apps que não vêm da Play Store).
4. Abre a app **RLBarber**: entra logo na barbearia. Vai a **Marcar**, escolhe serviço, barbeiro, dia e hora.

O código de confirmação chega por email. No servidor da UA o email não sai, por isso o código aparece no próprio ecrã.

## Notas para ti (programador)

- `RLBarber.apk` = flavor `ua`: servidor da UA, barbearia fixa (`SLUG_FIXO = "rlbarber"` em `app/build.gradle.kts`).
  Para outra barbearia: muda o `SLUG_FIXO` e o nome em `app/src/ua/res/values/strings.xml` e gera outro APK.
- Os outros APKs são só para desenvolvimento e perguntam a barbearia:
  - `BarberFlow-dev-emulador.apk`: emulador + `iniciar-api-local.bat`.
  - `BarberFlow-dev-usb.apk`: telemóvel por cabo, com depuração USB e `adb reverse tcp:8080 tcp:8080`.
- Para atualizar a app: envia o novo `RLBarber.apk` e instala por cima. Os dados ficam.
