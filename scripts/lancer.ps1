param([switch]$Verifier)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$racineCinema = Split-Path -Parent $PSScriptRoot
$jarCinema = Join-Path $racineCinema 'target\cinema-billetterie-1.0.0.jar'
$fichierPidCinema = Join-Path $racineCinema '.tools\cinema.pid'
$repertoireLogsCinema = Join-Path $racineCinema 'logs'

if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = Join-Path $env:USERPROFILE '.jdks\ms-25.0.4.1'
}
$javaCinema = Join-Path $env:JAVA_HOME 'bin\java.exe'
$versionCinema = Join-Path $env:JAVA_HOME 'release'
if (-not (Test-Path -LiteralPath $javaCinema) -or -not (Test-Path -LiteralPath $versionCinema) -or
    -not (Select-String -LiteralPath $versionCinema -Pattern '^JAVA_VERSION="25[."]' -Quiet)) {
    throw 'Définissez JAVA_HOME vers un JDK 25 avant de lancer ce script.'
}
if (-not (Test-Path -LiteralPath $jarCinema)) {
    throw 'JAR absent. Exécutez .\mvnw.cmd clean verify à la racine du projet.'
}
$portCinema = if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8080 }
$ancienPidCinema = 0
if (Test-Path -LiteralPath $fichierPidCinema) {
    [void][int]::TryParse((Get-Content -LiteralPath $fichierPidCinema -Raw).Trim(), [ref]$ancienPidCinema)
}
$ecouteCinema = @(Get-NetTCPConnection -LocalPort $portCinema -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique)
foreach ($numeroCinema in $ecouteCinema) {
    $processusCinema = Get-CimInstance Win32_Process -Filter "ProcessId = $numeroCinema"
    $commandeCinema = [string]$processusCinema.CommandLine
    $jarAbsoluCinema = $commandeCinema.IndexOf($jarCinema, [StringComparison]::OrdinalIgnoreCase) -ge 0
    # Compatibilité avec le lancement relatif historique, uniquement pour le PID mémorisé.
    $jarRelatifCinema = $numeroCinema -eq $ancienPidCinema -and
        $commandeCinema -match '-jar\s+"?target[\\/]cinema-billetterie-1\.0\.0\.jar"?\s*$'
    if ($processusCinema.Name -ne 'java.exe' -or $commandeCinema -notmatch '\s-jar\s' -or
        -not ($jarAbsoluCinema -or $jarRelatifCinema)) {
        throw "Le port $portCinema appartient à un autre programme. Aucun processus arrêté."
    }
}
if ($Verifier) {
    Write-Output 'JDK 25 et JAR disponibles ; processus du port vérifiés.'
    Write-Output "Jeton TMDB présent dans ce terminal : $(-not [string]::IsNullOrWhiteSpace($env:TMDB_API_TOKEN))"
    return
}

Push-Location $racineCinema
try {
    docker compose up -d --wait --wait-timeout 120
    if ($LASTEXITCODE -ne 0) { throw 'MySQL ne démarre pas ; application actuelle conservée.' }
    foreach ($numeroCinema in $ecouteCinema) { Stop-Process -Id $numeroCinema }
    New-Item -ItemType Directory -Path (Split-Path $fichierPidCinema), $repertoireLogsCinema -Force | Out-Null
    # Start-Process hérite de l'environnement du terminal, y compris TMDB_API_TOKEN.
    $nouveauCinema = Start-Process -FilePath $javaCinema -ArgumentList '-jar', ('"' + $jarCinema + '"') `
        -WorkingDirectory $racineCinema -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $repertoireLogsCinema 'cinema.stdout.log') `
        -RedirectStandardError (Join-Path $repertoireLogsCinema 'cinema.stderr.log')
    $nouveauCinema.Id | Set-Content -LiteralPath $fichierPidCinema
    for ($essaiCinema = 0; $essaiCinema -lt 60; $essaiCinema++) {
        if ($nouveauCinema.HasExited) { throw 'Le JAR a quitté. Consultez les journaux dans logs/.' }
        try {
            $santeCinema = Invoke-RestMethod "http://localhost:$portCinema/actuator/health" -TimeoutSec 2
            if ($santeCinema.status -eq 'UP') {
                Write-Output "CinéBil démarré : http://localhost:$portCinema/"
                Write-Output "Jeton TMDB transmis au serveur : $(-not [string]::IsNullOrWhiteSpace($env:TMDB_API_TOKEN))"
                return
            }
        } catch { }
        Start-Sleep -Milliseconds 500
    }
    throw 'Démarrage non confirmé. Consultez les journaux dans logs/.'
} finally {
    Pop-Location
}
