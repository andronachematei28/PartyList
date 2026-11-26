# Building PartyList

The current build script targets Windows and requires a JDK with `javac`, Android SDK Platform 35, and Android SDK Build Tools 36.1.0. Set `LOCALAPPDATA` to the Windows user profile containing `Android\Sdk`, or adjust the SDK path in `build.ps1`.

Run `./build.ps1` in PowerShell. The script creates `build/app-aligned.apk`, an unsigned APK. Sign it with your own Android signing key before installing. Keep your keystore and passwords outside this repository. An APK signed with a different key cannot update an existing installation signed with another key.
