# =============================================================================
#  Pokretanje projekta "Novi Sad" - backend + frontend (i Elasticsearch/MinIO
#  ako su instalirani) na bilo kojoj Windows masini.
#
#  Pokretanje (iz korena projekta):
#      powershell -ExecutionPolicy Bypass -File scripts\start.ps1
#  ili dvoklikom na scripts\start.bat
#
#  Sta skripta radi, korak po korak:
#    1. Pronalazi JDK 21 (ne oslanja se na PATH, koji cesto pokazuje na stariju
#       Javu ili samo JRE).
#    2. Proverava da su Node.js i npm dostupni.
#    3. Proverava da li nesto slusa na 5432 (PostgreSQL).
#    4. Podesava lozinku baze: koristi application-local.yml ako postoji,
#       DB_PASSWORD ako je vec u okruzenju, inace pita jednom (lozinka se ne
#       upisuje na disk osim ako se to izricito potvrdi).
#    5. Ako PostgreSQL radi, proverava da li baza 'uesnovisad' postoji i
#       automatski je pravi ako ne postoji (preko createdb/psql alata).
#    6. Ako su Elasticsearch i MinIO instalirani (podrazumevano se traze u
#       C:\UES\tools, po istom rasporedu kao u UPUTSTVO.md), pokrece ih i ceka
#       da budu spremni. Ako nisu instalirani, backend se pokrece sa
#       STORAGE_TYPE=local i SEARCH_ENABLED=false - UES deo (pretraga, PDF
#       indeksiranje) se tada preskace, a ostatak aplikacije radi normalno.
#    7. Pokrece backend (mvnw spring-boot:run) u novom prozoru i ceka da
#       odgovori na /api/auth/me.
#    8. Pokrece frontend (npm start, sa npm install ako nedostaje
#       node_modules) u novom prozoru i ceka da odgovori na :4200.
#    9. Otvara pretrazivac na http://localhost:4200.
#
#  Ako je nesto vec pokrenuto (port je zauzet), taj korak se preskace - bezbedno
#  je pustiti skriptu vise puta zaredom.
#
#  Parametri:
#    -SkipEs             ne pokusavaj da pokrenes Elasticsearch/MinIO
#    -EsPath <put>        gde je raspakovan Elasticsearch (podrazumevano trazi
#                         C:\UES\tools\elasticsearch-*)
#    -MinioPath <put>     gde je minio.exe (podrazumevano C:\UES\tools\minio.exe)
#    -DbPassword <lozinka> lozinka za PostgreSQL korisnika 'postgres', da se
#                         izbegne interaktivno pitanje (korisno za automatizaciju)
# =============================================================================

param(
    [switch]$SkipEs,
    [string]$EsPath,
    [string]$MinioPath,
    [string]$DbPassword
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RootDir     = Split-Path -Parent $PSScriptRoot
$BackendDir  = Join-Path $RootDir "backend"
$FrontendDir = Join-Path $RootDir "frontend"

# --- pomocne funkcije ---------------------------------------------------------

function Write-Step($text) {
    Write-Host ""
    Write-Host ">>> $text" -ForegroundColor Cyan
}

function Write-Ok($text) {
    Write-Host "    [OK] $text" -ForegroundColor Green
}

function Write-Warn2($text) {
    Write-Host "    [!] $text" -ForegroundColor Yellow
}

function Write-Fail($text) {
    Write-Host "    [GRESKA] $text" -ForegroundColor Red
}

function Test-Port([int]$portNumber) {
    return $null -ne (Get-NetTCPConnection -LocalPort $portNumber -State Listen -ErrorAction SilentlyContinue)
}

# Ceka da URL odgovori jednim od zadatih HTTP kodova (401 se npr. racuna kao
# "backend radi", jer /api/auth/me bez tokena namerno vraca 401).
function Wait-ForHttp([string]$url, [int]$timeoutSeconds, [int[]]$acceptedCodes) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
            if ($acceptedCodes -contains [int]$resp.StatusCode) { return $true }
        } catch {
            if ($_.Exception.Response) {
                $code = $_.Exception.Response.StatusCode.value__
                if ($acceptedCodes -contains $code) { return $true }
            }
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

Write-Host "=================================================================="
Write-Host "  Novi Sad - pokretanje projekta"
Write-Host "=================================================================="

# --- 1. JDK 21 ----------------------------------------------------------------

Write-Step "Trazim JDK 21"

function Test-Jdk21Home([string]$candidatePath) {
    # Vraca $true samo ako je ovo koren JDK 21 instalacije (ima release fajl
    # sa JAVA_VERSION="21...). Nikad ne baca gresku - vraca $false umesto toga,
    # da provera jednog kandidata ne obori celu skriptu.
    try {
        if (-not $candidatePath) { return $false }
        $releaseFile = Join-Path $candidatePath "release"
        if (-not (Test-Path -LiteralPath $releaseFile)) { return $false }
        $content = Get-Content -LiteralPath $releaseFile -Raw -ErrorAction SilentlyContinue
        return ($content -match 'JAVA_VERSION="21')
    } catch {
        return $false
    }
}

function Find-Jdk21 {
    # 1) JAVA_HOME, ako vec pokazuje na 21.
    # Ociscen od navodnika i praznina koje cesto ostanu kad se promenljiva
    # postavlja rucno preko System Properties (npr. JAVA_HOME="C:\..." sa
    # navodnicima kopiranim iz uputstva za Linux/Mac, ili slucajan razmak na
    # kraju) - bez ovoga bi provera tiho ili glasno propala iako JDK postoji.
    if ($env:JAVA_HOME) {
        $cleaned = $env:JAVA_HOME.Trim().Trim('"').Trim("'").TrimEnd('\')
        if (Test-Jdk21Home $cleaned) { return $cleaned }
        $script:JavaHomeChecked = $cleaned
    }

    # 2) java.exe na PATH-u - moze biti postavljen a JAVA_HOME da ne pokazuje na njega
    $javaOnPath = Get-Command "java.exe" -ErrorAction SilentlyContinue
    if ($javaOnPath) {
        $candidate = Split-Path (Split-Path $javaOnPath.Source -Parent) -Parent
        if (Test-Jdk21Home $candidate) { return $candidate }
    }

    # 3) uobicajene instalacione putanje razlicitih distribucija JDK-a
    $patterns = @(
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Java\jdk-21*",
        "C:\Program Files\Microsoft\jdk-21*",
        "C:\Program Files\Amazon Corretto\jdk21*",
        "C:\Program Files\Zulu\zulu-21*",
        "C:\Program Files\BellSoft\LibericaJDK-21*"
    )
    foreach ($pattern in $patterns) {
        $found = Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found -and (Test-Jdk21Home $found.FullName)) { return $found.FullName }
    }
    return $null
}

$jdk21 = Find-Jdk21
if (-not $jdk21) {
    Write-Fail "JDK 21 nije pronadjen ni u JAVA_HOME ni u uobicajenim folderima."
    if ($script:JavaHomeChecked) {
        Write-Host "    JAVA_HOME je bio postavljen na: $script:JavaHomeChecked" -ForegroundColor Yellow
        Write-Host "    Tamo nije nadjen 'release' fajl sa JAVA_VERSION=`"21...`" - proveri da" -ForegroundColor Yellow
        Write-Host "    JAVA_HOME pokazuje na KOREN JDK instalacije (ne na 'bin' podfolder)." -ForegroundColor Yellow
    }
    Write-Host "    Preuzmi JDK 21 sa: https://adoptium.net/temurin/releases/?version=21" -ForegroundColor Yellow
    exit 1
}
Write-Ok "JDK 21: $jdk21"

# --- 2. Node.js / npm -----------------------------------------------------------

Write-Step "Proveravam Node.js i npm"
try {
    $nodeVersion = (node -v)
    $npmVersion  = (npm -v)
    Write-Ok "Node $nodeVersion, npm $npmVersion"
} catch {
    Write-Fail "Node.js nije pronadjen na PATH-u."
    Write-Host "    Preuzmi ga sa: https://nodejs.org/" -ForegroundColor Yellow
    exit 1
}

# --- 3. PostgreSQL --------------------------------------------------------------

Write-Step "Proveravam PostgreSQL (port 5432)"
if (Test-Port 5432) {
    Write-Ok "Nesto slusa na 5432 - pretpostavljam da je PostgreSQL pokrenut"
} else {
    Write-Warn2 "Nista ne slusa na 5432. Backend nece moci da se poveze na bazu."
    Write-Warn2 "Pokreni PostgreSQL servis (Services -> postgresql-x64-*) pa probaj ponovo."
}

# --- 4. Lozinka baze ------------------------------------------------------------

Write-Step "Podesavanje konekcije ka bazi"

$localYml      = Join-Path $BackendDir "src\main\resources\application-local.yml"
$springProfile = $null

if (Test-Path $localYml) {
    Write-Ok "Nadjen application-local.yml - koristim profil 'local'"
    $springProfile = "local"
} elseif ($DbPassword) {
    $env:DB_PASSWORD = $DbPassword
    Write-Ok "Koristim lozinku prosledjenu kroz -DbPassword"
} elseif ($env:DB_PASSWORD) {
    Write-Ok "DB_PASSWORD je vec postavljen u okruzenju - koristim ga"
} else {
    Write-Warn2 "application-local.yml ne postoji, a DB_PASSWORD nije podesen."
    $securePwd = Read-Host "Unesi lozinku za PostgreSQL korisnika 'postgres' (Enter = 'postgres')" -AsSecureString
    $bstr      = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePwd)
    $plainPwd  = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    if ([string]::IsNullOrEmpty($plainPwd)) { $plainPwd = "postgres" }
    $env:DB_PASSWORD = $plainPwd
    Write-Ok "Lozinka postavljena samo za ovu sesiju (nije upisana na disk)."

    $save = Read-Host "Sacuvati je u application-local.yml za sledeci put? (d/N)"
    if ($save -eq "d" -or $save -eq "D") {
        $escaped = $plainPwd.Replace("'", "''")
        $content = @"
# Napravljeno automatski preko scripts\start.ps1
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/uesnovisad
    username: postgres
    password: $escaped
"@
        Set-Content -Path $localYml -Value $content -Encoding UTF8
        Write-Ok "Sacuvano u $localYml (u .gitignore je, nece otici u git)"
    }
}

# --- 4b. Priprema baze (kreira 'uesnovisad' ako ne postoji) --------------------

Write-Step "Priprema baze podataka"

function Find-PostgresBin {
    # 1) na PATH
    $onPath = Get-Command "createdb.exe" -ErrorAction SilentlyContinue
    if ($onPath) { return Split-Path $onPath.Source -Parent }

    # 2) uobicajena instalaciona putanja (moze biti vise verzija - uzmi najnoviju)
    $found = Get-ChildItem -Path "C:\Program Files\PostgreSQL\*\bin\createdb.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1
    if ($found) { return Split-Path $found.FullName -Parent }

    return $null
}

if (-not (Test-Port 5432)) {
    Write-Warn2 "PostgreSQL ne radi - preskacem proveru/kreiranje baze."
} else {
    $pgBin = Find-PostgresBin
    if (-not $pgBin) {
        Write-Warn2 "Alati PostgreSQL-a (createdb/psql) nisu pronadjeni - baza se ne moze automatski napraviti."
        Write-Warn2 "Napravi je rucno: createdb -U postgres uesnovisad (vidi UPUTSTVO.md, odeljak 1)."
    } else {
        # Lozinka za konekciju: iz application-local.yml ako postoji (tada je nismo
        # ranije citali), inace je vec u $env:DB_PASSWORD iz koraka 4.
        $createDbPassword = $env:DB_PASSWORD
        if (-not $createDbPassword -and (Test-Path $localYml)) {
            $ymlContent = Get-Content $localYml -Raw
            if ($ymlContent -match '(?m)^\s*password:\s*(.+?)\s*$') {
                $createDbPassword = $Matches[1]
            }
        }

        if (-not $createDbPassword) {
            Write-Warn2 "Lozinka za proveru baze nije poznata - preskacem automatsko kreiranje."
        } else {
            $env:PGPASSWORD = $createDbPassword
            try {
                $psqlExe     = Join-Path $pgBin "psql.exe"
                $createdbExe = Join-Path $pgBin "createdb.exe"

                $exists = & $psqlExe -U postgres -h localhost -tAc "SELECT 1 FROM pg_database WHERE datname='uesnovisad'" 2>$null
                if ($exists -match "1") {
                    Write-Ok "Baza 'uesnovisad' vec postoji"
                } else {
                    & $createdbExe -U postgres -h localhost uesnovisad 2>$null
                    if ($LASTEXITCODE -eq 0) {
                        Write-Ok "Baza 'uesnovisad' je kreirana"
                    } else {
                        Write-Warn2 "Nije uspelo automatsko kreiranje baze - proveri lozinku ili je napravi rucno."
                    }
                }
            } finally {
                Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
            }
        }
    }
}

# --- 5. Elasticsearch + MinIO (UES deo) -----------------------------------------

$searchEnabled = $false
$storageType   = "local"

if (-not $SkipEs) {
    Write-Step "Trazim Elasticsearch i MinIO (UES deo)"

    if (-not $EsPath) {
        $esCandidate = Get-ChildItem -Path "C:\UES\tools\elasticsearch-*" -Directory -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($esCandidate) { $EsPath = $esCandidate.FullName }
    }
    if (-not $MinioPath) {
        $minioCandidate = "C:\UES\tools\minio.exe"
        if (Test-Path $minioCandidate) { $MinioPath = $minioCandidate }
    }

    if ($EsPath -and (Test-Path (Join-Path $EsPath "bin\elasticsearch.bat")) -and $MinioPath -and (Test-Path $MinioPath)) {
        Write-Ok "Nadjeni: $EsPath, $MinioPath"

        if (Test-Port 9200) {
            Write-Ok "Elasticsearch vec radi na 9200"
        } else {
            Write-Host "    Pokrecem Elasticsearch (prvi put moze potrajati ~30-60s)..."
            $esExe = Join-Path $EsPath "bin\elasticsearch.bat"
            $esLog = Join-Path $env:TEMP "novisad-elasticsearch.log"
            # Bitno: log putanja NE sme biti pod navodnicima - cmd.exe redirekcija
            # (>) sa navodnicima oko cilja tiho ne uspeva da pokrene proces.
            Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "`"$esExe`" > $esLog 2>&1" -WindowStyle Hidden
        }

        if (Test-Port 9000) {
            Write-Ok "MinIO vec radi na 9000"
        } else {
            Write-Host "    Pokrecem MinIO..."
            $minioDataDir = Join-Path (Split-Path $MinioPath -Parent) "minio-data"
            New-Item -ItemType Directory -Force -Path $minioDataDir | Out-Null
            $env:MINIO_ROOT_USER     = "novisad"
            $env:MINIO_ROOT_PASSWORD = "novisad123"
            Start-Process -FilePath $MinioPath `
                -ArgumentList "server", "`"$minioDataDir`"", "--console-address", ":9001" `
                -WindowStyle Hidden
        }

        Write-Host "    Cekam da Elasticsearch i MinIO budu spremni..."
        $esReady    = Wait-ForHttp "http://localhost:9200/_cluster/health" 90 @(200)
        $minioReady = Wait-ForHttp "http://localhost:9000/minio/health/live" 30 @(200)

        if ($esReady -and $minioReady) {
            Write-Ok "Elasticsearch i MinIO su spremni"
            $searchEnabled = $true
            $storageType   = "minio"
        } else {
            Write-Warn2 "Elasticsearch/MinIO se nisu podigli na vreme - nastavljam bez UES dela"
        }
    } else {
        Write-Warn2 "Elasticsearch/MinIO nisu pronadjeni na ovoj masini (trazeno u C:\UES\tools)."
        Write-Warn2 "UES deo (pretraga, PDF indeksiranje) ce biti iskljucen - ostatak aplikacije radi normalno."
        Write-Warn2 "Uputstvo za instalaciju: UPUTSTVO.md, odeljak 4."
    }
} else {
    Write-Step "Elasticsearch/MinIO preskoceni (-SkipEs)"
}

# --- 6. Backend -------------------------------------------------------------

Write-Step "Backend (Spring Boot)"

if (Test-Port 8080) {
    Write-Ok "Nesto vec slusa na 8080 - pretpostavljam da je backend pokrenut"
} else {
    $profileArg = if ($springProfile) { "`"-Dspring-boot.run.profiles=$springProfile`"" } else { "" }

    $dbPasswordLine = ""
    if ($env:DB_PASSWORD) {
        $escapedPwd = $env:DB_PASSWORD -replace "'", "''"
        $dbPasswordLine = "`$env:DB_PASSWORD = '$escapedPwd'"
    }

    $backendScriptLines = @(
        "Set-Location -LiteralPath '$BackendDir'"
        "`$env:JAVA_HOME = '$jdk21'"
        "`$env:Path = '$jdk21\bin;' + `$env:Path"
        "`$env:STORAGE_TYPE = '$storageType'"
        "`$env:SEARCH_ENABLED = '$($searchEnabled.ToString().ToLower())'"
        $dbPasswordLine
        "& .\mvnw.cmd spring-boot:run $profileArg"
    ) | Where-Object { $_ -ne "" }

    $backendLauncher = Join-Path $env:TEMP "novisad-start-backend.ps1"
    Set-Content -Path $backendLauncher -Value ($backendScriptLines -join "`n") -Encoding UTF8

    Write-Host "    Otvaram novi prozor za backend..."
    Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit", "-File", "`"$backendLauncher`""

    Write-Host "    Cekam da backend odgovori (do 90s)..."
    if (Wait-ForHttp "http://localhost:8080/api/auth/me" 90 @(401, 200)) {
        Write-Ok "Backend radi na http://localhost:8080"
    } else {
        Write-Fail "Backend se nije podigao na vreme. Proveri prozor koji je otvoren za backend."
    }
}

# --- 7. Frontend --------------------------------------------------------------

Write-Step "Frontend (Angular)"

if (Test-Port 4200) {
    Write-Ok "Nesto vec slusa na 4200 - pretpostavljam da je frontend pokrenut"
} else {
    $nodeModules = Join-Path $FrontendDir "node_modules"
    $installLine = if (Test-Path $nodeModules) { "" } else { "npm install" }

    $frontendScriptLines = @(
        "Set-Location -LiteralPath '$FrontendDir'"
        $installLine
        "npm start"
    ) | Where-Object { $_ -ne "" }

    $frontendLauncher = Join-Path $env:TEMP "novisad-start-frontend.ps1"
    Set-Content -Path $frontendLauncher -Value ($frontendScriptLines -join "`n") -Encoding UTF8

    Write-Host "    Otvaram novi prozor za frontend..."
    if (-not (Test-Path $nodeModules)) {
        Write-Host "    (node_modules ne postoji, prvo ide npm install - moze potrajati)"
    }
    Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit", "-File", "`"$frontendLauncher`""

    Write-Host "    Cekam da frontend odgovori (do 180s, prvi put moze da potraje)..."
    if (Wait-ForHttp "http://localhost:4200" 180 @(200)) {
        Write-Ok "Frontend radi na http://localhost:4200"
    } else {
        Write-Fail "Frontend se nije podigao na vreme. Proveri prozor koji je otvoren za frontend."
    }
}

# --- 8. Zavrsetak ---------------------------------------------------------------

Write-Step "Gotovo"
Write-Host ""
Write-Host "  Frontend:  http://localhost:4200"
Write-Host "  Backend:   http://localhost:8080"
if ($searchEnabled) {
    Write-Host "  ES/MinIO:  ukljuceni (UES deo aktivan)"
} else {
    Write-Host "  ES/MinIO:  iskljuceni (UES deo nije dostupan na ovoj masini)"
}
Write-Host ""
Write-Host "  Admin nalog: admin@novisad.rs / Admin123!"
Write-Host ""
Write-Host "  Backend i frontend rade u svojim prozorima - zatvori ih (ili Ctrl+C u"
Write-Host "  njima) da ih zaustavis, ili pokreni scripts\stop.ps1."
Write-Host ""

Start-Process "http://localhost:4200"
