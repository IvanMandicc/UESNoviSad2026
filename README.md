Novi-Sad
Potrebno je implementirati aplikaciju po uzoru na New-Now za podršku pronalaska
događaja i manifestacija.
Korisnik aplikacije ima na raspolaganju sledeće funkcionalnosti:
[K1] Zahtev za registraciju korisnika. Pristigle zahteve za registraciju obrađuje
administrator sistema (zahtev A1). Administrator sistema je predefinisan korisnik u
sistemu.
[K2] Prijava i odjava sa sistema. Kada se korisnik uspešno prijavi na aplikaciju
omogućiti korisniku da se odjavi.
[K3] Rukovanje mestima1. Obavezna polja su naziv, adresa, tip mesta i opis. Mesto
mora da sadrži sliku. Jedino administrator sistema može da rukuje mestom, dok
menadžer mesta može da ažurira attribute mesta, poput adrese, tipa mesta i opisa. Na
stranici mesta se nalaze svi predstojeći događaji. Pored naziva mesta prikazati i
ukupnu, srednju vrednost ocene mesta.
[K4] Rukovanje događajima. Obavezna polja događaja su naziv događaja, mesto,
adresa, tip događaja, datum, da li je događaj redovan, cena ulaska, ili informacija da
je događaj besplatan. Jedino menadžer mesta može da rukuje događajima. Događaj
mora da sadrži jednu sliku
[K5] Ostavljanje utiska (engl. review) na mesto. Prilikom pisanja utiska formira se
ocena mesta i opciono ostavlja komentar. Na utisku se bira događaj koji se održao na
datom mestu i nakon toga je moguće oceniti mesto prema: 1) nastupu 2) zvuku i
svetlu; 3) prostoru; 4) ukupnom utisku; na skali od 1-10, gde svaka od navedenih
stavki ima svoju ocenu, gde nije neophodno oceniti svaku stavku. Korisnici mogu da
ostave utisak samo ukoliko je događaj označen kao redovan i ukoliko se izabrani
dogadjaj već održao na datom mestu u trenutku pisanja utiska. Na recenziji prikazati
informaciju koliko se puta događaj ukupno održao u trenutku pisanja recenzije.
[K6] Pretraga i filtriranje mesta i događaja. Na stranici za mesta moguće je pretražiti
sva mesta prema nazivu, adresi ili tipu mesta. Na stranici za događaje vide se današnji
događaji sa svih mesta i moguće je pretražiti i filtrirati događaje prema tipu, mestu,
adresi ili ceni. Moguće je odabrati i filtrirati događaje koji se održavaju na
proizvoljom datumu u prošlosti ili budućnosti.
[K7] Sortiranje utisaka. Na stranici mesta utiske je moguće sortirati po ocenama i
datumu objavljivanja rastuće i opadajuće.
[K8] Pregled početne stranice. Korisnici na početnoj stranici vide događaje sa svih
mesta koji se održavaju danas. Korisnici takođe mogu da vide “najpopularnija
mesta”, tj mesta sa najviše pozitivnih ocena kao i najskorija 3 utiska sa
najpopularnijeg mesta.
[K9] Promena lozinke. Pri promeni lozinke prvo se unosi trenutna lozinka i dva puta
se unosi nova lozinka. Nakon promene lozinke korisniku se šalje mejl.
[K10] Promena dodatnih podataka na profilu kao i sliku. Na profilu se vidi spisak
svih utisaka, i mesta na kojima je korisnik menadžer.
Skraćeno od Sistem za Upravljanje Bazama Podataka.

Projekat (UES) 2026
Menadžer mesta ima na raspolaganju sve funkcionalnosti koje ima i registrovan korisnik,
kao i:
[M1] Ažuriranje atributa na mestu i rukovanje događajima. (nastavak na K4).
[M2] Rukovanje utiscima. Menadžer ima uvid u sve utiske koji su ostavljeni na
objektu od strane korisnika i može da sakrije ili ukloni pojedinačne utiske. Uklonjeni
utisci se logički brišu i ocena sa utiska poništava i ne računa se u ukupnoj oceni
mesta. Ocene ostavljene na sakrivenom utisku se i dalje računaju u ukupnoj oceni
objekta.
[M3] Ostavljanje komentara na utisku. Moguće je odgovoriti (eng. reply) na utisak.
Ukoliko je menadžer odgovorio na utisak, običan korisnik može odgovoriti na
komentar menadžera. Moguć je proizvoljan broj odgovora na komentar (reply na
reply na reply…).
[M4] Analitika (engl. analytics) mesta. Za svako mesto na kojem je korisnik
menadžer, moguće je videti ukupan broj događaja u izabranom periodu, podelu na
redovne i neredovne, kao i na besplatne i plaćene događaje, top-liste događaja i mesta
sa najvišim i najnižim prosečnim ocenama i najskorija tri utiska sa najpopularnijeg
mesta. Statistika i top-liste su dostupne na nedeljnom, mesečnom i godišnjem nivou,
kao i za proizvoljno definisan vremenski opseg (početni i krajnji datum). [Za predmet
KVA]: Koristiti grafičku biblioteku za vizuelizaciju, poput Chart.js, Apache ECharts
ili sl.
Administrator sitema ima na raspolaganju sve funkcionalnosti koje ima menadžer
objekta i registrovan korisnik, ali i:
[A1] nastavak na K1. Obrada zahteva za registraciju. Administrator ima pravo da
prihvati ili odbije zahtev za registraciju. Korisnik može da se prijavi na sistem tek
kada je zahtev za registraciju prihvaćen. Nakon što je zahtev obrađen, korisniku se
šalje mejl.
[A2] nastavak na K3. Upravljanje mestima i menadžerima za mesta. Administrator
može da dodeli korisniku sistema ulogu menadžera mesta. Moguće je dodati i ukloniti
menadžere sa mesta. Menadžer koji je uklonjen sa objekta postaje običan korisnik.
UES DEO:
Omogućiti indeksiranje mesta u Elasticsearch-u, omogućiti čuvanje slika i dokumenata u
MinIO bazi. Dodatno, implementirati funkcionalnost da se pored slika, za mesto može
upload-ovati PDF dokument koji sadrži opis mesta u slobodnoj formi (full-text).
[S1] Pretraga mesta:
 Pretraživanje mesta po nazivu
 Pretraživanje mesta po opisu
 Pretraživanje mesta po opisu iz zakačenog PDF fajla (sadržaj PDF-a se parsira i
indeksira u ES kao Text polje)
 Pretraživanje mesta po opsegu broja review-a (od - do), gde može biti zadata
donja i/ili gornja granica opsega
2
Projekat (UES) 2026
 Pretraživanje mesta po prosečnoj oceni (po kategorijama: nastup, zvuk, svetlo,
prostor, ukupan utisak) u opsegu (od - do), gde može biti zadata donja i/ili gornja
granica opsega
 Kombinacija prethodnih parametara pretrage (BooleanQuery, omogućiti AND i
OR operator između polja)
 Pretprocesirati upit, tako da bude nezavisan od velikog i malog slova, kao i
ćiriličnog ili latiničnog pisma (morate pisati svoju konfiguraciju za analyzer, nije
dovoljno samo iskoristiti ugrađeni Serbian Analyzer)
 Obezbediti podršku u poljima forme za unos PhrazeQuery (kada je vrednost
polja unešena pod dvostrukim navodnicima), PrefixQuery (kada je vrednost polja
unešena sa karakterom „*“ npr. „Ivan M*“) i FuzzyQuery (empirijski odredite distancu
koja vam odgovara, pokreće se kada vrednost polja počinje sa karakterom „~“ npr.
„~rizika“)
 Implementirati „more like this“ pretragu koja će uzeti u obzir polja: naslov, opis
i opis iz PDF-a. Min/max term frequency podesiti empirijski.
Prilikom prikaza rezultata prikazati naziv mesta, opis mesta (onaj koji se zadaje iz UI-a,
ne treba onaj iz PDF-a) , omogućite sortiranje rezultata po nazivu (smislite kako da
dopunite indeksnu strukturu kako biste podržali ovo) i dinamički sažetak (Highlighter). Iz
prikaza svih mesta omogućiti preuzimanje PDF-a sa opisom objekta
Za implementaciju aplikacije iskoristiti sledeće softverske pakete:
● Za Serverske veb tehnologije:
o Spring framework
o Apache Tomcat (ne mora biti posebno integrisan, može Spring Boot)
o MySQL ili PostgreSQL
● Za Klijentske veb tehnologije
o Angular framework
● Za UES
o Elasticsearch
o MinIO
Podatke kojima upravlja aplikacija organizovati uz oslonac na SUBP2.
Nefunkcionalni zahtevi
Podržati autentifikaciju korisnika upotrebom korisničkog mejla i lozinke i autorizaciju
korisnika upotrebom mehanizma tokena.
Beležiti poruke o važnim događajima koji su nastali prilikom izvršavanja aplikacije.
Pod rukovanjem se podrazumevaju aktivnosti vezane za dodavanje, promenu i uklanjanje odgovarajućih
pojava entiteta. Većinu informacionih sistema karakteriše neograničen period čuvanja podataka te se
aktivnost uklanjanja odgovarajućih pojava entiteta retko koristi.
3
Projekat (UES) 2026
Arhitektura aplikacije
Aplikacija je raspoređena na tri uređaja: Veb pretraživač, Spring kontejner (u Tomcat
serveru ili pokrenut pomoću Spring Boot) i SUBP. Dijagram rasporeda prikazan je na
slici 1.
Slika 1: Arhitektura aplikacije – dijagram rasporeda
Back-end aplikaciju implementirati upotrebom Spring framework-a [1], uz oslonac na
Spring Boot [2]. Front-end aplikacija mora postojati i komunicira sa back-end
aplikacijom putem RESTful servisa. Kao SUBP koristiti MySQL Server [3] ili neki drugi
relacioni SUBP. Za beleženje poruka koristiti log4j API [4]. Za izgradnju softvera
koristiti Apache Maven [5] ili neki drugi alat, a dozvoljeno je i koristiti Spring Boot i na
taj način konfigurisati i pokretati aplikaciju. Za formiranje ocene potrebno je
demonstrirati funkcionalnosti kroz korisnički interfejs na oba predmeta.
Veb aplikacija je Angular aplikacija i ocenjuje se na Klijentskim veb arhitekturama
(KVA). Ukoliko postoje studenti koji su položili Softverske veb tehnologije (SVT), a
nisu položili KVA- backend deo rešenja može biti realizovan kroz Firebase [6] ili neki
sličan servis.
Model podataka
Na linku prikazan je model podataka aplikacije. Neregistrovan korisnik (entitet
AccountRequest) ima jedino mogućnost slanja zahteva za registraciju na aplikaciji.
4
Projekat (UES) 2026
Entitet User predstavlja registrovanog korisnika aplikacije i namenjen je skladištenju
podataka koji se koriste za autentifikaciju i autorizaciju. Korisnik takođe može biti
administrator sistema (entitet Administrator) ili menadžer mesta (entitet Manages). Mesto
je opisano entitetom Location, dok su događaji opisani entitetom Event. Entitet Review
predstavlja utisak od strane korisnika. Pri utisku, korisnik može da navede ocene vezane
za objekat (entitet Rate), ali može ostaviti i komentar (entitet Comment)