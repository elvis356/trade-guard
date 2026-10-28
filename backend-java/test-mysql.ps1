param(
    [string]$Database = "tradeguard_test",
    [string]$Username = "tradeguard_test",
    [string]$HostName = "localhost",
    [ValidateRange(1, 65535)]
    [int]$Port = 3306
)

if ($Database -notmatch '^[A-Za-z0-9_]+$') {
    throw "Database must contain only letters, digits, and underscores."
}

$securePassword = Read-Host "MySQL test account password" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$mavenWrapper = Join-Path $PSScriptRoot "mvnw.cmd"
$mavenExitCode = 1
$locationPushed = $false

try {
    $env:SPRING_DATASOURCE_URL = "jdbc:mysql://${HostName}:${Port}/${Database}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
    $env:SPRING_DATASOURCE_USERNAME = $Username
    $env:SPRING_DATASOURCE_PASSWORD =
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver"
    $env:SPRING_JPA_DATABASE_PLATFORM = "org.hibernate.dialect.MySQLDialect"
    $env:SPRING_FLYWAY_LOCATIONS = "classpath:db/migration-mysql"

    Push-Location $PSScriptRoot
    $locationPushed = $true
    & $mavenWrapper -B clean verify
    $mavenExitCode = $LASTEXITCODE
}
finally {
    if ($locationPushed) {
        Pop-Location
    }
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    $env:SPRING_DATASOURCE_URL = $null
    $env:SPRING_DATASOURCE_USERNAME = $null
    $env:SPRING_DATASOURCE_PASSWORD = $null
    $env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = $null
    $env:SPRING_JPA_DATABASE_PLATFORM = $null
    $env:SPRING_FLYWAY_LOCATIONS = $null
}

exit $mavenExitCode
