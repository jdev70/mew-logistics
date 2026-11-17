param(
    [string]$Username = 'mew-psm'
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
    $users = @(Invoke-RestMethod -Method Get `
        -Uri "http://localhost:8180/admin/realms/mew-local/users?username=$Username&exact=true" -Headers $headers)
    if ($users.Count -ne 1) {
        throw "Expected one Keycloak user named '$Username', found $($users.Count)."
    }
    Write-Output $users[0].id
} finally {
    $adminPassword = $null
}
