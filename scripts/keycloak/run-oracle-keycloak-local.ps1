param()

$securePassword = Read-Host 'Enter the local MEW_APP password' -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

try {
    $env:MEW_ORACLE_LOCAL_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    Push-Location (Join-Path $PSScriptRoot '..\..\backend-java')
    try {
        mvn spring-boot:run "-Dspring-boot.run.profiles=oracle-local,keycloak-local"
    } finally {
        Pop-Location
    }
} finally {
    Remove-Item Env:MEW_ORACLE_LOCAL_PASSWORD -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}
