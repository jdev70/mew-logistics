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

function Get-JwtSubject([string]$Token) {
    $payload = $Token.Split('.')[1].Replace('-', '+').Replace('_', '/')
    switch ($payload.Length % 4) {
        2 { $payload += '==' }
        3 { $payload += '=' }
    }
    $claims = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload)) | ConvertFrom-Json
    return $claims.sub
}

$password = ConvertTo-PlainText (Read-Host "Enter the Keycloak password for $Username" -AsSecureString)
try {
    $tokenResponse = Invoke-RestMethod -Method Post `
        -Uri 'http://localhost:8180/realms/mew-local/protocol/openid-connect/token' `
        -ContentType 'application/x-www-form-urlencoded' `
        -Body @{ client_id = 'mew-dashboard'; grant_type = 'password'; username = $Username; password = $password }
    $jwtSubject = Get-JwtSubject $tokenResponse.access_token
    Write-Host "JWT subject: $jwtSubject"
    $headers = @{ Authorization = "Bearer $($tokenResponse.access_token)" }
    $context = Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/v1/authorization/context/' -Headers $headers
    $assets = @(Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/v1/assets/' -Headers $headers)

    [PSCustomObject]@{
        AuthenticatedSubject = $context.active_context.persona_id
        Actor = $context.active_context.actor_name
        AssetCount = $assets.Count
    } | Format-List
} finally {
    $password = $null
}
