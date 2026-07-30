[CmdletBinding()]
param(
    [string]$Repository = "yadielglz/glz-tv",
    [string]$Environment = "production",
    [string]$KeystorePath = (Join-Path ([Environment]::GetFolderPath("MyDocuments")) "GLZ Signing\glz-tv-release.jks")
)

$ErrorActionPreference = "Stop"
$alias = "glztv-release"
$distinguishedName = "CN=GLZ TV, O=GLZTech, C=US"

if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    throw "keytool was not found. Install JDK 17 before running this script."
}
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI was not found. Install it and run 'gh auth login'."
}

gh auth status | Out-Null

$parent = Split-Path -Parent $KeystorePath
New-Item -ItemType Directory -Force -Path $parent | Out-Null
if (Test-Path -LiteralPath $KeystorePath) {
    throw "A keystore already exists at '$KeystorePath'. It was not overwritten."
}

$storePasswordSecure = Read-Host "Create the permanent keystore password" -AsSecureString
$keyPasswordSecure = Read-Host "Create the permanent key password" -AsSecureString
$storePassword = [System.Net.NetworkCredential]::new("", $storePasswordSecure).Password
$keyPassword = [System.Net.NetworkCredential]::new("", $keyPasswordSecure).Password
if ($storePassword.Length -lt 12 -or $keyPassword.Length -lt 12) {
    throw "Both passwords must contain at least 12 characters."
}

try {
    & keytool -genkeypair `
        -keystore $KeystorePath `
        -storetype JKS `
        -storepass $storePassword `
        -alias $alias `
        -keypass $keyPassword `
        -keyalg RSA `
        -keysize 4096 `
        -validity 10958 `
        -dname $distinguishedName
    if ($LASTEXITCODE -ne 0) { throw "keytool failed." }

    gh api --method PUT "repos/$Repository/environments/$Environment" | Out-Null
    $keystoreBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($KeystorePath))
    $keystoreBase64 | gh secret set ANDROID_SIGNING_KEY_BASE64 --repo $Repository --env $Environment
    $storePassword | gh secret set ANDROID_KEYSTORE_PASSWORD --repo $Repository --env $Environment
    $alias | gh secret set ANDROID_KEY_ALIAS --repo $Repository --env $Environment
    $keyPassword | gh secret set ANDROID_KEY_PASSWORD --repo $Repository --env $Environment

    Write-Host ""
    Write-Host "Signing is configured for $Repository ($Environment)." -ForegroundColor Green
    Write-Host "Permanent keystore: $KeystorePath"
    Write-Host "Key alias: $alias"
    Write-Host "Back up this keystore and both passwords offline before publishing a release."
}
finally {
    $storePassword = $null
    $keyPassword = $null
    $keystoreBase64 = $null
}
