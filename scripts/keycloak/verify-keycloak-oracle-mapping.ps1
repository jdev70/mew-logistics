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

$adminPassword = ConvertTo-PlainText (Read-Host 'Enter the local Keycloak administrator password' -AsSecureString)
try {
    $tokenResponse = Invoke-RestMethod -Method Post `
        -Uri 'http://localhost:8180/realms/master/protocol/openid-connect/token' `
        -ContentType 'application/x-www-form-urlencoded' `
        -Body @{ client_id = 'admin-cli'; grant_type = 'password'; username = 'local-admin'; password = $adminPassword }
    $headers = @{ Authorization = "Bearer $($tokenResponse.access_token)" }
    $userResponse = Invoke-WebRequest -Method Get `
        -Uri "http://localhost:8180/admin/realms/mew-local/users?username=$Username&exact=true" -Headers $headers
    $users = [System.Collections.Generic.List[object]]::new()
    foreach ($user in ($userResponse.Content | ConvertFrom-Json)) {
        $users.Add($user)
    }
    if ($users.Count -ne 1) {
        throw "Expected one Keycloak user named '$Username', found $($users.Count)."
    }

    $subject = $users[0].id
    $sqlPlus = 'C:\app\dataa\product\21c\dbhomeXE\bin\sqlplus.exe'
    $sqlScript = Join-Path $PSScriptRoot '..\oracle\verify-keycloak-subject.sql'
    if (-not (Test-Path $sqlPlus)) {
        throw "SQL*Plus was not found at $sqlPlus. Update the local path in this verifier if Oracle XE is installed elsewhere."
    }

    Write-Host "Keycloak subject: $subject"
    Write-Host "SQL*Plus will now prompt for the local MEW_APP password. This check is read-only."
    $oracleOutput = & $sqlPlus -L -S 'MEW_APP@localhost:1521/XEPDB1' "@$sqlScript" $subject $PersonnelId 2>&1
    $oracleText = ($oracleOutput | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $oracleText -match '(^|\r?\n)(ORA-|SP2-)') {
        throw "Oracle mapping verification failed.`n$oracleText"
    }

    Write-Host $oracleText
    if ($oracleText -notmatch 'MAPPING_STATUS=MATCH') {
        throw "Keycloak subject and Oracle authorization assignment do not match. Follow docs/LOCAL_KEYCLOAK_ORACLE_MAPPING.md to repair the mapping."
    }
    Write-Host "Verified: Keycloak user '$Username' is mapped to Oracle personnel '$PersonnelId'." -ForegroundColor Green
} finally {
    $adminPassword = $null
}
