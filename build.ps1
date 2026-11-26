$ErrorActionPreference='Stop'
if (Test-Path 'C:\Program Files\Java\jdk-21.0.11\bin\javac.exe') { $env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH" }
$base=$PSScriptRoot
$sdk="$env:LOCALAPPDATA\Android\Sdk"
$bt="$sdk\build-tools\36.1.0"
$android="$sdk\platforms\android-35\android.jar"
New-Item -ItemType Directory -Force "$base\build\classes","$base\build\dex" | Out-Null
& "$bt\aapt.exe" package -f -m -J "$base\build" -M "$base\AndroidManifest.xml" -S "$base\res" -A "$base\assets" -I $android -F "$base\build\app-unsigned.apk"
if($LASTEXITCODE -ne 0){throw 'aapt failed'}
Get-ChildItem -LiteralPath "$base\build\classes\ro\zilefrumoase" -Filter '*.class' -File -ErrorAction SilentlyContinue | Remove-Item
& javac -encoding UTF-8 --release 8 -classpath $android -d "$base\build\classes" "$base\src\ro\partylist\MainActivity.java" "$base\src\ro\partylist\PlannedNotifications.java" "$base\src\ro\partylist\Boot.java" "$base\src\ro\partylist\L10n.java"
if($LASTEXITCODE -ne 0){throw 'javac failed'}
$classes=Get-ChildItem "$base\build\classes" -Recurse -Filter '*.class' | ForEach-Object {$_.FullName}
& "$bt\d8.bat" --min-api 28 --lib $android --output "$base\build\dex" $classes
if($LASTEXITCODE -ne 0){throw 'd8 failed'}
Push-Location "$base\build\dex"
& "$bt\aapt.exe" add "$base\build\app-unsigned.apk" classes.dex
Pop-Location
& "$bt\zipalign.exe" -f 4 "$base\build\app-unsigned.apk" "$base\build\app-aligned.apk"
Write-Host "Unsigned APK created at build/app-aligned.apk. Sign it with your own key before installing."
