# Como instalar a app BarberFlow (Android)

Ficheiro a enviar: **`apk\BarberFlow.apk`** (por WhatsApp, email ou Google Drive).

## Passos para quem vai testar

1. Abre a mensagem e toca no ficheiro **BarberFlow.apk** (se preciso, descarrega-o primeiro).
2. O telemóvel avisa que não pode instalar apps desta origem → toca em **Definições** → ativa **Permitir desta origem** → volta atrás.
3. Toca em **Instalar**. Se aparecer um aviso do Play Protect, toca em **Instalar mesmo assim** (é normal em apps que não vêm da Play Store).
4. Abre a app **BarberFlow** e escreve o código da barbearia: **rlbarber**.

Para marcar, usa um **email verdadeiro**: o código de confirmação chega por email.

## Notas para ti (programador)

- A app `BarberFlow.apk` usa o servidor da UA. Só funciona depois de a API estar publicada lá (ver `deploy-ua\LER-PRIMEIRO.md`).
- Os outros APKs são só para desenvolvimento:
  - `BarberFlow-dev-emulador.apk`: emulador + `iniciar-api-local.bat`.
  - `BarberFlow-dev-usb.apk`: telemóvel por cabo, com depuração USB e `adb reverse tcp:8080 tcp:8080`.
- Para atualizar a app: envia o novo `BarberFlow.apk` e instala por cima. Os dados ficam.
