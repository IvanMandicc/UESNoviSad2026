# Novi Sad — uputstvo za pokretanje

Implementirano do sada: **K1** (zahtev za registraciju), **K2** (prijava i odjava), **A1**
(obrada zahteva), **K3** (rukovanje mestima) i **A2** (upravljanje menadžerima mesta). Specifikacija celog projekta je u [README.md](README.md).

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

Autorizacija: `Authorization: Bearer <token>`. Token važi 24h (`app.jwt.expiration-seconds`).

## Struktura

```
backend/src/main/java/rs/ftn/uns/novisad/
  model/         User, AccountRequest, Location, Manages, Role, RequestStatus, LocationType
  repository/    Spring Data JPA repozitorijumi
  service/       AccountRequestService (K1/A1), AuthService (K2),
                 LocationService (K3), ManagerService (A2)
  storage/       StorageService + LocalFileSystemStorageService (slike mesta)
  security/      JwtService, JwtAuthenticationFilter, UserDetailsService, 401/403 handleri
  config/        SecurityConfig, CorsConfig, AdminSeeder
  web/           REST kontroleri
  dto/           zahtevi i odgovori
  exception/     GlobalExceptionHandler

frontend/src/app/
  core/          modeli, servisi, JWT interceptor, guard-ovi
  features/      auth (login, register), admin (zahtevi), locations (lista,
                 stranica mesta, forma), home
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
- **Stranica mesta** ima dve prazne sekcije koje čekaju svoje zahteve: predstojeći
  događaji (K4) i prosečna ocena mesta (K5).
