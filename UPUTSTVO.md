# Novi Sad — uputstvo za pokretanje

Implementirano do sada: **K1** (zahtev za registraciju), **K2** (prijava i odjava), **A1**
(obrada zahteva), **K3** (rukovanje mestima), **A2** (upravljanje menadžerima mesta) i
**K4/M1** (rukovanje događajima) i **K5** (utisci i ocene mesta). Specifikacija celog projekta je u [README.md](README.md).

## Tehnologije

| Sloj      | Tehnologija                                        |
|-----------|----------------------------------------------------|
| Backend   | Spring Boot 3.5.16, Java 21, Maven (wrapper)       |
| Bezbednost| Spring Security 6 + JWT (jjwt 0.12.7), BCrypt      |
| Baza      | PostgreSQL 17                                       |
| Frontend  | Angular 19 (standalone komponente, signals)         |
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

## 4. Pokretanje frontend-a

```powershell
cd frontend
npm start
```

Aplikacija je na `http://localhost:4200`.

## 5. Predefinisan administrator

Pri prvom pokretanju backend kreira administratora sistema:

- email: `admin@novisad.rs`
- lozinka: `Admin123!`

Menja se kroz `app.admin.*` u konfiguraciji (ili `ADMIN_EMAIL` / `ADMIN_PASSWORD`).

## 6. Tok za demonstraciju

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
| GET    | `/api/locations`                              | prijavljen | K3 |
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
| GET    | `/api/events?today=`                          | prijavljen | K4  |
| GET    | `/api/events/{id}`                            | prijavljen | K4  |
| GET    | `/api/events/{id}/image`                      | **javno** | K4   |
| PUT    | `/api/events/{id}`                            | menadžer mesta ili ADMIN | K4 |
| DELETE | `/api/events/{id}`                            | menadžer mesta ili ADMIN | K4 |
| GET    | `/api/locations/{id}/reviews`                 | prijavljen | K5  |
| POST   | `/api/locations/{id}/reviews`                 | prijavljen | K5  |
| GET    | `/api/locations/{id}/reviewable-events`       | prijavljen | K5  |
| GET    | `/api/reviews/{id}`                           | prijavljen | K5  |

Autorizacija: `Authorization: Bearer <token>`. Token važi 24h (`app.jwt.expiration-seconds`).

## Struktura

```
backend/src/main/java/rs/ftn/uns/novisad/
  model/         User, AccountRequest, Location, Manages, Event, Review, Rate, Comment,
                 Role, RequestStatus, LocationType, EventType, RateCategory
  repository/    Spring Data JPA repozitorijumi
  service/       AccountRequestService (K1/A1), AuthService (K2),
                 LocationService (K3), ManagerService (A2), EventService (K4/M1),
                 ReviewService (K5)
  storage/       StorageService + LocalFileSystemStorageService (slike mesta)
  security/      JwtService, JwtAuthenticationFilter, UserDetailsService, 401/403 handleri
  config/        SecurityConfig, CorsConfig, AdminSeeder
  web/           REST kontroleri
  dto/           zahtevi i odgovori
  exception/     GlobalExceptionHandler

frontend/src/app/
  core/          modeli, servisi, JWT interceptor, guard-ovi
  features/      auth (login, register), admin (zahtevi), locations (lista,
                 stranica mesta, forma), events (stranica događaja, forma),
                 reviews (forma za utisak), home
  shared/        navbar
```

## Napomene za dalji rad

- **Slanje mejlova** (A1 nakon obrade zahteva, K9 nakon promene lozinke) još nije
  implementirano — mesta su označena `TODO` u `AccountRequestService`.
- **Uloga MANAGER** prati stanje u tabeli `manages`: korisnik postaje `MANAGER` kada dobije
  prvo mesto, a vraća se na `USER` kada mu se ukloni poslednje. Administrator ne može biti
  menadžer mesta.
- **`ddl-auto: update`** je pogodan za razvoj. Pred predaju preći na `validate` uz migracije.
- **UES deo** (Elasticsearch, MinIO, PDF full-text pretraga, S1) nije započet.
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
