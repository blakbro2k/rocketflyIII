#requires -Version 5.1
$ErrorActionPreference = 'Stop'
$base = $env:FIREFLY_BASE_URL
if ([string]::IsNullOrWhiteSpace($base)) {
    $base = Read-Host 'Firefly installation URL (include https:// and any installation subpath)'
}
$endpoint = $base.Trim().TrimEnd('/') + '/api/v1/accounts?type=asset'
$uri = $null
if (-not [Uri]::TryCreate($endpoint, [UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -notin 'http','https' -or $uri.UserInfo) {
    throw 'Enter a valid HTTP or HTTPS installation URL without credentials.'
}
$token = $env:FIREFLY_API_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    $secret = Read-Host 'Firefly personal access token (input hidden)' -AsSecureString
    $token = [System.Net.NetworkCredential]::new('', $secret).Password
}
if ([string]::IsNullOrWhiteSpace($token)) { throw 'An API token is required.' }
Add-Type -AssemblyName System.Net.Http
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.AllowAutoRedirect = $false
$client = [System.Net.Http.HttpClient]::new($handler)
try {
    $client.Timeout = [TimeSpan]::FromSeconds(30)
    $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $token.Trim())
    $client.DefaultRequestHeaders.Accept.ParseAdd('application/json')
    $response = $client.GetAsync($uri).GetAwaiter().GetResult()
    $media = $response.Content.Headers.ContentType.MediaType
    "HTTP $([int]$response.StatusCode) | Content-Type: $media"
    if ($response.IsSuccessStatusCode -and $media -match 'json') {
        try {
            $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
        } catch {
            'Response body could not be parsed as JSON.'
            return
        }
        "data is an array: $($body.data -is [array])"
        foreach ($field in 'total','count','per_page','current_page','total_pages') {
            $value = $body.meta.pagination.$field
            if ($value -is [int] -or $value -is [long]) { "${field}: $value" }
            else { "${field}: missing or non-integer" }
        }
    }
} catch {
    "Request failed: $($_.Exception.GetBaseException().GetType().Name)"
} finally {
    if ($response) { $response.Dispose() }
    $client.Dispose()
}
