param(
    [string]$Username = 'mew-psm',
    [string]$PersonnelId = 'MEW-PER-0000'
)

function ConvertTo-PlainText([Security.SecureString]$SecureValue) {
    $valueBstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($valueBstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($valueBstr)
    }
}

$securePassword = Read-Host "Enter the Keycloak password for $Username" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

try {
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    $tokenResponse = Invoke-RestMethod -Method Post `
        -Uri 'http://localhost:8180/realms/mew-local/protocol/openid-connect/token' `
        -ContentType 'application/x-www-form-urlencoded' `
        -Body @{ client_id = 'mew-dashboard'; grant_type = 'password'; username = $Username; password = $password }

    $encodedPayload = $tokenResponse.access_token.Split('.')[1].Replace('-', '+').Replace('_', '/')
    $encodedPayload = $encodedPayload.PadRight($encodedPayload.Length + ((4 - $encodedPayload.Length % 4) % 4), '=')
    $subject = (([Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encodedPayload))) | ConvertFrom-Json).sub
    if ([string]::IsNullOrWhiteSpace($subject)) {
        throw 'Keycloak returned a token without a subject claim.'
    }

    $sqlplus = 'C:\app\dataa\product\21c\dbhomeXE\bin\sqlplus.exe'
    $sqlScript = Join-Path $PSScriptRoot '..\oracle\map-keycloak-sub.sql'
    $oraclePassword = ConvertTo-PlainText (Read-Host 'Enter the local MEW_APP password' -AsSecureString)
    Write-Host "Mapping Keycloak subject to $PersonnelId in Oracle."
    $oracleOutput = @("CONNECT MEW_APP@localhost:1521/XEPDB1", $oraclePassword, "@$sqlScript $subject $PersonnelId") | & $sqlplus -L /nolog
    $oracleExitCode = $LASTEXITCODE
    $oracleOutput | Write-Output
    if ($oracleExitCode -ne 0 -or (($oracleOutput -join "`n") -match 'SP2-|ORA-')) {
        throw "Oracle subject mapping failed with exit code $oracleExitCode."
    }
} finally {
    $oraclePassword = $null
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}
