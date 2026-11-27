# LayarCerdas AI (Android starter)

Android floating bubble + user-triggered screen capture/OCR + AI explanation using an OpenAI-compatible chat-completions endpoint. This starter is a learning/prototype project, not a production security or privacy product.

## Build in Termux
1. Install Termux from a reputable source and run `bash termux-setup.sh`.
2. Place/extract this folder at `~/LayarCerdas`.
3. Run `bash build-termux.sh`.
4. APK path: `app/build/outputs/apk/debug/app-debug.apk`.

If Android SDK packages are not available in your Termux environment, use GitHub Actions: upload this folder to a GitHub repository and trigger **Android APK Build** in Actions. Download the debug APK from the run's artifacts.

## First run
- Launch the app; grant Display over other apps permission for the floating bubble.
- Tap Capture OCR and approve Android screen-sharing prompt.
- Review/edit OCR text, enter an instruction, configure an OpenAI-compatible endpoint/model/API key, save, then press Ask AI.
- API keys are stored in ordinary local SharedPreferences in this prototype (not encrypted); do not use sensitive keys. This should be upgraded to encrypted storage before release.

## Limitations
- Floating overlay currently opens the app panel; bubble is draggable.
- OCR is single-frame and user-triggered; no continuous screen monitoring.
- No automatic insertion or submission of answers in other apps.
- AI call requires your own API endpoint and key and sends OCR text only when you press Ask AI.
- Release signing is not configured. GitHub Actions currently builds a debug APK artifact.
