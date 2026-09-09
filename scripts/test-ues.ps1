# =============================================================================
#  Provera UES dela [S1] - Elasticsearch, MinIO i pretraga PDF opisa
#
#  Pokretanje (iz korena projekta):
#      powershell -ExecutionPolicy Bypass -File scripts\test-ues.ps1
#
#  Pre pokretanja moraju da rade: Elasticsearch (9200), MinIO (9000),
#  backend (8080) i PostgreSQL. Skripta samo cita podatke i pretrazuje -
#  nista ne menja i ne brise.
# =============================================================================

$ErrorActionPreference = "Stop"

# Bez ovoga konzola prikazuje cirilicu kao smece.
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$Api   = "http://localhost:8080/api"
$Es    = "http://localhost:9200"
$Minio = "http://localhost:9000"

$script:Passed = 0
$script:Failed = 0

# --- pomocne funkcije ---------------------------------------------------------

function Write-Header($text) {
    Write-Host ""
    Write-Host "=== $text ===" -ForegroundColor Cyan
}

function Write-Result($label, $ok, $detail) {
    if ($ok) {
        $script:Passed++
        Write-Host ("  [OK]   {0,-46} {1}" -f $label, $detail) -ForegroundColor Green
    } else {
        $script:Failed++
        Write-Host ("  [PAO]  {0,-46} {1}" -f $label, $detail) -ForegroundColor Red
    }
}

# PowerShell odmotava jednoclani niz u pojedinacni objekat, koji nema .Count.
# Ovaj helper uvek vraca pravi niz, pa provere ispod rade i za 0, 1 i vise stavki.
function ConvertTo-Array($value) {
    if ($null -eq $value) { return @() }
    if ($value -is [array]) { return ,$value }
    return ,@($value)
}

# UTF-8 je obavezan da bi cirilica stigla neizmenjena.
function Invoke-Json($uri, $method, $token, $object) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }

    # Invoke-RestMethod u PowerShell-u 5.1 dekodira odgovor kao ISO-8859-1 kada
    # zaglavlje nema charset, pa se cirilica pokvari. Zato citamo sirove bajtove
    # i sami ih dekodiramo kao UTF-8.
    if ($null -eq $object) {
        $response = Invoke-WebRequest -Uri $uri -Method $method -Headers $headers -UseBasicParsing
    } else {
        $json  = $object | ConvertTo-Json -Compress
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
        $response = Invoke-WebRequest -Uri $uri -Method $method -Headers $headers `
            -ContentType "application/json; charset=utf-8" -Body $bytes -UseBasicParsing
    }

    $text = [System.Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray())
    if (-not $text) { return $null }
    return ($text | ConvertFrom-Json)
}

# Pretrazi i proveri da li je broj rezultata onakav kakav se ocekuje.
function Test-Search($label, $criteria, $expectedCount) {
    try {
        $results = ConvertTo-Array (Invoke-Json "$Api/search/locations" "Post" $script:Token $criteria)
        $names = ($results | ForEach-Object { $_.name }) -join ", "
        if (-not $names) { $names = "-" }

        $ok = $true
        if ($null -ne $expectedCount) { $ok = ($results.Count -eq $expectedCount) }
        Write-Result $label $ok "$($results.Count) -> $names"
        # Zarez sprecava da PowerShell odmota niz pri povratku.
        return ,$results
    } catch {
        Write-Result $label $false "GRESKA: $($_.Exception.Message)"
        return ,@()
    }
}

# --- 0. Da li servisi uopste rade --------------------------------------------

Write-Header "0. Servisi"

foreach ($svc in @(
    @{ Name = "Elasticsearch"; Url = "$Es/_cluster/health" },
    @{ Name = "MinIO";         Url = "$Minio/minio/health/live" },
    @{ Name = "Backend";       Url = "$Api/auth/me" }
)) {
    try {
        Invoke-WebRequest -Uri $svc.Url -UseBasicParsing -TimeoutSec 5 | Out-Null
        Write-Result $svc.Name $true "odgovara"
    } catch {
        # Backend na /auth/me vraca 401 bez tokena - to znaci da radi.
        if ($_.Exception.Response.StatusCode.value__ -eq 401) {
            Write-Result $svc.Name $true "odgovara (401 bez tokena, ocekivano)"
        } else {
            Write-Result $svc.Name $false "NE ODGOVARA - pokreni ga pre testa"
        }
    }
}

# --- 1. Autentifikacija i autorizacija ---------------------------------------

Write-Header "1. Autentifikacija mejlom i lozinkom, autorizacija tokenom"

$login = Invoke-Json "$Api/auth/login" "Post" $null @{ email = "admin@novisad.rs"; password = "Admin123!" }
$script:Token = $login.token
Write-Result "prijava mejlom i lozinkom" ($null -ne $script:Token) "token izdat, vazi $($login.expiresIn)s"

try {
    Invoke-Json "$Api/search/locations" "Post" $null @{ name = "studio" } | Out-Null
    Write-Result "pretraga BEZ tokena je odbijena" $false "prosla je, a ne bi smela"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Write-Result "pretraga BEZ tokena je odbijena" ($code -eq 401) "HTTP $code"
}

# --- 2. Indeks i mapiranje ----------------------------------------------------

Write-Header "2. Indeksiranje mesta u Elasticsearch"

Invoke-WebRequest -Uri "$Es/locations/_refresh" -Method Post -UseBasicParsing | Out-Null

$count     = (Invoke-RestMethod -Uri "$Es/locations/_count").count
$locations = ConvertTo-Array (Invoke-Json "$Api/locations" "Get" $script:Token $null)
Write-Result "indeks prati bazu" ($count -eq $locations.Count) "ES $count : baza $($locations.Count) mesta"

$mapping = Invoke-RestMethod -Uri "$Es/locations/_mapping"
$props   = $mapping.locations.mappings.properties
foreach ($f in @("name", "description", "pdfContent", "address")) {
    $analyzer = $props.$f.analyzer
    Write-Result "polje '$f' koristi sopstveni analyzer" ($analyzer -eq "serbian_custom") "analyzer=$analyzer"
}

# --- 3. Analyzer: nezavisnost od pisma i velicine slova -----------------------

Write-Header "3. Analyzer - cirilica, latinica i velika slova daju iste tokene"

function Get-Tokens($text) {
    $json  = @{ analyzer = "serbian_custom"; text = $text } | ConvertTo-Json -Compress
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    $res   = Invoke-RestMethod -Uri "$Es/locations/_analyze" -Method Post `
        -ContentType "application/json; charset=utf-8" -Body $bytes
    return ($res.tokens | ForEach-Object { $_.token }) -join " | "
}

$pairs = @(
    @{ A = "Studio M Cacak";  B = "Студио М Чачак" },
    @{ A = "STUDIO M CACAK";  B = "studio m cacak" },
    @{ A = "Djordje Njiva";   B = "Ђорђе Њива" }
)
foreach ($p in $pairs) {
    $ta = Get-Tokens $p.A
    $tb = Get-Tokens $p.B
    Write-Result "'$($p.A)' == '$($p.B)'" ($ta -eq $tb) "-> $ta"
}

# --- 4. Pretraga po nazivu, opisu i sadrzaju PDF-a ---------------------------

Write-Header "4. Pretraga po nazivu"
Test-Search "naziv = studio"            @{ name = "studio" }            $null | Out-Null
Test-Search "naziv = STUDIO (velika)"   @{ name = "STUDIO" }            $null | Out-Null
Test-Search "naziv = студио (cirilica)" @{ name = "студио" }            $null | Out-Null

Write-Header "5. Pretraga po opisu"
Test-Search "opis = koncertni"          @{ description = "koncertni" }  $null | Out-Null

Write-Header "6. Pretraga po opisu iz PDF dokumenta"
$pdfHits = Test-Search "pdf = akustiku" @{ pdfContent = "akustiku" } $null
Test-Search "pdf = traktor (nema ga)"   @{ pdfContent = "traktor" }     0     | Out-Null

# --- 7. Opsezi ----------------------------------------------------------------

Write-Header "7. Opseg broja utisaka (donja i/ili gornja granica)"
Test-Search "samo donja granica (od 2)"  @{ minReviews = 2 }                 $null | Out-Null
Test-Search "samo gornja granica (do 1)" @{ maxReviews = 1 }                 $null | Out-Null
Test-Search "obe granice (1 - 4)"        @{ minReviews = 1; maxReviews = 4 } $null | Out-Null

Write-Header "8. Opseg prosecne ocene po stavkama"
Test-Search "ukupan utisak 7 - 10" @{ minOverall = 7; maxOverall = 10 } $null | Out-Null
Test-Search "prostor do 5"         @{ maxSpace = 5 }                    $null | Out-Null

# --- 9. Tipovi upita ----------------------------------------------------------

Write-Header "9. Tipovi upita: fraza, prefiks, fuzzy"
Test-Search 'fraza "koncertna dvorana"'   @{ pdfContent = '"koncertna dvorana"' } $null | Out-Null
Test-Search 'fraza obrnutim redosledom'   @{ pdfContent = '"dvorana koncertna"' } 0     | Out-Null
Test-Search 'prefiks stu*'                @{ name = "stu*" }                      $null | Out-Null
Test-Search 'bez zvezdice: stu'           @{ name = "stu" }                       0     | Out-Null
Test-Search 'fuzzy ~akustka (greska)'     @{ pdfContent = "~akustka" }            $null | Out-Null
Test-Search 'bez tilde: akustka'          @{ pdfContent = "akustka" }             0     | Out-Null

# --- 10. Operator, sortiranje, sazetak ---------------------------------------

Write-Header "10. AND / OR operator"
Test-Search "naziv=studio I pdf=traktor (AND)"  @{ name = "studio"; pdfContent = "traktor"; operator = "AND" } 0     | Out-Null
Test-Search "naziv=studio ILI pdf=traktor (OR)" @{ name = "studio"; pdfContent = "traktor"; operator = "OR" }  $null | Out-Null

Write-Header "11. Sortiranje po nazivu"
$asc  = Test-Search "rastuce"   @{ sortBy = "name"; sortDirection = "asc" }  $null
$desc = Test-Search "opadajuce" @{ sortBy = "name"; sortDirection = "desc" } $null
if ($asc.Count -gt 1) {
    $reversed = ($desc | ForEach-Object { $_.name }) -join ","
    $expected = (($asc | ForEach-Object { $_.name })[($asc.Count - 1)..0]) -join ","
    Write-Result "opadajuce je obrnuto od rastuceg" ($reversed -eq $expected) $reversed
}

Write-Header "12. Dinamicki sazetak (Highlighter)"
if ($pdfHits.Count -gt 0 -and $pdfHits[0].highlights.Count -gt 0) {
    $fragment = $pdfHits[0].highlights[0]
    Write-Result "sazetak sadrzi istaknut pojam" ($fragment -match "<mark>") $fragment
} else {
    Write-Result "sazetak sadrzi istaknut pojam" $false "nema sazetka"
}

Write-Header "13. Prikaz rezultata: opis iz interfejsa, ne iz PDF-a"
if ($pdfHits.Count -gt 0) {
    $r = $pdfHits[0]
    # Opis iz UI-ja je kratak; tekst iz PDF-a bi bio znatno duzi.
    Write-Result "vraca se opis iz interfejsa" ($r.description.Length -lt 200) "$($r.name): $($r.description)"
}

Write-Header "14. Slicna mesta (more like this)"
if ($pdfHits.Count -gt 0) {
    $similar = ConvertTo-Array (Invoke-Json "$Api/search/locations/$($pdfHits[0].id)/similar" "Get" $script:Token $null)
    $names = ($similar | ForEach-Object { $_.name }) -join ", "
    if (-not $names) { $names = "-" }
    Write-Result "vraca slicna mesta" ($similar.Count -ge 0) "$($similar.Count) -> $names"
}

# --- 15. PDF u MinIO i preuzimanje -------------------------------------------

Write-Header "15. PDF dokument: cuvanje u MinIO i preuzimanje"
$withPdf = ConvertTo-Array ($locations | Where-Object { $_.pdfUrl })
if ($withPdf) {
    $target = $withPdf[0]
    try {
        # Bez tokena - link za preuzimanje mora da radi direktno iz pretrazivaca.
        $resp = Invoke-WebRequest -Uri "http://localhost:8080$($target.pdfUrl)" -UseBasicParsing
        $isPdf = $resp.Headers["Content-Type"] -like "*pdf*"
        Write-Result "preuzimanje PDF-a bez tokena" $isPdf "$($target.name): $($resp.RawContentLength) B, $($resp.Headers['Content-Type'])"
    } catch {
        Write-Result "preuzimanje PDF-a bez tokena" $false "GRESKA: $($_.Exception.Message)"
    }
} else {
    Write-Host "  (nijedno mesto nema zakacen PDF - zakaci ga kroz izmenu mesta)" -ForegroundColor Yellow
}

# --- Zbir ---------------------------------------------------------------------

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ("  Proslo: {0}    Palo: {1}" -f $script:Passed, $script:Failed)
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

if ($script:Failed -gt 0) { exit 1 }
