$results = Get-Content .\docs\api-test-results.json -Raw | ConvertFrom-Json
$sbVersion = '3.3.5'
$javaVersion = '21.0.1'
$mysqlVersion = '8.0.44'
$swaggerVersion = 'springdoc-openapi-starter-webmvc-ui 2.6.0'

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add('# Portfolio Management API Test Report') | Out-Null
$lines.Add('') | Out-Null
$lines.Add('## Environment') | Out-Null
$lines.Add("- Spring Boot version: $sbVersion") | Out-Null
$lines.Add("- Java version: $javaVersion") | Out-Null
$lines.Add("- MySQL version: $mysqlVersion") | Out-Null
$lines.Add("- Swagger version: $swaggerVersion") | Out-Null
$lines.Add('') | Out-Null
$lines.Add('## Swagger Configuration') | Out-Null
$lines.Add('- Dependency added: org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0') | Out-Null
$lines.Add('- Configuration class: OpenApiConfig') | Out-Null
$lines.Add('- URL to access Swagger UI: http://localhost:8080/swagger-ui/index.html') | Out-Null
$lines.Add('- URL to access OpenAPI JSON: http://localhost:8080/v3/api-docs') | Out-Null
$lines.Add('') | Out-Null
$lines.Add('## Tested Endpoints') | Out-Null
$lines.Add('') | Out-Null

foreach ($t in $results) {
    $lines.Add('### Endpoint') | Out-Null
    $lines.Add("Method: $($t.method)") | Out-Null
    $lines.Add("URL: $($t.url)") | Out-Null
    $lines.Add('') | Out-Null
    $lines.Add("Purpose: $($t.purpose)") | Out-Null
    $lines.Add('') | Out-Null
    $lines.Add('Request Body:') | Out-Null
    $lines.Add('```json') | Out-Null
    if ([string]::IsNullOrWhiteSpace([string]$t.requestBody)) {
        $lines.Add('N/A') | Out-Null
    } else {
        $lines.Add([string]$t.requestBody) | Out-Null
    }
    $lines.Add('```') | Out-Null
    $lines.Add('') | Out-Null
    $lines.Add("Expected Response: HTTP $($t.expectedStatus)") | Out-Null
    $lines.Add('') | Out-Null
    $lines.Add('Actual Response:') | Out-Null
    $lines.Add('```json') | Out-Null
    if ([string]::IsNullOrWhiteSpace([string]$t.actualResponse)) {
        $lines.Add('{}') | Out-Null
    } else {
        $lines.Add([string]$t.actualResponse) | Out-Null
    }
    $lines.Add('```') | Out-Null
    $lines.Add('') | Out-Null
    $lines.Add("Status: HTTP $($t.actualStatus)") | Out-Null
    $lines.Add('') | Out-Null
    $lines.Add("Database Verification: $($t.dbVerification)") | Out-Null
    $lines.Add('') | Out-Null
    $lines.Add("Result: $($t.result)") | Out-Null
    $lines.Add('') | Out-Null
}

$total = $results.Count
$passed = ($results | Where-Object { $_.result -eq 'PASS' }).Count
$failed = ($results | Where-Object { $_.result -eq 'FAIL' }).Count

$lines.Add('## Summary') | Out-Null
$lines.Add('') | Out-Null
$lines.Add("- Total APIs tested: $total") | Out-Null
$lines.Add("- Passed: $passed") | Out-Null
$lines.Add("- Failed: $failed") | Out-Null
$lines.Add('- Issues fixed: Swagger/OpenAPI integration added; all controllers documented and exposed in OpenAPI; security updated to allow Swagger routes.') | Out-Null
$lines.Add('- Remaining issues (if any): Stock quote endpoint may return 502 when Yahoo Finance upstream is unavailable; endpoint behavior is documented and handled.') | Out-Null

$lines -join "`r`n" | Set-Content .\docs\API_TEST_REPORT.md
Write-Output 'REPORT_GENERATED'
