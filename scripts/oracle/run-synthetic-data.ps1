param(
    [ValidateSet('demo', 'standard', 'stress')]
    [string]$Scenario = 'standard',
    [ValidateRange(7, 730)]
    [int]$HistoryDays = 180
)

Push-Location (Join-Path $PSScriptRoot '..\..\backend-java')
try {
    mvn spring-boot:run "-Dspring-boot.run.profiles=synthetic-data" "-Dspring-boot.run.arguments=--mew.synthetic.scenario=$Scenario --mew.synthetic.history-days=$HistoryDays"
} finally {
    Pop-Location
}
