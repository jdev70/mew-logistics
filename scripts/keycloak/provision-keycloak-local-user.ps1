param(
    [string]$Username = 'mew-psm',
    [string]$PersonnelId = 'MEW-PER-0000'
)

function ConvertTo-PlainText([Security.SecureString]$SecureValue) {
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

$adminPassword = ConvertTo-PlainText (Read-Host 'Enter the local Keycloak administrator password' -AsSecureString)
$userPassword = ConvertTo-PlainText (Read-Host "Set the Keycloak password for $Username" -AsSecureString)

try {
    $tokenResponse = Invoke-RestMethod -Method Post `
        -Uri 'http://localhost:8180/realms/master/protocol/openid-connect/token' `
        -ContentType 'application/x-www-form-urlencoded' `
        -Body @{ client_id = 'admin-cli'; grant_type = 'password'; username = 'local-admin'; password = $adminPassword }
    $headers = @{ Authorization = "Bearer $($tokenResponse.access_token)" }
    $baseUri = 'http://localhost:8180/admin/realms/mew-local'
    $users = @(Invoke-RestMethod -Method Get -Uri "$baseUri/users?username=$Username&exact=true" -Headers $headers)

    if ($users.Count -eq 0) {
        Invoke-RestMethod -Method Post -Uri "$baseUri/users" -Headers $headers -ContentType 'application/json' `
            -Body (@{ username = $Username; enabled = $true; emailVerified = $true; requiredActions = @() } | ConvertTo-Json)
        $users = @(Invoke-RestMethod -Method Get -Uri "$baseUri/users?username=$Username&exact=true" -Headers $headers)
    }
    if ($users.Count -ne 1) {
        throw "Expected one Keycloak user named '$Username', found $($users.Count)."
    }

    $userId = $users[0].id
    Invoke-RestMethod -Method Put -Uri "$baseUri/users/$userId" -Headers $headers -ContentType 'application/json' `
        -Body (@{ username = $Username; enabled = $true; emailVerified = $true; requiredActions = @() } | ConvertTo-Json)
    Invoke-RestMethod -Method Put -Uri "$baseUri/users/$userId/reset-password" -Headers $headers -ContentType 'application/json' `
        -Body (@{ type = 'password'; value = $userPassword; temporary = $false } | ConvertTo-Json)

    Write-Host "Provisioned $Username. Its Keycloak subject is: $userId"
    Write-Host "Map this subject manually to $PersonnelId using the documented SQL*Plus command."
} finally {
    $adminPassword = $null
    $userPassword = $null
}
