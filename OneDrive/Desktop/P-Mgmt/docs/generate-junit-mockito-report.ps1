$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$surefireDir = Join-Path $projectRoot 'target/surefire-reports'
$jacocoPath = Join-Path $projectRoot 'target/site/jacoco/jacoco.xml'
$outputPath = Join-Path $PSScriptRoot 'JUNIT_MOCKITO_TEST_REPORT.md'

if (-not (Test-Path $surefireDir)) {
    throw "Surefire reports not found at $surefireDir. Run Maven tests first."
}

$reportFiles = Get-ChildItem -Path $surefireDir -Filter 'TEST-*.xml' | Sort-Object Name
if (-not $reportFiles) {
    throw "No JUnit XML files found in $surefireDir."
}

$totalTests = 0
$totalFailures = 0
$totalErrors = 0
$totalSkipped = 0
$totalTime = 0.0
$suiteRows = New-Object System.Collections.Generic.List[object]

foreach ($file in $reportFiles) {
    [xml]$xml = Get-Content -Path $file.FullName -Raw
    $suite = $xml.testsuite
    if ($null -eq $suite) {
        continue
    }

    $tests = [int]$suite.tests
    $failures = [int]$suite.failures
    $errors = [int]$suite.errors
    $skipped = [int]$suite.skipped
    $time = [double]$suite.time

    $totalTests += $tests
    $totalFailures += $failures
    $totalErrors += $errors
    $totalSkipped += $skipped
    $totalTime += $time

    $suiteRows.Add([pscustomobject]@{
        Name = [string]$suite.name
        Tests = $tests
        Failures = $failures
        Errors = $errors
        Skipped = $skipped
        Time = [Math]::Round($time, 3)
    }) | Out-Null
}

$coverageRows = @()
$instructionCoverage = $null
$branchCoverage = $null

if (Test-Path $jacocoPath) {
    [xml]$jacoco = Get-Content -Path $jacocoPath -Raw
    foreach ($package in $jacoco.report.package) {
        foreach ($class in $package.class) {
            $className = ([string]$class.name).Replace('/', '.')
            $counters = @{}
            foreach ($counter in $class.counter) {
                $counters[[string]$counter.type] = $counter
            }

            if ($counters.ContainsKey('LINE')) {
                $lineCounter = $counters['LINE']
                $missed = [double]$lineCounter.missed
                $covered = [double]$lineCounter.covered
                $percent = if (($missed + $covered) -eq 0) { 100 } else { [Math]::Round(($covered / ($missed + $covered)) * 100, 2) }
                $coverageRows += [pscustomobject]@{
                    Class = $className
                    LinesCovered = [int]$covered
                    LinesMissed = [int]$missed
                    LineCoverage = $percent
                }
            }
        }
    }

    $instructionCounter = $jacoco.report.counter | Where-Object { $_.type -eq 'INSTRUCTION' } | Select-Object -First 1
    if ($instructionCounter) {
        $missed = [double]$instructionCounter.missed
        $covered = [double]$instructionCounter.covered
        $instructionCoverage = if (($missed + $covered) -eq 0) { 100 } else { [Math]::Round(($covered / ($missed + $covered)) * 100, 2) }
    }

    $branchCounter = $jacoco.report.counter | Where-Object { $_.type -eq 'BRANCH' } | Select-Object -First 1
    if ($branchCounter) {
        $missed = [double]$branchCounter.missed
        $covered = [double]$branchCounter.covered
        $branchCoverage = if (($missed + $covered) -eq 0) { 100 } else { [Math]::Round(($covered / ($missed + $covered)) * 100, 2) }
    }
}

$unitSuites = $suiteRows | Where-Object { $_.Name -like '*.service.*Test' -or $_.Name -like '*.chatbot.*Test' }
$integrationSuites = $suiteRows | Where-Object { $_.Name -like '*IntegrationTest' }

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add('# JUnit and Mockito Test Report') | Out-Null
$lines.Add('') | Out-Null
$lines.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')") | Out-Null
$lines.Add('') | Out-Null
$lines.Add('## Scope') | Out-Null
$lines.Add('- Test framework: JUnit 5 via Spring Boot starter test') | Out-Null
$lines.Add('- Mocking framework: Mockito') | Out-Null
$lines.Add('- Build tool: Maven Surefire with JaCoCo coverage reporting') | Out-Null
$lines.Add('') | Out-Null
$lines.Add('## Summary') | Out-Null
$lines.Add("- Total tests: $totalTests") | Out-Null
$lines.Add("- Failures: $totalFailures") | Out-Null
$lines.Add("- Errors: $totalErrors") | Out-Null
$lines.Add("- Skipped: $totalSkipped") | Out-Null
$lines.Add("- Total runtime: $([Math]::Round($totalTime, 3)) seconds") | Out-Null
if ($null -ne $instructionCoverage) {
    $lines.Add("- Instruction coverage: $instructionCoverage%") | Out-Null
}
if ($null -ne $branchCoverage) {
    $lines.Add("- Branch coverage: $branchCoverage%") | Out-Null
}
$lines.Add('') | Out-Null
$lines.Add('## Test Suites') | Out-Null
$lines.Add('| Suite | Tests | Failures | Errors | Skipped | Time (s) |') | Out-Null
$lines.Add('| --- | ---: | ---: | ---: | ---: | ---: |') | Out-Null
foreach ($suite in $suiteRows) {
    $lines.Add("| $($suite.Name) | $($suite.Tests) | $($suite.Failures) | $($suite.Errors) | $($suite.Skipped) | $($suite.Time) |") | Out-Null
}
$lines.Add('') | Out-Null
$lines.Add('## Mockito-Covered Unit Tests') | Out-Null
if ($unitSuites) {
    foreach ($suite in $unitSuites) {
        $lines.Add("- $($suite.Name): $($suite.Tests) tests") | Out-Null
    }
} else {
    $lines.Add('- No Mockito-oriented unit suites were detected.') | Out-Null
}
$lines.Add('') | Out-Null
$lines.Add('## Integration Tests') | Out-Null
if ($integrationSuites) {
    foreach ($suite in $integrationSuites) {
        $lines.Add("- $($suite.Name): $($suite.Tests) tests") | Out-Null
    }
} else {
    $lines.Add('- No integration suites were detected.') | Out-Null
}

if ($coverageRows.Count -gt 0) {
    $lines.Add('') | Out-Null
    $lines.Add('## Line Coverage By Class') | Out-Null
    $lines.Add('| Class | Covered | Missed | Line Coverage |') | Out-Null
    $lines.Add('| --- | ---: | ---: | ---: |') | Out-Null
    foreach ($row in ($coverageRows | Sort-Object LineCoverage, Class -Descending:$false)) {
        $lines.Add("| $($row.Class) | $($row.LinesCovered) | $($row.LinesMissed) | $($row.LineCoverage)% |") | Out-Null
    }
}

$lines.Add('') | Out-Null
$lines.Add('## Notes') | Out-Null
$lines.Add('- AuthServiceTest and PortfolioServiceTest exercise business logic with Mockito-backed collaborators.') | Out-Null
$lines.Add('- PortfolioApiIntegrationTest verifies token issuance plus authenticated portfolio CRUD through Spring MockMvc.') | Out-Null
$lines.Add('- Raw XML and coverage artifacts remain available under target/surefire-reports and target/site/jacoco for CI use.') | Out-Null

$lines -join "`r`n" | Set-Content -Path $outputPath
Write-Output "REPORT_GENERATED:$outputPath"