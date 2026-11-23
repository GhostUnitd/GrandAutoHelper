# Grand Auto Helper — Android prototype

This is a Kotlin Android project. It is an experimental prototype, not a finished or verified game automation tool.

## Build an APK without installing Android Studio (GitHub Actions)

1. Sign in to GitHub and create a new **private** repository named `GrandAutoHelper`.
2. Extract this ZIP on your computer.
3. Upload the contents of the `GrandAutoHelper` folder to the repository root (the `.github` folder must also be uploaded).
4. Open the repository's **Actions** tab and enable Actions if GitHub asks.
5. Select **Build APK** → **Run workflow**.
6. When the run finishes successfully, open it and download the artifact named `GrandAutoHelper-debug`.
7. Extract the artifact. It contains `app-debug.apk`. Transfer it to your Android phone and install it.
8. Android may ask permission to install apps from the file manager/browser you used. Allow it only for this APK you built yourself.

## First launch

1. Open Grand Auto Helper.
2. Tap **1. Включить службу управления**.
3. In Android Accessibility settings, enable **Grand Auto Helper**.
4. Return to the app.
5. Set a delay and tap **2. Запросить захват экрана и запустить**.
6. Approve Android's screen-capture prompt.
7. Stop from the app when finished.

## Important prototype limitations

- The arrow classifier is intentionally basic and has not been validated on a live device. It may misread the prompt or tap the wrong control.
- The screen coordinates are estimated from the provided 1536×864 landscape screenshots and scaled proportionally. Different UI scaling or aspect ratios may require calibration.
- It currently taps only when the detected direction changes. A repeated same-direction prompt may need additional state handling.
- This version does not navigate the character, avoid obstacles, or perform the delivery loop.
- Android may stop screen capture when the game or system changes display state.
- Use only where automation is permitted by the game's rules. This project does not bypass anti-cheat or game protections.
