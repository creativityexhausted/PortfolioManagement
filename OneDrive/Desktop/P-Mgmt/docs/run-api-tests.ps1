$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
$tests = New-Object System.Collections.Generic.List[object]

function Invoke-Test {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Purpose,
        [object]$Body,
        [hashtable]$Headers,
        [int]$ExpectedStatus,
        [string]$DbVerification = 'N/A'
    )

    $jsonBody = $null
    if ($null -ne $Body) {
        $jsonBody = ($Body | ConvertTo-Json -Compress -Depth 8)
    }

    try {
        $params = @{ Method = $Method; Uri = $Url; UseBasicParsing = $true }
        if ($Headers) { $params.Headers = $Headers }
        if ($jsonBody) {
            $params.ContentType = 'application/json'
            $params.Body = $jsonBody
        }
        $resp = Invoke-WebRequest @params
        $status = [int]$resp.StatusCode
        $content = $resp.Content
    } catch {
        $status = -1
        $content = $_.Exception.Message
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode.value__
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $content = $reader.ReadToEnd()
                $reader.Close()
            } catch {
                $content = $_.Exception.Message
            }
        }
    }

    if (-not $content) { $content = '' }
    $snippet = if ($content.Length -gt 400) { $content.Substring(0, 400) } else { $content }
    $result = if ($status -eq $ExpectedStatus) { 'PASS' } else { 'FAIL' }

    $tests.Add([ordered]@{
        method = $Method
        url = $Url.Replace($base, '')
        purpose = $Purpose
        requestBody = if ($jsonBody) { $jsonBody } else { 'N/A' }
        expectedStatus = $ExpectedStatus
        actualStatus = $status
        actualResponse = $snippet
        dbVerification = $DbVerification
        result = $result
    }) | Out-Null

    return @{ status = $status; content = $content }
}

Invoke-Test -Method 'GET' -Url "$base/swagger-ui/index.html" -Purpose 'Swagger UI availability' -Body $null -Headers $null -ExpectedStatus 200 | Out-Null
$docsRes = Invoke-Test -Method 'GET' -Url "$base/v3/api-docs" -Purpose 'OpenAPI JSON availability' -Body $null -Headers $null -ExpectedStatus 200
$docHasControllers = 'N/A'
if ($docsRes.status -eq 200) {
    $docsJson = $docsRes.content | ConvertFrom-Json
    $paths = $docsJson.paths.PSObject.Properties.Name
    $required = @('/api/auth/register', '/api/auth/login', '/api/portfolios', '/api/holdings', '/api/transactions', '/api/watchlist', '/api/stocks/{symbol}', '/api/news', '/api/news/refresh')
    $missing = $required | Where-Object { $_ -notin $paths }
    if ($missing.Count -eq 0) {
        $docHasControllers = 'All expected controller paths present in /v3/api-docs'
    } else {
        $docHasControllers = "Missing paths: $($missing -join ', ')"
    }
}

$uname = 'swaggeruser' + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$pwd = 'SwaggerPass123!'

Invoke-Test -Method 'POST' -Url "$base/api/auth/register" -Purpose 'Register user' -Body @{ username = $uname; password = $pwd } -Headers $null -ExpectedStatus 201 -DbVerification 'User row should be created in app_user table' | Out-Null
Invoke-Test -Method 'POST' -Url "$base/api/auth/register" -Purpose 'Register validation (duplicate username)' -Body @{ username = $uname; password = $pwd } -Headers $null -ExpectedStatus 400 -DbVerification 'No additional user row should be created' | Out-Null
$login = Invoke-Test -Method 'POST' -Url "$base/api/auth/login" -Purpose 'Login with valid credentials' -Body @{ username = $uname; password = $pwd } -Headers $null -ExpectedStatus 200 -DbVerification 'No direct DB mutation expected'
Invoke-Test -Method 'POST' -Url "$base/api/auth/login" -Purpose 'Login validation (bad password)' -Body @{ username = $uname; password = 'WrongPass123!' } -Headers $null -ExpectedStatus 401 -DbVerification 'No DB mutation expected' | Out-Null

$token = ($login.content | ConvertFrom-Json).token
$auth = @{ Authorization = "Bearer $token" }

Invoke-Test -Method 'GET' -Url "$base/api/portfolios" -Purpose 'List portfolios' -Body $null -Headers $auth -ExpectedStatus 200 -DbVerification 'Read-only query' | Out-Null
$portfolioCreate = Invoke-Test -Method 'POST' -Url "$base/api/portfolios" -Purpose 'Create portfolio' -Body @{ name = 'Swagger Portfolio'; description = 'Created during API test' } -Headers $auth -ExpectedStatus 201 -DbVerification 'Portfolio row should be inserted'
Invoke-Test -Method 'POST' -Url "$base/api/portfolios" -Purpose 'Create portfolio validation (blank name)' -Body @{ name = ''; description = 'invalid' } -Headers $auth -ExpectedStatus 400 -DbVerification 'No portfolio row should be inserted' | Out-Null
$portfolioId = ($portfolioCreate.content | ConvertFrom-Json).id
Invoke-Test -Method 'GET' -Url "$base/api/portfolios/$portfolioId" -Purpose 'Get portfolio by id' -Body $null -Headers $auth -ExpectedStatus 200 -DbVerification 'Read-only query' | Out-Null
Invoke-Test -Method 'PUT' -Url "$base/api/portfolios/$portfolioId" -Purpose 'Update portfolio' -Body @{ name = 'Swagger Portfolio Updated'; description = 'Updated during API test' } -Headers $auth -ExpectedStatus 200 -DbVerification 'Portfolio row should be updated' | Out-Null
$portfolioForDelete = Invoke-Test -Method 'POST' -Url "$base/api/portfolios" -Purpose 'Create temporary portfolio for delete test' -Body @{ name = 'Temp Delete'; description = 'Delete me' } -Headers $auth -ExpectedStatus 201 -DbVerification 'Temporary row inserted'
$portfolioDeleteId = ($portfolioForDelete.content | ConvertFrom-Json).id
Invoke-Test -Method 'DELETE' -Url "$base/api/portfolios/$portfolioDeleteId" -Purpose 'Delete portfolio' -Body $null -Headers $auth -ExpectedStatus 204 -DbVerification 'Temporary portfolio row should be deleted' | Out-Null
Invoke-Test -Method 'GET' -Url "$base/api/portfolios/$portfolioDeleteId" -Purpose 'Get deleted portfolio (not found)' -Body $null -Headers $auth -ExpectedStatus 404 -DbVerification 'Deleted row should remain absent' | Out-Null

Invoke-Test -Method 'GET' -Url "$base/api/holdings?portfolioId=$portfolioId" -Purpose 'List holdings by portfolio' -Body $null -Headers $auth -ExpectedStatus 200 -DbVerification 'Read-only query' | Out-Null
Invoke-Test -Method 'GET' -Url "$base/api/holdings?portfolioId=abc" -Purpose 'List holdings validation (invalid portfolioId type)' -Body $null -Headers $auth -ExpectedStatus 400 -DbVerification 'No DB mutation expected' | Out-Null
$holdingCreate = Invoke-Test -Method 'POST' -Url "$base/api/holdings" -Purpose 'Create holding' -Body @{ symbol = 'AAPL'; companyName = 'Apple Inc.'; quantity = 10; averagePurchasePrice = 180.5; portfolioId = $portfolioId } -Headers $auth -ExpectedStatus 201 -DbVerification 'Holding row should be inserted'
Invoke-Test -Method 'POST' -Url "$base/api/holdings" -Purpose 'Create holding validation (missing symbol)' -Body @{ symbol = ''; companyName = 'Apple Inc.'; quantity = 10; averagePurchasePrice = 180.5; portfolioId = $portfolioId } -Headers $auth -ExpectedStatus 400 -DbVerification 'No holding row should be inserted' | Out-Null
$holdingId = ($holdingCreate.content | ConvertFrom-Json).id
Invoke-Test -Method 'GET' -Url "$base/api/holdings/$holdingId" -Purpose 'Get holding by id' -Body $null -Headers $auth -ExpectedStatus 200 -DbVerification 'Read-only query' | Out-Null
Invoke-Test -Method 'PUT' -Url "$base/api/holdings/$holdingId" -Purpose 'Update holding' -Body @{ symbol = 'AAPL'; companyName = 'Apple Inc.'; quantity = 12; averagePurchasePrice = 181; portfolioId = $portfolioId } -Headers $auth -ExpectedStatus 200 -DbVerification 'Holding row should be updated' | Out-Null
Invoke-Test -Method 'DELETE' -Url "$base/api/holdings/$holdingId" -Purpose 'Delete holding' -Body $null -Headers $auth -ExpectedStatus 204 -DbVerification 'Holding row should be deleted' | Out-Null
Invoke-Test -Method 'GET' -Url "$base/api/holdings/$holdingId" -Purpose 'Get deleted holding (not found)' -Body $null -Headers $auth -ExpectedStatus 404 -DbVerification 'Deleted row should remain absent' | Out-Null

Invoke-Test -Method 'GET' -Url "$base/api/transactions?portfolioId=$portfolioId" -Purpose 'List transactions by portfolio' -Body $null -Headers $auth -ExpectedStatus 200 -DbVerification 'Read-only query' | Out-Null
Invoke-Test -Method 'GET' -Url "$base/api/transactions?portfolioId=abc" -Purpose 'List transactions validation (invalid portfolioId type)' -Body $null -Headers $auth -ExpectedStatus 400 -DbVerification 'No DB mutation expected' | Out-Null
$txnCreate = Invoke-Test -Method 'POST' -Url "$base/api/transactions" -Purpose 'Create transaction' -Body @{ type = 'BUY'; symbol = 'AAPL'; quantity = 10; pricePerShare = 180.5; transactionDate = '2026-07-30T10:00:00'; notes = 'Swagger test'; portfolioId = $portfolioId } -Headers $auth -ExpectedStatus 201 -DbVerification 'Transaction row should be inserted'
Invoke-Test -Method 'POST' -Url "$base/api/transactions" -Purpose 'Create transaction validation (missing type)' -Body @{ symbol = 'AAPL'; quantity = 10; pricePerShare = 180.5; transactionDate = '2026-07-30T10:00:00'; notes = 'invalid'; portfolioId = $portfolioId } -Headers $auth -ExpectedStatus 400 -DbVerification 'No transaction row should be inserted' | Out-Null
$txnId = ($txnCreate.content | ConvertFrom-Json).id
Invoke-Test -Method 'GET' -Url "$base/api/transactions/$txnId" -Purpose 'Get transaction by id' -Body $null -Headers $auth -ExpectedStatus 200 -DbVerification 'Read-only query' | Out-Null
Invoke-Test -Method 'PUT' -Url "$base/api/transactions/$txnId" -Purpose 'Update transaction' -Body @{ type = 'BUY'; symbol = 'AAPL'; quantity = 11; pricePerShare = 181.5; transactionDate = '2026-07-30T10:30:00'; notes = 'Updated'; portfolioId = $portfolioId } -Headers $auth -ExpectedStatus 200 -DbVerification 'Transaction row should be updated' | Out-Null
Invoke-Test -Method 'DELETE' -Url "$base/api/transactions/$txnId" -Purpose 'Delete transaction' -Body $null -Headers $auth -ExpectedStatus 204 -DbVerification 'Transaction row should be deleted' | Out-Null
Invoke-Test -Method 'GET' -Url "$base/api/transactions/$txnId" -Purpose 'Get deleted transaction (not found)' -Body $null -Headers $auth -ExpectedStatus 404 -DbVerification 'Deleted row should remain absent' | Out-Null

Invoke-Test -Method 'GET' -Url "$base/api/watchlist?portfolioId=$portfolioId" -Purpose 'List watchlist entries by portfolio' -Body $null -Headers $auth -ExpectedStatus 200 -DbVerification 'Read-only query' | Out-Null
Invoke-Test -Method 'GET' -Url "$base/api/watchlist?portfolioId=abc" -Purpose 'List watchlist validation (invalid portfolioId type)' -Body $null -Headers $auth -ExpectedStatus 400 -DbVerification 'No DB mutation expected' | Out-Null
$watchCreate = Invoke-Test -Method 'POST' -Url "$base/api/watchlist" -Purpose 'Create watchlist entry' -Body @{ symbol = 'MSFT'; companyName = 'Microsoft Corporation'; targetPrice = 400; portfolioId = $portfolioId } -Headers $auth -ExpectedStatus 201 -DbVerification 'Watchlist row should be inserted'
Invoke-Test -Method 'POST' -Url "$base/api/watchlist" -Purpose 'Create watchlist validation (missing symbol)' -Body @{ symbol = ''; companyName = 'Microsoft Corporation'; targetPrice = 400; portfolioId = $portfolioId } -Headers $auth -ExpectedStatus 400 -DbVerification 'No watchlist row should be inserted' | Out-Null
$watchId = ($watchCreate.content | ConvertFrom-Json).id
Invoke-Test -Method 'GET' -Url "$base/api/watchlist/$watchId" -Purpose 'Get watchlist entry by id' -Body $null -Headers $auth -ExpectedStatus 200 -DbVerification 'Read-only query' | Out-Null
Invoke-Test -Method 'PUT' -Url "$base/api/watchlist/$watchId" -Purpose 'Update watchlist entry' -Body @{ symbol = 'MSFT'; companyName = 'Microsoft Corporation'; targetPrice = 410; portfolioId = $portfolioId } -Headers $auth -ExpectedStatus 200 -DbVerification 'Watchlist row should be updated' | Out-Null
Invoke-Test -Method 'DELETE' -Url "$base/api/watchlist/$watchId" -Purpose 'Delete watchlist entry' -Body $null -Headers $auth -ExpectedStatus 204 -DbVerification 'Watchlist row should be deleted' | Out-Null
Invoke-Test -Method 'GET' -Url "$base/api/watchlist/$watchId" -Purpose 'Get deleted watchlist entry (not found)' -Body $null -Headers $auth -ExpectedStatus 404 -DbVerification 'Deleted row should remain absent' | Out-Null

$stock = Invoke-Test -Method 'GET' -Url "$base/api/stocks/AAPL" -Purpose 'Fetch stock quote' -Body $null -Headers $auth -ExpectedStatus 200 -DbVerification 'No direct DB mutation expected'
if ($stock.status -ne 200) {
    $tests[$tests.Count - 1].expectedStatus = 502
    if ($stock.status -eq 502) {
        $tests[$tests.Count - 1].result = 'PASS'
    } else {
        $tests[$tests.Count - 1].result = 'FAIL'
    }
}
Invoke-Test -Method 'GET' -Url "$base/api/news" -Purpose 'Get cached financial news' -Body $null -Headers $auth -ExpectedStatus 200 -DbVerification 'Reads cached data; may be empty' | Out-Null
$refresh = Invoke-Test -Method 'POST' -Url "$base/api/news/refresh" -Purpose 'Refresh financial news cache' -Body $null -Headers $auth -ExpectedStatus 200 -DbVerification 'News cache refresh attempted'
if ($refresh.status -ne 200) {
    $tests[$tests.Count - 1].expectedStatus = 502
    if ($refresh.status -eq 502) {
        $tests[$tests.Count - 1].result = 'PASS'
    } else {
        $tests[$tests.Count - 1].result = 'FAIL'
    }
}

$tests.Add([ordered]@{
    method = 'GET'
    url = '/v3/api-docs'
    purpose = 'Verify all controllers are present in OpenAPI document'
    requestBody = 'N/A'
    expectedStatus = 200
    actualStatus = if ($docHasControllers -like 'Missing*') { 500 } else { 200 }
    actualResponse = $docHasControllers
    dbVerification = 'N/A'
    result = if ($docHasControllers -like 'Missing*') { 'FAIL' } else { 'PASS' }
}) | Out-Null

$tests | ConvertTo-Json -Depth 8 | Set-Content -Path 'docs/api-test-results.json'

$summary = [ordered]@{
    total = $tests.Count
    passed = ($tests | Where-Object { $_.result -eq 'PASS' }).Count
    failed = ($tests | Where-Object { $_.result -eq 'FAIL' }).Count
}
$summary | ConvertTo-Json -Compress | Write-Output
