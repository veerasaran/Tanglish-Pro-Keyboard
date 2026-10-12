# Tanglish Pro

Tanglish Pro is a standalone Android input-method starter project for Tamil, English and Tanglish typing. It includes an offline phrase translator, a manual Translate key, color themes, emoji picker, local clipboard history and editable text shortcuts.

## What is included

- Android keyboard service that can be selected as the system keyboard and used in ordinary Android apps, including messaging and social apps.
- English and Tanglish QWERTY layouts, plus a native Tamil script layout.
- A Translate button that converts the current uncommitted Tanglish phrase on-device.
- Common offline phrase patterns, including `Naan college ku poren` → `I am going to college.`
- Light, Ocean, Rose and Dark keyboard colors.
- Emoji picker and a local clipboard list (up to 30 text entries).
- Editable local text shortcuts; type a shortcut and press space to expand it.
- A GitHub Actions workflow that builds a debug APK in the cloud.

## Important translation limit

Translation is a small phrase/rule engine contained in `OfflineTranslator.kt`. It works offline and handles the included patterns, but it is not a general-purpose machine-translation model. For a phrase that has no matching rule, it currently preserves the typed words and adds sentence punctuation. Add reviewed phrase patterns to that file to extend coverage. This keeps the keyboard from sending typed text to a translation service.

## Privacy

The app requests no `INTERNET` permission and has no analytics or network dependency. Keyboard input used for translation is held in the keyboard service while the current phrase is being typed; translation runs locally. Shortcuts and clipboard history are stored in app-private preferences on the phone. Clipboard history reads and saves the current clipboard only when the user taps the clipboard button, and keeps up to 30 entries. Android itself may show its standard clipboard access notice.

## Build an APK in a browser (GitHub Actions)

You do not need Android Studio on your computer. GitHub Actions runs the Gradle/Android build on a hosted runner and gives you the APK as a downloadable artifact.

1. Download and extract `TanglishPro.zip` on a computer.
2. Sign in to GitHub and create a new repository. A private repository is fine.
3. On the repository page, choose **Add file → Upload files**. Upload the extracted project contents, including the hidden `.github` folder and its `workflows/android.yml` file, then commit the upload to `main`.
4. Open the repository's **Actions** tab and select **Build Tanglish Pro APK**. If the workflow has not run yet, choose **Run workflow**.
5. When the run finishes, open it and download the `tanglish-pro-debug-apk` artifact. Extract the downloaded artifact ZIP to get `app-debug.apk`.
6. Transfer `app-debug.apk` to your Android phone, open it, and allow installation from that source if Android asks. Then open Tanglish Pro, enable it in keyboard settings, and select it from the input-method picker.

The first build downloads the Android Gradle plugin and Android SDK components to GitHub's build runner. Review GitHub's repository and Actions settings before uploading if you do not want the source project stored there. The debug APK is suitable for installing on your own phone; distributing it publicly requires a release build signed with a key you control.

### If GitHub asks you to create the workflow manually

The `.github/workflows/android.yml` file is part of this project. It must be committed at that exact path in the repository root. Ensure your upload includes hidden files; otherwise create `.github/workflows` in GitHub's web editor and copy the contents of the included file there.

## Build locally (optional)

With JDK 17, Android SDK API 35, and Gradle 8.9 installed, run this from the project root:

```text
gradle assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`.

## Install and enable the keyboard

Install the APK, launch **Tanglish Pro**, tap **1. Enable keyboard**, enable Tanglish Pro in Android's keyboard settings, then tap **2. Choose Tanglish Pro**. In a text field, use the toolbar to switch among **EN**, **அ**, and **Ta**. Tap **Translate** to convert the current Tanglish phrase. The keyboard is a normal Android input method, so Android apps that accept third-party keyboards can use it.

## FlorisBoard reference and licensing

FlorisBoard was used only as a product and feature reference: https://github.com/florisboard/florisboard. Its repository identifies the project as Apache License 2.0. This starter project is an independent implementation and does not copy FlorisBoard source files, assets, package names or branding. No FlorisBoard dependency is included. If you later copy or adapt FlorisBoard code, comply with Apache-2.0: include the license and relevant notices, retain applicable copyright/patent/trademark/attribution notices, mark modified files, and do not imply endorsement. See `LICENSES.md`.

## Project structure

```text
TanglishPro/
├── .github/workflows/android.yml  # Cloud APK build
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/tanglishpro/
│       │   ├── MainActivity.kt
│       │   ├── core/LocalStore.kt
│       │   ├── core/OfflineTranslator.kt
│       │   └── keyboard/TanglishInputMethod.kt
│       └── res/                    # Settings layout, strings, theme, IME metadata, icon
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```
