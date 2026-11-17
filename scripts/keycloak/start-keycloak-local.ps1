param()

$securePassword = Read-Host 'Create a local Keycloak administrator password' -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

try {
    $env:KEYCLOAK_ADMIN_USERNAME = 'local-admin'
    $env:KEYCLOAK_ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    Push-Location (Join-Path $PSScriptRoot '..\..\identity\keycloak')
    try {
        docker compose up -d
    } finally {
        Pop-Location
    }
} finally {
    Remove-Item Env:KEYCLOAK_ADMIN_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:KEYCLOAK_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}
