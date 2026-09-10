# Novi Sad — uputstvo za pokretanje

## Brzi start

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start.ps1
```

ili dvoklik na `scripts\start.bat`. Skripta sama pronalazi JDK 21, proverava
Node.js i PostgreSQL, pokreće Elasticsearch i MinIO ako su instalirani (inače
ih preskače i UES deo isključi bez rušenja aplikacije), pa pokreće backend i
frontend u zasebnim prozorima i otvara pretraživač. Radi na bilo kojoj Windows
mašini — ne oslanja se na `JAVA_HOME` iz tvog profila niti na fiksne putanje.

Zaustavljanje: `powershell -ExecutionPolicy Bypass -File scripts\stop.ps1`
(ili `scripts\stop.bat`) — gasi backend, frontend, Elasticsearch i MinIO;
PostgreSQL ne dira jer je to deljeni sistemski servis.

Parametri `start.ps1`: `-SkipEs` (ne pokušavaj Elasticsearch/MinIO),
`-EsPath`/`-MinioPath` (druge putanje), `-DbPassword` (izbegava interaktivno
pitanje za lozinku baze).

Ostatak ovog fajla opisuje isti postupak ručno, korak po korak — koristan kad
nešto treba podesiti drugačije nego što skripta pretpostavlja.


Implementirano do sada: **K1** (zahtev za registraciju), **K2** (prijava i odjava), **A1**
(obrada zahteva), **K3** (rukovanje mestima), **A2** (upravljanje menadžerima mesta) i
**K4/M1** (rukovanje događajima), **K5** (utisci i ocene mesta), **K6** (pretraga i
filtriranje), **K9** (promena lozinke), **K10** (profil korisnika) i **UES deo** (Elasticsearch,
MinIO, PDF full-text pretraga, **S1**). Specifikacija celog projekta je u [README.md](README.md).

## Tehnologije

| Sloj      | Tehnologija                                        |
|-----------|----------------------------------------------------|
| Backend   | Spring Boot 3.5.16, Java 21, Maven (wrapper)       |
| Bezbednost| Spring Security 6 + JWT (jjwt 0.12.7), BCrypt      |
| Baza      | PostgreSQL 17                                       |
| Frontend  | Angular 19 (standalone komponente, signals)         |
| Pretraga  | Elasticsearch 8.19.5 (sopstveni analyzer)           |
| Fajlovi   | MinIO (slike i PDF dokumenti)                       |
| PDF       | Apache PDFBox 3 (izvlačenje teksta)                 |
| Testovi   | JUnit 5 + MockMvc nad in-memory H2                  |

## 1. Priprema baze

PostgreSQL 17 servis već radi na portu `5432`. Kreiraj bazu (jednom):

```powershell
& "C:\Program Files\PostgreSQL\17\bin\createdb.exe" -U postgres uesnovisad
```

Komanda će tražiti lozinku `postgres` korisnika. Tabele pravi Hibernate pri prvom
pokretanju (`ddl-auto: update`).

## 2. Lozinka baze

> **Ne diraj `application.yml`.** Lozinka ide isključivo u `application-local.yml`, koji je
> u `.gitignore`-u. `application.yml` sadrži samo podrazumevane vrednosti sa `${...}`
> placeholder-ima i mora ostati kompletan — ako se iz njega obrišu ključevi poput
> `app.admin.first-name`, aplikacija neće startovati.

Napravi **novi** fajl `backend/src/main/resources/application-local.yml` (postoji
`.example` verzija kao predložak) i u njega upiši samo ono što se razlikuje:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/uesnovisad
    username: postgres
    password: TVOJA_LOZINKA
```

Alternativa — postavi promenljive okruženja `DB_URL`, `DB_USER`, `DB_PASSWORD`.
Podrazumevane vrednosti su `jdbc:postgresql://localhost:5432/uesnovisad`, `postgres`, `postgres`.

## 3. Pokretanje backend-a

`JAVA_HOME` mora da pokazuje na JDK 21 (na sistemu je `C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot`;
na `PATH`-u je inače JRE 8, koji ne radi):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot"
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Backend sluša na `http://localhost:8080`.

Testovi (ne traže pokrenut PostgreSQL, koriste H2):

```powershell
.\mvnw.cmd test
```

## 4. Pokretanje Elasticsearch-a i MinIO-a (UES deo)

Oba servisa su instalirana u `C:\UES\tools` i pokreću se bez Dockera.

**Terminal 3 — Elasticsearch** (`http://localhost:9200`):

```powershell
C:\UES\tools\start-elasticsearch.bat
```

**Terminal 4 — MinIO** (API `:9000`, konzola `http://localhost:9001`):

```powershell
C:\UES\tools\start-minio.bat
```

MinIO pristup: korisnik `novisad`, lozinka `novisad123`. Bucket `novisad` se pravi sam
pri prvom pokretanju backend-a.

> **Podešeno za razvoj:** u `elasticsearch.yml` je isključen bezbednosni sloj
> (bez TLS-a i lozinke) i spušteni su pragovi zauzeća diska na apsolutne vrednosti
> (2gb / 1gb / 500mb). Podrazumevani procentualni pragovi (85/90/95%) blokiraju
> alokaciju shardova na skoro punom disku.

Ako se indeks raziđe sa bazom, kao admin: **Pretraga → Ponovo indeksiraj**
(ili `POST /api/search/reindex`).

**Rad bez ES-a i MinIO-a:** postavi `STORAGE_TYPE=local` i `SEARCH_ENABLED=false` —
aplikacija tada čuva fajlove na disk i preskače indeksiranje. Testovi tako i rade.

## 5. Pokretanje frontend-a

```powershell
cd frontend
npm start
```

Aplikacija je na `http://localhost:4200`.

## 6. Predefinisan administrator

Pri prvom pokretanju backend kreira administratora sistema:

- email: `admin@novisad.rs`
- lozinka: `Admin123!`

Menja se kroz `app.admin.*` u konfiguraciji (ili `ADMIN_EMAIL` / `ADMIN_PASSWORD`).

## 7. Tok za demonstraciju

1. `/registracija` — pošalji zahtev za registraciju **[K1]**
2. Pokušaj prijave tim nalogom → `403` uz poruku da zahtev nije obrađen
3. Prijavi se kao admin → `/admin/zahtevi` → **Prihvati** **[A1]**
4. Prijavi se novim nalogom **[K2]**
5. Klikni **Odjavi se** u navigaciji **[K2]**

Za K3 i A2:

1. Kao admin: `/mesta` → **Dodaj mesto** (naziv, adresa, tip, opis, slika) **[K3]**
2. Na stranici mesta → sekcija **Menadžeri mesta** → dodeli korisnika **[A2]**
   (uloga mu automatski postaje `MANAGER`)
3. Prijavi se kao taj korisnik → na stranici mesta klikni **Izmeni atribute** **[K3]**
   (menja adresu, tip i opis; naziv i sliku ne)
4. Kao admin ukloni menadžera sa mesta → vraća se na ulogu `USER` **[A2]**

Za K4 i M1 (kao menadžer mesta):

1. Na stranici mesta → **Dodaj događaj** (naziv, adresa, tip, datum, redovan?, cena
   ili besplatno, slika) **[K4]**
2. Događaj se pojavljuje u sekciji **Predstojeći događaji** na stranici mesta **[K3]**
3. **Prikaži i održane** → vide se i događaji koji su prošli
4. Na stranici događaja: **Izmeni** / **Ukloni događaj** **[M1]**
5. Za redovan događaj sa više pojava istog naziva prikazuje se **Održan do sada N puta** **[K5]**

Za K5 (kao bilo koji prijavljen korisnik):

1. Na stranici mesta → **Ostavi utisak** **[K5]**
2. Bira se **redovan događaj koji se već održao** — samo takvi se nude
3. Ocenjuju se stavke 1-10: nastup, zvuk i svetlo, prostor, ukupan utisak. Nije
   neophodno oceniti svaku; ponovni klik na istu ocenu je poništava
4. Komentar je opcion
5. Nakon objave: srednja ocena mesta se osvežava na stranici mesta i u listi mesta **[K3]**

Za K6:

1. `/mesta` → traka za pretragu: naziv ili adresa + tip mesta
2. `/dogadjaji` → podrazumevano **današnji događaji sa svih mesta**; filteri: tip, mesto,
   datum (bilo koji u prošlosti ili budućnosti), besplatan/plaćen ulaz i raspon cene
3. Dugme **Svi datumi** skida ograničenje na jedan dan

Za K9 i K10:

1. Klik na svoje ime u navigaciji → `/profil`
2. **Podaci profila** — ime, prezime, grad, telefon (email se ne menja) **[K10]**
3. **Promeni sliku** → izaberi fajl → **Sačuvaj sliku** **[K10]**
4. **Promena lozinke** — trenutna, pa dva puta nova **[K9]**
5. Ispod: **Mesta kojima upravljam** i **Moji utisci** **[K10]**

Za UES deo (S1):

1. Kao admin: `/mesta` → izmeni mesto → dodaj **PDF sa opisom mesta**
   (tekst se čita PDFBox-om i indeksira u Elasticsearch)
2. `/pretraga` → pretraži po **nazivu**, **opisu** ili **opisu iz PDF-a**
3. Probaj posebne oblike unosa:
   - `"koncertna dvorana"` — tačna fraza (obrnut redosled ne vraća ništa)
   - `akust*` — prefiks
   - `~akustka` — toleriše grešku u kucanju
4. Probaj **ćirilicu**: `студио` vraća isto što i `studio` i `STUDIO`
5. Opsezi: broj utisaka i prosečna ocena po stavkama (od–do)
6. **AND / OR** između popunjenih polja, **sortiranje po nazivu**
7. U rezultatu: **dinamički sažetak** sa istaknutim pojmom, **Preuzmi PDF opis**
   i **Slična mesta** (more-like-this)

## REST API

| Metoda | Putanja                                       | Pristup | Zahtev |
|--------|-----------------------------------------------|---------|--------|
| POST   | `/api/registration-requests`                  | javno   | K1     |
| POST   | `/api/auth/login`                             | javno   | K2     |
| POST   | `/api/auth/logout`                            | prijavljen | K2  |
| GET    | `/api/auth/me`                                | prijavljen | —   |
| GET    | `/api/admin/registration-requests?all=`       | ADMIN   | A1     |
| POST   | `/api/admin/registration-requests/{id}/approve` | ADMIN | A1     |
| POST   | `/api/admin/registration-requests/{id}/reject`  | ADMIN | A1     |
| GET    | `/api/locations?query=&type=`                 | prijavljen | K3/K6 |
| GET    | `/api/locations/{id}`                         | prijavljen | K3 |
| GET    | `/api/locations/{id}/image`                   | **javno** | K3 |
| POST   | `/api/locations`                              | ADMIN   | K3     |
| PUT    | `/api/locations/{id}`                         | ADMIN   | K3     |
| PATCH  | `/api/locations/{id}/attributes`              | ADMIN ili menadžer tog mesta | K3 |
| DELETE | `/api/locations/{id}`                         | ADMIN   | K3     |
| GET    | `/api/admin/users`                            | ADMIN   | A2     |
| GET    | `/api/admin/locations/{id}/managers`          | ADMIN   | A2     |
| POST   | `/api/admin/locations/{id}/managers`          | ADMIN   | A2     |
| DELETE | `/api/admin/locations/{id}/managers/{userId}` | ADMIN   | A2     |
| GET    | `/api/locations/{id}/events?all=`             | prijavljen | K4  |
| POST   | `/api/locations/{id}/events`                  | menadžer mesta ili ADMIN | K4 |
| GET    | `/api/locations/{id}/events/permissions`      | prijavljen | K4  |
| GET    | `/api/events?query=&type=&locationId=&date=&freeEntry=&minPrice=&maxPrice=&allDates=` | prijavljen | K4/K6 |
| GET    | `/api/events/{id}`                            | prijavljen | K4  |
| GET    | `/api/events/{id}/image`                      | **javno** | K4   |
| PUT    | `/api/events/{id}`                            | menadžer mesta ili ADMIN | K4 |
| DELETE | `/api/events/{id}`                            | menadžer mesta ili ADMIN | K4 |
| GET    | `/api/locations/{id}/reviews`                 | prijavljen | K5  |
| POST   | `/api/locations/{id}/reviews`                 | prijavljen | K5  |
| GET    | `/api/locations/{id}/reviewable-events`       | prijavljen | K5  |
| GET    | `/api/reviews/{id}`                           | prijavljen | K5  |
| GET    | `/api/users/me`                               | prijavljen | K10 |
| PUT    | `/api/users/me`                               | prijavljen | K10 |
| POST   | `/api/users/me/image`                         | prijavljen | K10 |
| GET    | `/api/users/{id}/image`                       | **javno** | K10  |
| POST   | `/api/users/me/password`                      | prijavljen | K9  |
| POST   | `/api/search/locations`                       | prijavljen | S1  |
| GET    | `/api/search/locations/{id}/similar`          | prijavljen | S1  |
| POST   | `/api/search/reindex`                         | ADMIN   | UES    |
| GET    | `/api/locations/{id}/pdf`                     | **javno** | UES  |

Autorizacija: `Authorization: Bearer <token>`. Token važi 24h (`app.jwt.expiration-seconds`).

## Struktura

```
backend/src/main/java/rs/ftn/uns/novisad/
  model/         User, AccountRequest, Location, Manages, Event, Review, Rate, Comment,
                 Role, RequestStatus, LocationType, EventType, RateCategory
  repository/    + LocationSpecifications i EventSpecifications (dinamicki filteri [K6])
  service/       AccountRequestService (K1/A1), AuthService (K2),
                 LocationService (K3), ManagerService (A2), EventService (K4/M1),
                 ReviewService (K5), UserProfileService (K9/K10), EmailService (A1/K9)
  storage/       StorageService + LocalFileSystemStorageService + MinioStorageService
  search/        LocationDocument, LocationIndexService, LocationSearchService,
                 SearchQueryParser, CyrillicTransliterator, PdfTextExtractor [UES]
  security/      JwtService, JwtAuthenticationFilter, UserDetailsService, 401/403 handleri
  config/        SecurityConfig, CorsConfig, AdminSeeder
  web/           REST kontroleri
  dto/           zahtevi i odgovori
  exception/     GlobalExceptionHandler

frontend/src/app/
  core/          modeli, servisi, JWT interceptor, guard-ovi
  features/      auth (login, register), admin (zahtevi), locations (lista,
                 stranica mesta, forma), events (stranica događaja, forma),
                 events/list (stranica događaja [K6]), reviews (forma za utisak),
                 profile (podaci, slika, lozinka, utisci [K9]/[K10]),
                 search (napredna pretraga [S1]), home
  shared/        navbar
```

## Napomene za dalji rad

- **Slanje mejlova** je implementirano (`EmailService`) za A1 i K9, ali je
  **isključeno** (`app.mail.enabled: false`) — poruke se beleže u log umesto da se šalju.
  Za stvarno slanje: postavi `MAIL_ENABLED=true` i `spring.mail.*` podatke (host, port,
  username, password) u `application-local.yml`. Kod se ne menja.
- **Testovi rade nad H2, a aplikacija nad PostgreSQL-om.** Dve greške su prošle testove
  a pukle na Postgresu (rezervisana reč `VALUE`, i `null` parametri u dinamičkim
  upitima). Zato su filteri [K6] pisani preko `Specification`-a umesto
  `(:param is null or ...)` obrasca. Vredi svaku novu funkcionalnost proveriti i na
  živoj bazi, ne samo testovima.
- **Uloga MANAGER** prati stanje u tabeli `manages`: korisnik postaje `MANAGER` kada dobije
  prvo mesto, a vraća se na `USER` kada mu se ukloni poslednje. Administrator ne može biti
  menadžer mesta.
- **`ddl-auto: update`** je pogodan za razvoj. Pred predaju preći na `validate` uz migracije.
- **Sopstveni analyzer** (`elasticsearch/location-settings.json`) preslikava ćirilicu u
  latinicu i uklanja dijakritike pre indeksiranja i pre izvršavanja upita, pa `студио`,
  `Studio` i `STUDIO` daju iste tokene. Ugrađeni Serbian Analyzer takođe preslikava
  ćirilicu u latinicu (kroz `serbian_normalization`), ali koristi Serbian Light Stemmer,
  koji je previše agresivan za nazive i adrese: `Чачак` svodi na koren `cack` umesto na
  `cacak`, `Студио` na `studi` umesto na `studio`. Zato je sopstvena konfiguracija
  neophodna, kako specifikacija i traži — provereno u
  `scripts/test-ues.ps1` (sekcija 3).
- **`minimum_should_match` za more-like-this je `1`**, ne podrazumevanih `30%`. Mesto sa
  zakačenim PDF-om daje mnogo termina pa procentualni prag nikada nije bio ispunjen —
  testirano je 30%, 25%, 20%, 10%, 2 i 1. Kada baza naraste, prag treba podići.
- **Elasticsearch nije izvor istine** — relaciona baza jeste. Greška pri indeksiranju se
  beleži u log, ali ne obara poslovnu operaciju; indeks se popravlja preko `reindex`.
- **Pretraga bez ES-a vraća `503`, ne `500`.** Kada je `SEARCH_ENABLED=false` (nema
  Elasticsearch-a na mašini), `/api/search/*` vraća jasnu poruku umesto da propadne;
  `LocationIndexService` je i ranije poštovao tu zastavicu za indeksiranje, ali
  `LocationSearchController` ju je za same upite ignorisao dok skripta za pokretanje
  (`scripts/start.ps1`) to nije otkrila.
- **Aktivan obim S1 je namerno sužen na traženo:** pretraga po nazivu, opisu i sadržaju
  PDF-a, i opseg broja utisaka. Kod za BooleanQuery (AND/OR), PhraseQuery/PrefixQuery/
  FuzzyQuery, opseg ocene po kategorijama, sortiranje po nazivu, Highlighter i
  "more like this" postoji i radi u `LocationSearchService`, ali je zakomentarisan u
  `search()` i u `LocationSearchController` (endpoint `/similar`) — nije obrisan, samo
  isključen. Otkomentarisati po potrebi.
- **Uzgred popravljeno:** nepostojeća ruta je vraćala `500` umesto `404`, jer je
  `GlobalExceptionHandler` hvatao `NoResourceFoundException` istim handler-om kao i
  prave greške. Dodat poseban handler za taj slučaj.
- **Slike mesta** se čuvaju u folderu `backend/uploads/` iza interfejsa `StorageService`.
  Kada dođe UES deo, dodaje se `MinioStorageService` i menja `app.storage.type` — ostatak
  koda ostaje netaknut.
- **Srednja ocena mesta** je prosek svih datih ocena (svih stavki) na aktivnim utiscima.
  Uklonjen utisak [M2] se ne računa, sakriven se i dalje računa — kako specifikacija traži.
- **Jedan utisak po korisniku i događaju.** Specifikacija to ne kaže eksplicitno, ali
  utisak se vezuje za konkretnu pojavu događaja, pa dupliranje nema smisla (vraća `409`).
- **Nesaglasnost u specifikaciji:** K5 navodi 4 stavke ocenjivanja (zvuk i svetlo su
  jedna), dok UES deo [S1] pominje 5 (zvuk i svetlo odvojeno). Implementirane su 4, po K5.
- **Redovan događaj** se u bazi vodi kao više pojava sa istim nazivom na istom mestu i
  različitim datumima. Zato je "koliko se puta događaj održao" (K5) broj pojava tog
  naziva na tom mestu čiji je datum u prošlosti. Specifikacija ovo ne definiše
  eksplicitno — ako profesor traži drugačije, menja se samo upit u `EventRepository`.
