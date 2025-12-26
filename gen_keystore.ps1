# gen_keystore.ps1
# Script to create a new Keystore and generate Base64 string for GitHub Actions

$password = "RakutenMonitor2025"
$alias = "key0"

Write-Host "Creating new release.jks..."
if (Test-Path "release.jks") { Remove-Item "release.jks" }

# Find keytool
$keytool = "keytool"
if ($env:JAVA_HOME) {
    if (Test-Path "$env:JAVA_HOME\bin\keytool.exe") {
        $keytool = "$env:JAVA_HOME\bin\keytool.exe"
    }
}

# Fallback: Check Android Studio default location
if (-not (Get-Command $keytool -ErrorAction SilentlyContinue)) {
    $androidKeytool = "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
    if (Test-Path $androidKeytool) {
        $keytool = $androidKeytool
    }
}

& $keytool -genkeypair -v `
  -keystore release.jks `
  -alias $alias `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -storepass $password `
  -keypass $password `
  -dname "CN=RakutenMonitor, OU=Dev, O=Personal, L=Tokyo, ST=Tokyo, C=JP"

if (-not (Test-Path "release.jks")) {
    Write-Error "Failed to create release.jks"
    exit 1
}

Write-Host "Encoding to Base64..."
$bytes = [System.IO.File]::ReadAllBytes("release.jks")
$base64 = [Convert]::ToBase64String($bytes)

$base64 | Out-File -FilePath "keystore_base64.txt" -Encoding ascii

Write-Host ""
Write-Host "==============================================="
Write-Host "SUCCESS: Keystore created."
Write-Host "==============================================="
Write-Host "Please update your GitHub Repository Secrets:"
Write-Host "Settings > Secrets and variables > Actions"
Write-Host ""
Write-Host "1. ANDROID_KEYSTORE_BASE64 : Copy content of 'keystore_base64.txt'"
Write-Host "2. ANDROID_KEYSTORE_PASSWORD : $password"
Write-Host "3. ANDROID_KEY_PASSWORD      : $password"
Write-Host "4. ANDROID_KEY_ALIAS         : $alias"
Write-Host "==============================================="
