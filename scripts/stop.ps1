# =============================================================================
#  Zaustavljanje projekta "Novi Sad": backend, frontend, Elasticsearch, MinIO.
#  PostgreSQL se ne dira - to je deljeni sistemski servis.
#
#  Pokretanje (iz korena projekta):
#      powershell -ExecutionPolicy Bypass -File scripts\stop.ps1
#  ili dvoklikom na scripts\stop.bat
# =============================================================================

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ports = [ordered]@{
    8080 = "Backend (Spring Boot)"
    4200 = "Frontend (Angular)"
    9200 = "Elasticsearch"
    9000 = "MinIO"
}

Write-Host "=================================================================="
Write-Host "  Novi Sad - zaustavljanje servisa"
Write-Host "=================================================================="
Write-Host ""

foreach ($entry in $ports.GetEnumerator()) {
    # .Keys + indeksiranje je nepouzdano nad [ordered] recnikom u ovoj verziji
    # PowerShell-a (vracalo prazne vrednosti) - GetEnumerator() radi ispravno.
    $port  = $entry.Key
    $name  = $entry.Value
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue

    if (-not $conns) {
        Write-Host "  [-] $name (port $port): nije pokrenut"
        continue
    }

    $processIds = $conns | Select-Object -Expand OwningProcess -Unique
    foreach ($processId in $processIds) {
        $proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($proc) {
            Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
            Write-Host "  [x] $name (port $port): zaustavljen (pid $processId, $($proc.ProcessName))" -ForegroundColor Green
        }
    }
}

Write-Host ""
Write-Host "  PostgreSQL nije diran - to je deljeni sistemski servis."
Write-Host ""
