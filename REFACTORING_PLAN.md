# Findly / ProductScout – Code Review und verbindlicher Refactoring-Plan

Stand der Prüfung: 2026-09-12, Branch `master`, Commit `94bea417e907d597fa48795f6184c6e440685b21`.

## Fortschritt am 2026-09-19 – Schritt 8

- Formular auf die tatsächlich unterstützte Inserats-URL reduziert; leere
  Eingabe statt Mock-Vorgaben. Start erfolgt einmalig per POST mit anschließendem
  Wechsel nach `/results/{id}`. Die Ergebnisseite unterstützt Direktaufruf und
  Neuladen ohne erneuten Analyseauftrag.
- Gemeinsamer Vertrag in `frontend/src/types/analysis.ts`: Zod validiert Antworten
  zur Laufzeit und leitet TypeScript-Typen daraus ab. Detaillierte Backend-Status,
  Nullwerte, Kandidaten, Extraktions-/Preisbelege und Vergleiche sind abgebildet.
  Fehlerhafte Antworten und fremde Analyse-IDs werden abgewiesen.
- Sequenzielles Status-Polling mit 1/2/5 Sekunden Backoff (danach 5 Sekunden).
  `COMPLETED` lädt `/result`, `FAILED` beendet Polling. 429 berücksichtigt
  `Retry-After`, 409 führt zurück zur Statusabfrage. AbortController beendet
  laufende Requests und Timer beim Seitenwechsel; verspätete Antworten können
  keine andere Analyse überschreiben. Lesefehler bieten einen erneuten Abruf
  derselben Analyse statt eines weiteren POST.
- Problem Details werden strukturiert ausgewertet und über stabile Codes
  lokalisiert; rohe Antworttexte und interne Fehlermeldungen erscheinen nicht
  im UI. Trace-IDs bleiben für die Fehlersuche sichtbar.
- Synthetische Marktwerte, Deal-Scores, Transportkosten, Distanzen, Fahrzeug-
  Defaults und Kaufempfehlungen vollständig aus dem Frontend entfernt, alte
  Mock-Datei und Decision-Typen gelöscht. Fehlende Werte heißen „Unbekannt“.
  Originalpreis, historischer Originalpreis und Gebrauchtmarkt bleiben getrennt;
  vorzeichenbehaftete Ersparnis/Aufpreis stammen ausschließlich vom Backend.
- Kandidaten unter 75 %, fehlende eindeutige Zuordnung, Identifier-Konflikte,
  Einzel-Scores, Gewichte und Warnungen werden dargestellt. Quellen nennen
  Abrufzeit und Confidence, erlauben nur HTTPS-Links und öffnen ohne Opener-
  Zugriff. Transport/Kaufentscheidung ist ausdrücklich noch nicht verfügbar.
- Vitest/React Testing Library eingerichtet und in Frontend-CI aufgenommen.
  19 Tests prüfen API-Vertrag, Fehlerredaktion, Navigation, Polling-Abstände,
  Terminalzustände, Rate Limits, Abbruch, späte Antworten, doppelte Starts,
  Nullwerte, unsichere Kandidaten, Quellenlinks und negative Ersparnis.

Verifikation: Lokale Lint-/TypeScript-Prüfung und 19 Tests erfolgreich. Derselbe
Quellstand besteht im Node-20-Container zusätzlich den Next.js-Produktionsbuild;
Lint, TypeScript, Tests und Build melden dort keine Warnungen. Der gestartete
Produktionsserver liefert `/` und `/results/{id}` mit HTTP 200 und passendem
Seiteninhalt; `git diff --check` besteht ebenfalls. Der lokale
Turbopack-Build ist weiterhin durch die Portbindungsbeschränkung der Umgebung
blockiert. Bei `npm ci` bleibt die bekannte Deprecation-Warnung zu ESLint 9;
lokal zusätzlich die Installationsskript-Warnung zu unrs-resolver. Keine Regeln
oder Tests deaktiviert. Backend und vollständiger Compose-Stack wurden für diese
Frontend-Phase nicht erneut abgenommen. Reale Provider-Adapter bleiben offen.

Nächster fachlicher Schritt ist Schritt 9: das persönliche „Lohnt sich“-Modul
mit tatsächlichen Eingaben und belegten Routing-/Fahrzeug-/Miet-/Kraftstoffdaten.
Ohne diese Daten bleiben Transportkosten und Empfehlungen unbekannt.

## Fortschritt am 2026-09-19 – Schritt 7

- V7 ergänzt Product, ExtractedAttribute, ProductMatch und PriceEvidence als
  persistierte, typisierte JSONB-Evidenz-Snapshots. Kandidaten behalten Quellen,
  Attribute, Einzel-Scores, angewandte Gewichte und Rang. Ein partieller Unique-
  Index erzwingt höchstens ein ausgewähltes Match pro Analyse.
- Konfigurierbares gewichtetes Matching mit Normalisierung fehlender Merkmale,
  Unicode-/Textnormalisierung, EAN-8-/EAN-13-Prüfziffern und markengebundenen SKUs.
  Auswahl ab 0,75 mit mindestens 0,10 Abstand; beide Grenzwerte inklusive und
  nur nach oben konfigurierbar. Identifier-/Markenkonflikte verhindern Auswahl;
  Marke/Kategorie allein genügen nicht. Unbelegte Kandidatenfelder zählen nicht
  als Übereinstimmung. Confidence beschreibt Evidenzübereinstimmung, keine
  kalibrierte Wahrscheinlichkeit.
- Preisvalidierung akzeptiert ausschließlich vollständige positive Beträge mit
  ISO-Währung, HTTPS-Quelle, Abruf innerhalb der letzten 30 Tage, Preisart,
  Confidence >= 0,75 und expliziter Quellenklasse. Original-/historische Preise
  priorisieren Hersteller, aktuelle Gebrauchtpreise Marktplätze; nur die beste
  vorhandene Quellenklasse derselben Preisart/Währung fließt in den Median ein.
- Ergebnisvertrag um Kandidaten, Extraktionsbelege, Preisbelege und getrennte
  Vergleiche erweitert. Ersparnis bleibt vorzeichenbehaftet; negative Werte
  heißen `SURCHARGE`. Fremdwährungen werden nicht verrechnet, unbekannte Werte
  bleiben null. Alte synthetische Ergebnisse bleiben als unverifiziert markiert.
- Deterministische Tests für Scores/Gewichte, Grenzwerte, Abstand, Identifier,
  fehlende/widersprüchliche Evidenz und Preisakzeptanz. PostgreSQL-Workflow-Test
  prüft Provider ohne Transaktion, Persistenz, API-Ausgabe und Aufpreis; Flyway-
  Upgrade-Test prüft den partiellen Unique-Index und unvollständige Preisbelege.

Backend-Abnahmelauf: `DOCKER_HOST=unix:///var/run/docker.sock ./mvnw -B spotless:apply clean verify`
erfolgreich mit **166 Tests, 0 Fehlern, 0 übersprungenen Tests**, Enforcer, Spotless
und SpotBugs. `git diff --check` ebenfalls erfolgreich. Bekannte Java-/Dependency-
und Entwicklungsbenutzer-Warnungen bleiben offen; keine Quality Gates oder Tests
wurden deaktiviert. Die warning-freie Gesamtabnahme bleibt deshalb offen.

Nächster fachlicher Schritt ist Schritt 8 (Frontend-Vertrag,
Polling, Nullwerte, Kandidaten/Quellen und Entfernung clientseitiger Fake-Werte).
Reale Provider-Adapter bleiben offen; die Standardkonfiguration erfindet weiterhin
keine Produkte oder Preise. Frontend und Compose wurden in Schritt 7 nicht geändert.

## Fortschritt am 2026-09-19 – Schritte 4, 5 und 6

Schritt 4 – Listing-Client:

- Apache HttpClient verwendet einen prüfenden Resolver direkt im Socket-Connector;
  validierte Adressobjekte werden an den Verbindungsaufbau gebunden. Private,
  Loopback-, Link-Local-, Multicast-, unspecified und weitere nicht öffentliche
  IPv4-/IPv6-Adressen sowie gemischte DNS-Antworten werden abgelehnt. Die normale
  TLS-Zertifikats- und Hostnamenprüfung bleibt aktiv.
- Maximal drei vollständig neu validierte Redirects, keine automatischen
  Redirects/Retry-/Cookie-/Kompressionsmechanismen. Verbindungen werden je Hop
  neu aufgebaut; Redirect-Bodies werden nicht vollständig eingelesen.
- Validierte Konfiguration für Connect-/Read-Timeout und maximal 10 MiB.
  Streaming-Abbruch nach Limit + 1 Byte und zusätzliche absolute Request-Deadline
  verhindern unbegrenzte Downloads bzw. endloses Tröpfeln von Antwortdaten.
- Jsoup ersetzt HTML-Regexes. Konkrete Inseratsfelder, Produkt-JSON-LD und Meta-Daten
  liefern Evidenz; Beschreibungsbeträge oder Neupreise werden nicht als Kaufpreis
  übernommen. Fehlende, ungültige oder widersprüchliche Preise/Währungen bleiben
  `null`. Blockierte Zugriffe und Parserfehler erhalten stabile Fehlercodes.

Schritt 5 – asynchrone Verarbeitung:

- Start persistiert URL, `CREATED` und Fünf-Minuten-Deadline in einer abgeschlossenen
  kurzen Transaktion, bevor Scheduling erfolgt. Executor: Core 2, Max 4, Queue 20,
  `AbortPolicy`; volle Queues liefern 429 und hinterlassen einen dauerhaft
  fehlgeschlagenen Job statt Arbeit im Request-Thread auszuführen.
- `AnalysisStatusService` übernimmt Status-/Ergebnisschreibzugriffe in separaten
  `REQUIRES_NEW`-Transaktionen. Atomarer Claim unter Zeilensperre verhindert doppelte
  Verarbeitung; HTTP-/Provider-Aufrufe liegen außerhalb dieser Transaktionen.
- Statusfolge: `CREATED → FETCHING_LISTING → EXTRACTING_LISTING →
  IDENTIFYING_PRODUCT → RESEARCHING_PRICES → COMPLETED`; Fehler sind aus jedem
  nichtterminalen Zustand möglich. Das tatsächliche `MVP.md` enthält weiterhin nur
  die alten Grobzustände; die Zwischenzustände konkretisieren hier den Plan.
- Deadline umfasst Wartezeit in der Queue. Timer unterbrechen Jobs; ein DB-Sweep
  markiert abgelaufene Jobs auch nach Neustart. Keine automatische Wiederaufnahme:
  unterbrochene Jobs bleiben bis zur ursprünglichen Deadline nachvollziehbar und
  werden dann `FAILED`. Späte Antworten überschreiben keine terminalen Zustände.
- Neue Migrationen V3–V5 für unbekannte Inseratspreise/Währungen, URL/Deadline,
  Statusumstellung und persistierte Warnungen; vorhandene Migrationen unverändert.

Schritt 6 – Provider ohne erfundene Ergebnisse:

- OCR-, Vision-, Search- und Pricing-Schnittstellen mit unveränderlichen Domain-DTOs
  statt JPA-Entities. Typisierte, validierte Konfiguration und bedingte Beans;
  `DISABLED` und `LOCAL_STUB` liefern ausschließlich `UNAVAILABLE` ohne Treffer.
- Keyword-Modellzuordnungen, feste Confidence-Werte und synthetische Preisbänder
  aus den Backend-Services entfernt. Bild-URLs sind keine analysierten Bildinhalte.
  Ohne ausgewähltes Produkt erfolgt kein Pricing-Aufruf; Suchtreffer werden vor
  Implementierung des Matchers nicht als eindeutige Identifikation ausgegeben.
- Erfolgreich extrahierte Inserate können als Teilergebnis `COMPLETED` werden:
  Produkt, Marktpreise und Deal bleiben unbekannt; persistierte Warnungen enthalten
  u. a. `SEARCH_UNAVAILABLE`, `PRODUCT_NOT_IDENTIFIED` und `PRICE_NOT_VERIFIED`.
  Abruf-/Parsingfehler bleiben `FAILED`; COMPLETED ohne Inseratsergebnis ist verboten.
- V6 markiert historische abgeschlossene Ergebnisse als `LEGACY_RESULT_UNVERIFIED`.
  Historische Daten bleiben erhalten, ihre synthetischen Produkt-/Preisbehauptungen
  werden vom Ergebnis-Endpunkt aber nicht als Evidenz ausgegeben.

Tests umfassen lokale HTTP-Fixtures (JDK-Testserver, keine Live-Webseiten),
DNS-/Redirect-/Größen-/Timeoutfälle, Parser- und Provider-DTO-Validierung,
PostgreSQL-/Flyway-Upgrades, HTTP 202 vor Abschluss eines blockierten Abrufs,
Transaktionsfreiheit der Provider, konkurrierende Claims, Queue-Überlauf,
Deadline-Unterbrechung, terminale Zustände und Teilergebnisse ohne API-Keys.

Abnahmelauf: `DOCKER_HOST=unix:///var/run/docker.sock ./mvnw -B spotless:apply clean verify`
erfolgreich mit **151 Tests, 0 Fehlern und 0 übersprungenen Tests**; Enforcer,
Spotless und SpotBugs bestanden. `git diff --check` ebenfalls erfolgreich. Der
Docker-Socket wurde pro Befehl gesetzt, weil der voreingestellte Desktop-Kontext
nach dem Umgebungsneustart nicht lief; globale Docker-Einstellungen unverändert.
Frontend und vollständiger Compose-Stack wurden für diese Backend-Änderungen
nicht erneut abgenommen.

Zum Abschluss von Schritten 4–6 offen waren Schritt 7 (inzwischen siehe oben), reale
Provider-Adapter und Schritt 8 (Frontend-Polling, neuer Vertrag, Nullwerte und
Entfernung clientseitiger Fake-Berechnungen). Bekannte Toolchain-/Dependency-
Warnungen verhindern weiterhin die warning-freie Gesamtabnahme. OS-DNS-Auflösung
kann trotz Job-Abbruch bis zum Resolver-Timeout blockieren; die persistierte
Deadline und der Schutz vor verspäteten Ergebnissen bleiben wirksam.

## Fortschritt am 2026-09-19 – API-Fehlervertrag und URL-Validierung

Die fachlichen Änderungen aus Schritt 3 sind umgesetzt:

- Zentraler `AnalysisErrorCode` und ein gemeinsames Problemformat für Controller
  und Rate-Limit-Filter: `application/problem+json`, stabiler `code`, UTC-Zeitpunkt
  sowie identische Trace-ID im Body und im `X-Trace-ID`-Header.
- Ungültiges JSON, UUIDs und Request-Validierungsfehler liefern 400; unbekannte
  Analysen 404; laufende Analysen am Ergebnis-Endpunkt 409; fehlgeschlagene
  Analysen 422. Gespeicherte Provider-/Timeout-Codes werden auf 502/504 abgebildet.
  Ein inkonsistenter COMPLETED-Datensatz ist ein interner Fehler (500), kein 404.
  Framework-Statuscodes 405/406/415 bleiben erhalten.
- Interne Exception-Texte werden weder über Problem Details noch über den
  Status-Endpunkt ausgegeben, auch nicht bei bereits gespeicherten Fehlern.
- Gemeinsame syntaktische URL-Allowlist für Controller, Service und Listing-Client:
  HTTPS, exakte Kleinanzeigen-Hosts, Ports 80/443, maximal 2048 Zeichen,
  Normalisierung und Ablehnung von Userinfo, Fragmenten und Host-Verwechslungen.
  Ungültige URLs werden vor Persistenz und Scheduling abgewiesen; diese Prüfung
  führt keine DNS- oder Netzwerkaufrufe aus.
- CORS und Rate Limits über validierte `@ConfigurationProperties` statt
  Feldinjektion konfiguriert. CORS erlaubt nur explizite Origins, GET/POST/OPTIONS
  und benötigte Header; `X-Trace-ID` und `Retry-After` sind im Browser lesbar.
- Thread-sicheres, begrenztes Rate-Limit mit getrennten 60-Sekunden-Fenstern für
  Erstellung (20/min) und kombinierte Status-/Ergebnisabfragen (120/min) je IP.
  Maximal 10000 aktive IP/Operations-Fenster, abgelaufene Einträge werden entfernt.
  Preflights verbrauchen kein Budget; `X-Forwarded-For` umgeht das Limit nicht.
- README, `.env.example` und Compose dokumentieren bzw. übergeben die neuen
  Konfigurationswerte und den Fehlervertrag.

Verifikation: Backend-Lauf mit 84 erfolgreichen Tests einschließlich
PostgreSQL/Flyway, URL-Grenzfällen, allen Fehlercodes, Secret-Redaktion,
konkurrierenden Rate-Limit-Zugriffen, Ablauf/Speichergrenzen, CORS und ungültiger
Konfiguration. Enforcer, Spotless und SpotBugs bestanden. Die bereits dokumentierten
Java-/Dependency-Warnungen verhindern weiterhin eine warning-freie Gesamtabnahme;
keine Regeln wurden abgeschaltet oder Befunde unterdrückt.

Nächster Schritt ist Schritt 4: SSRF-sicherer Listing-Client mit gebundener
DNS-Auflösung, kontrollierten Redirects, tatsächlichem Streaming-Größenlimit,
konfigurierbaren Timeouts und HTML-Parser. Die bisherige DNS-/HTTP-Implementierung
ist dadurch noch nicht gehärtet. Ebenso bleiben die Async-Probleme, allgemeine
Fehlerklassifizierung im Orchestrator, Laufzeitbegrenzung und synthetischen
Produkt-/Preisdaten aus späteren Schritten offen. Rate Limits gelten derzeit pro
Anwendungsinstanz und sind nicht zwischen mehreren Instanzen geteilt.

## Fortschritt am 2026-09-18

Abgleich mit `b8d7d22`: Die bisherigen Phase-1-Commits haben Teile der Schritte 1–5
angelegt, aber keinen dieser Schritte vollständig abgenommen. Die nachfolgenden
Review-Befunde beschreiben den ursprünglichen Stand; diese Fortschrittsnotiz hält
die inzwischen überprüften Änderungen fest.

In dieser Fortsetzung umgesetzt:

- Neue Migration `V2__add_analysis_processing_metadata.sql` ergänzt die bereits
  im Entity verwendeten Versions-, Fortschritts-, Zeit- und Fehlerfelder. V1 bleibt
  unverändert. Bestehende terminale Analysen erhalten Fortschritt 100; eine
  Datenbank-Constraint begrenzt den Fortschritt auf 0–100.
- PostgreSQL-Tests prüfen das Upgrade von V1 mit vorhandenen Analysen, die erneute
  Flyway-Ausführung, Fortschrittsgrenzen, JPA-Persistenz und optimistische Sperren.
- Testcontainers von 1.20.6 auf 1.21.4 aktualisiert: Der vorherige Client scheiterte
  am aktuellen Docker mit „client version 1.32 is too old“.
- SpotBugs-Befund zur nicht wiederhergestellten transienten Filter-Map durch
  Verwendung von `OncePerRequestFilter` behoben; vorhandene Spotless-Abweichungen
  formatiert. Keine Quality Gates deaktiviert.
- Compose verwendet dieselben konfigurierten Datenbankzugänge für PostgreSQL und
  Backend. Frontend und Compose verwenden `NEXT_PUBLIC_API_URL` mit einer aus dem
  Browser erreichbaren Adresse. Der Frontend-Healthcheck verwendet explizit IPv4,
  passend zum Next.js-Listener (`0.0.0.0`); `localhost` wurde im Alpine-Container
  auf `::1` aufgelöst und führte zu „Connection refused“. README um Start- und
  Migrationshinweise ergänzt.

Verifikation:

- Backend `./mvnw -B spotless:apply clean verify`: erfolgreich; 9 Tests, keine
  Fehler oder übersprungenen Tests; Enforcer, Spotless und SpotBugs erfolgreich.
- **Noch nicht warning-frei:** Java-25-/Unsafe-Warnungen aus Build-Werkzeugen,
  dynamische Mockito-Agent-Anbindung, Testcontainers/JUnit-CloseableResource und
  automatisch erzeugter Spring-Security-Entwicklungsbenutzer bleiben zu beheben.
- `npm ci` erfolgreich, jedoch Warnungen zu ESLint 9 und einem durch die lokale
  npm-Konfiguration blockierten Installationsskript (`unrs-resolver`).
- Frontend `npm run lint:ci` und `npm run typecheck`: erfolgreich. Production Build
  im Compose-Frontend-Image erfolgreich und ohne Build-Warnungen. Der lokale
  Build scheitert an der Portbindungsbeschränkung der Ausführungsumgebung für
  Turbopack; der Container-Build prüft denselben Quellstand.
- Compose-Konfiguration syntaktisch validiert; isolierter Stack
  `findly-refactor-check` ohne Host-Portbelegung erfolgreich gestartet:
  PostgreSQL, Backend und Frontend jeweils `healthy`.
- `git diff --check`: erfolgreich. Die vollständige Definition of Done bleibt
  wegen der oben genannten Warnungen und fachlichen Lücken offen.

Nächste fachliche Schritte bleiben URL-/Fehlervertrag und SSRF-Client, danach die
vollständige State Machine mit transaktionsfreien Provider-Aufrufen. Besonders
kritisch: Async-Scheduling erfolgt derzeit vor dem Commit; `CallerRunsPolicy`
kann Arbeit in den Request verlagern. Produkt-/Preis-Hardcodings und die
Frontend-Fake-Berechnungen sind weiterhin vorhanden. Diese Fortsetzung macht
keine Aussage über Produktionsreife oder den Abschluss der Schritte 3–10.

## Auftrag an GitHub Copilot

Dieses Dokument ist die verbindliche Arbeitsanweisung für das Refactoring. Arbeite in kleinen, fachlich geschlossenen Commits. Nach jedem Commit müssen alle für den betroffenen Teil verfügbaren Quality Gates ohne Fehler und ohne Warnungen durchlaufen. Überspringe keine fehlgeschlagenen Prüfungen, deaktiviere keine Regeln und füge keine Suppressions hinzu, um Befunde zu verdecken.

Die Produktspezifikation in `MVP.md` ist fachliche Grundlage. Bei Widersprüchen gilt dieses Dokument für die Reihenfolge und die Sicherheitskorrekturen. Implementiere keine erfundenen Provider, Preise, Produktdaten, Entfernungen, Transportkosten oder Confidence-Werte.

## Review-Ergebnis

**Gesamturteil: Das Repository bildet die Oberfläche der Idee teilweise ab, erfüllt aber weder den MVP-Workflow noch die Sicherheits- und Evidenzanforderungen. Es ist derzeit ein nicht produktionsfähiger Prototyp mit synthetischer Geschäftslogik.**

Was bereits vorhanden ist:

- Monorepo-Grundstruktur mit Spring Boot Backend und Next.js Frontend.
- Java 25 und eine konkrete Spring-Boot-4-Version sind im Backend konfiguriert.
- Next.js 16, React 19, TypeScript und Tailwind CSS sind vorhanden.
- `POST /api/analyses` und `GET /api/analyses/{id}` existieren.
- Listing-, Produkt-, Preis- und Deal-Modelle sind rudimentär angelegt.
- Das Frontend zeigt Eingabeformular, Ergebnis, Deal-Score und Transportbereich.
- Das Frontend kompiliert und der Production Build ist erfolgreich.

Was das Projekt aktuell tatsächlich macht:

1. `POST /api/analyses` legt eine Analyse mit `PENDING` an.
2. Noch innerhalb desselben HTTP-Requests und derselben JPA-Transaktion lädt `ListingFetchService` die übergebene URL synchron.
3. HTML wird mit regulären Ausdrücken nach Titel, Beschreibung, Preis und Bild-URLs durchsucht.
4. Scheitert der Abruf oder das Parsing, liefert der Service scheinbar gültige Platzhalterdaten statt eines Fehlers.
5. Fehlt der Inseratspreis, erfindet `AnalysisService.detectListingPrice(...)` abhängig von URL-Schlüsselwörtern 129 €, 290 € oder 149 €.
6. `ProductIdentificationService` erkennt einige Schlüsselwörter und ordnet feste Modelle und Confidence-Werte zu, darunter unbelegte Modelle wie Ray-Ban Clubmaster, Oakley Jawbreaker oder Nike AeroFlex.
7. `PriceResearchService` erzeugt drei angebliche Preisquellen als Inseratspreis minus 15 %, Inseratspreis und Inseratspreis plus 15 %. Das sind keine recherchierten Quellen.
8. Der Datensatz wird sofort auf `COMPLETED` gesetzt und als HTTP 202 zurückgegeben, obwohl keine asynchrone Verarbeitung stattfindet.
9. Das Frontend lädt direkt danach einmalig das Ergebnis. Es gibt kein Status-Polling.
10. Das Frontend berechnet weitere erfundene Daten: Transportkosten aus 15 % der Preisdifferenz plus 15 €, Distanz 90 km, Gesamtstrecke 180 km und Fahrzeit 150 Minuten.

Damit verletzt der aktuelle Stand die wichtigste Produktregel: Findly darf bei fehlender Evidenz keine scheinbar verifizierten Aussagen erzeugen.

## Kritische Befunde

### P0 – Halluzinierte Preise und Quellen

Betroffene Dateien:

- `backend/src/main/java/dev/thomcgn/findly/analysis/AnalysisService.java`
- `backend/src/main/java/dev/thomcgn/findly/price/PriceResearchService.java`
- `frontend/src/lib/api/analysis.ts`

Probleme:

- `detectListingPrice(...)` erzeugt Preise ohne Quelle.
- `PriceResearchService` leitet vermeintliche Marktpreise nur aus dem Inseratspreis ab.
- Die Angebots-URL wird fälschlich als Quelle für drei berechnete Vergleichspreise verwendet.
- Das Frontend behandelt den höchsten synthetischen Marktwert als Originalpreis.
- Fehlende numerische Werte werden durch `toNumber(...)` still zu `0` und verlieren damit die Bedeutung „unbekannt“.
- Ersparnisse werden mit `Math.max(0, ...)` abgeschnitten; ein Aufpreis wird dadurch verborgen.

Verbindliche Änderung:

- Sämtliche Fallback-, Demo- und Schätzwerte aus dem produktiven Pfad entfernen.
- Unbekannte Preise als `null` modellieren.
- `PriceEvidence` nur speichern, wenn Preis, Währung, Quellenname, Quellen-URL, Abrufzeitpunkt, Preisart und Confidence vorhanden und validiert sind.
- Originalpreis, historischer Originalpreis und Gebrauchtmarktwert als getrennte Konzepte modellieren.
- Preisvergleich ausschließlich bei kompatibler Währung und verifiziertem Referenzpreis berechnen.
- Negative Ersparnis ausdrücklich als Aufpreis ausgeben.
- Bei deaktiviertem Preisprovider `PRICE_NOT_VERIFIED` beziehungsweise ein klares Teilergebnis liefern, niemals einen Wert synthetisieren.

### P0 – SSRF und unkontrollierter URL-Abruf

Betroffene Datei: `backend/src/main/java/dev/thomcgn/findly/listing/ListingFetchService.java`.

Probleme:

- Beliebige Nutzer-URLs werden mit Serverrechten abgerufen.
- Keine Domain-Allowlist, DNS-/IP-Prüfung, Portprüfung oder Redirect-Neuvalidierung.
- Redirects werden automatisch verfolgt.
- Keine Connect-/Read-Timeouts und kein Antwortgrößenlimit.
- Fehler werden verschluckt und in Platzhalterdaten umgewandelt.

Verbindliche Änderung:

- Vor Speicherung und Abruf ausschließlich `https://kleinanzeigen.de/...` und `https://www.kleinanzeigen.de/...` akzeptieren; Ports nur 80/443 gemäß Spezifikation.
- Host normalisieren, Userinfo ablehnen und IDN/Punycode korrekt behandeln.
- DNS-Auflösung prüfen und Loopback-, private, link-local, multicast, unspecified sowie Metadata-Adressen für IPv4 und IPv6 blockieren.
- Redirects nicht automatisch verfolgen. Jeden Redirect vollständig neu validieren; maximal drei.
- DNS-Rebinding-Risiko berücksichtigen: Ziel-IP beim Verbindungsaufbau an die zuvor validierte Auflösung binden oder einen Client/Resolver verwenden, der diese Garantie ermöglicht.
- Konfigurierbare Connect-/Read-Timeouts, Maximalgröße 10 MiB und Content-Type-Prüfung einführen.
- HTTP- und Parserfehler auf stabile `AnalysisErrorCode`-Werte abbilden.
- Kein CAPTCHA, Login oder Anti-Bot-System umgehen.

### P0 – Synchroner Netzwerkzugriff in langer Transaktion

Betroffene Datei: `backend/src/main/java/dev/thomcgn/findly/analysis/AnalysisService.java`.

Probleme:

- `createAnalysis(...)` ist `@Transactional` und führt URL-Abruf, Erkennung und Preislogik darin aus.
- Trotz HTTP 202 ist die Verarbeitung vollständig synchron.
- Status- und Fehlerfortschritt werden nicht dauerhaft schrittweise gespeichert.

Verbindliche Änderung:

- Startoperation auf Validierung, kurze Persistenz und Scheduling begrenzen.
- `Analysis` mit `CREATED` und Fortschritt 0 speichern und sofort HTTP 202 zurückgeben.
- Begrenzten `ThreadPoolTaskExecutor` mit Core 2, Max 4, Queue 20 und Prefix `analysis-` konfigurieren.
- Einen Orchestrator ohne übergreifendes `@Transactional` einführen.
- Statuswechsel und Ergebnis-Persistierung über getrennte Beans mit jeweils kurzen `REQUIRES_NEW`-Transaktionen ausführen.
- Externe Aufrufe dürfen nie bei aktiver JPA-Transaktion stattfinden.
- Maximale Laufzeit von fünf Minuten erzwingen und Fehler dauerhaft speichern.
- Doppelverarbeitung über atomaren Claim/optimistische Sperre verhindern.

### P0 – Produktidentifikation behauptet unbelegte Modelle

Betroffene Datei: `backend/src/main/java/dev/thomcgn/findly/product/ProductIdentificationService.java`.

Probleme:

- Schlüsselwörter führen unmittelbar zu konkreten Produktmodellen und festen Confidence-Werten.
- Bild-URLs werden als inhaltliche Evidenz behandelt, ohne Bilder zu analysieren.
- Keine Kandidatensuche, kein gewichtetes Matching, keine Identifier-Regeln und keine Zweitplatz-Abstandsregel.

Verbindliche Änderung:

- Heuristische Hardcodings vollständig aus dem Produktionspfad entfernen.
- Provider-Schnittstellen für OCR, Vision, Search und Pricing einführen; Provider liefern reine Domain-DTOs, keine Entities.
- Ohne SearchProvider keine eindeutige Produktidentifikation vortäuschen.
- Kandidaten, Einzel-Scores und Evidenz persistieren.
- Konfigurierbare Gewichte verwenden; fehlende Merkmale normalisieren.
- EAN-/SKU-Regeln sowie Mindest-Confidence 0,75 und Abstand zum Zweitplatzierten 0,10 implementieren.
- Unterhalb der Schwelle eine Kandidatenliste und deutliche Unsicherheit liefern.

### P0 – Keine kontrollierte Datenbankmigration

Betroffene Konfiguration: `backend/src/main/resources/application.properties`.

Probleme:

- H2 ist der Standard statt PostgreSQL.
- `spring.jpa.hibernate.ddl-auto=update` verändert das Schema automatisch.
- Flyway ist aktiviert, aber es existieren keine Migrationen.
- Docker Compose und `.env.example` fehlen.

Verbindliche Änderung:

- PostgreSQL als Standardlaufzeit konfigurieren; H2 aus dem Runtime-Pfad entfernen.
- `ddl-auto=validate` in allen Umgebungen.
- Flyway-Migrationen für das vollständige Schema anlegen.
- `docker-compose.yml`, Backend-/Frontend-Dockerfiles, Healthchecks und `.env.example` ergänzen.
- Testcontainers für PostgreSQL-Integrationstests verwenden; keine Abhängigkeit von lokal installiertem PostgreSQL.

### P1 – Unvollständiges Zustands- und Fehlermodell

Probleme:

- Status besitzt nur `PENDING`, `ANALYZING`, `COMPLETED`, `FAILED`; `ANALYZING` wird im vorhandenen Workflow nicht gesetzt.
- Keine zentrale Übergangsvalidierung, kein Fortschritt, kein Fehlercode, keine Warnungen, kein `@Version`.
- `COMPLETED` ist auch mit erfundenen/fehlenden Ergebnissen möglich.
- Exceptions werden nicht als RFC-9457-Problem Details ausgegeben.

Verbindliche Änderung:

- Die Statusfolge aus `MVP.md` vollständig übernehmen.
- `AnalysisStatusService` als einzige Schreibstelle für Status implementieren.
- `progress`, `version`, `startedAt`, `completedAt`, `failedAt`, `errorCode`, `errorMessage` und Warnungen migrieren.
- Terminalität von `FAILED`/`COMPLETED`, erlaubte Übergänge und `COMPLETED` nur mit Ergebnis testen.
- Globales Exception Handling mit `application/problem+json`, Trace-ID und stabilem Code einführen.

### P1 – API-Vertrag und Frontend-Workflow passen nicht zum MVP

Probleme:

- Es fehlt `GET /api/analyses/{id}/result`.
- `GET /api/analyses/{id}` liefert sofort das Gesamtergebnis statt Status/Progress.
- Das Frontend pollt nicht und besitzt keine Ergebnisseite `/results/[id]`.
- Backend nutzt `NEXT_PUBLIC_API_BASE_URL`, Spezifikation nennt `NEXT_PUBLIC_API_URL`.
- API-Typen sind auf mehrere Dateien verteilt und teilweise redundant/ad hoc lokal neu definiert.
- CORS ist über `@CrossOrigin(origins = "*")` vollständig offen.

Verbindliche Änderung:

- Status- und Ergebnis-Endpunkt trennen und spezifizierte HTTP-Statuscodes umsetzen.
- Zentrale Frontend-API-Typen aus einem Vertrag ableiten oder in genau einem Modul pflegen.
- Polling mit 1/2/5-Sekunden-Backoff, Abbruch per `AbortController` und Navigation zur Ergebnisseite implementieren.
- Problem-Details strukturiert parsen; keine rohen Response-Bodies als UI-Fehler anzeigen.
- Einen konsistenten Environment-Key festlegen: `NEXT_PUBLIC_API_URL`.
- CORS konfigurationsbasiert auf bekannte Frontend-Origins beschränken.

### P1 – „Lohnt sich“-Modul zeigt erfundene Resultate

Betroffene Datei: `frontend/src/lib/api/analysis.ts`.

Probleme:

- Entfernung, Fahrzeit und Transportart sind fest codiert.
- Transportkosten werden aus der Preisdifferenz statt Route, Verbrauch und Mietkonditionen erzeugt.
- Nutzerfelder zu Heimatort, Verkäuferort, Fahrzeug und Zeitwert werden vom Backend nicht verarbeitet.
- Deal-Score misst derzeit primär Nähe zum Marktpreis; ein sehr günstiger Preis kann dadurch paradoxerweise schlechter bewertet werden.

Verbindliche Änderung:

- Entscheidungsberechnung ins Backend verschieben und als testbare Domain Services implementieren.
- Routing-, Kraftstoffpreis- und RentalProvider als austauschbare Provider modellieren.
- Ohne Provider oder Eingabedaten `unknown`/`null` zurückgeben, keine Standarddistanz erfinden.
- Eigenes Fahrzeug anhand Innenmaßen, Nutzlast und Zerlegbarkeit prüfen; Volumen allein reicht nicht.
- Mietangebote nach Gesamtkosten inklusive Basispreis, Mehrkilometern, Pflichtgebühren, Schutzpaket und Kraftstoff sortieren.
- Historischen Originalpreis, aktuellen Gebrauchtmarkt, Kaufpreis, Transport, Zeitkosten und effektiven Kaufpreis getrennt darstellen.
- Deal-Score transparent erklären und zugrunde liegende Zahlen immer anzeigen.
- Das Modul erst nach einer stabilen, evidenzbasierten Produkt- und Preis-Pipeline aktivieren. Bis dahin UI als „noch nicht verfügbar“ kennzeichnen.

### P1 – Tests decken nur Context-Start ab

Vorhanden ist lediglich `FindlyApplicationTests`. Es fehlen praktisch alle Fach-, API-, Sicherheits- und Integrationstests.

Verbindliche Änderung:

- Unit Tests für Statusübergänge, Matching, Identifier, Preisberechnung, Deal-Berechnung und Währungsregeln.
- SSRF-Tests einschließlich IPv4, IPv6, Redirects, DNS-Auflösung, Userinfo, ungewöhnlicher Schreibweisen und Response-Limit.
- MockWebServer/WireMock für externe HTTP-Antworten; Tests greifen niemals auf echte Webseiten zu.
- Testcontainers-Tests für Flyway/JPA/PostgreSQL.
- Controller-Tests für HTTP 202, 400, 404, 409, 422, 429, 502 und 504 samt Problem-Details.
- Async-Test: HTTP-Response kommt vor Provider-Abschluss; Status bleibt nachweisbar.
- Test, dass Provider-Aufrufe ohne aktive Spring-Transaktion stattfinden.
- Frontend-Tests für Polling, Terminalzustände, Abbruch, Problem-Details und Nullwerte.

### P2 – Codequalität und Wartbarkeit

- `DealScoreService` wird mit `new` statt per Constructor Injection erzeugt.
- Paketstruktur ist technisch/flach statt feature-basiert mit `model`, `dto`, `service`, `repository`, `provider`.
- Entities werden direkt als Aggregat für Prozesslogik verwendet; klare Domain-/Provider-DTO-Grenzen fehlen.
- Regex-basiertes HTML-Parsing ist fragil; strukturierte JSON-LD-/Meta-Daten sollten priorisiert und ein HTML-Parser eingesetzt werden.
- `Listing.imageUrls` ist eager geladen; eigenes `ListingImage`-Modell mit Reihenfolge ist vorgesehen.
- Einzigartige DB-Constraint für höchstens ein ausgewähltes Match pro Analyse fehlt.
- Root-README, CI-Workflow und Betriebsdokumentation fehlen.
- Frontend-Lint meldet `@next/next/no-img-element` in `analysis-form.tsx`.

## Zielarchitektur

Behalte einen modularen Monolithen. Empfohlene Backend-Module:

```text
dev.thomcgn.findly
├── common
│   ├── config
│   ├── error
│   ├── security
│   └── web
├── analysis
│   ├── controller
│   ├── dto
│   ├── model
│   ├── repository
│   └── service
├── listing
│   ├── model
│   ├── repository
│   └── service
├── extraction
│   ├── kleinanzeigen
│   ├── model
│   └── service
├── vision
│   ├── model
│   └── provider
├── search
│   ├── model
│   └── provider
├── matching
│   ├── algorithm
│   ├── model
│   └── service
├── pricing
│   ├── model
│   ├── provider
│   └── service
└── decision
    ├── model
    ├── provider
    └── service
```

Provider-Auswahl muss ausschließlich über typisierte `@ConfigurationProperties` und bedingte Beans erfolgen. Deaktivierte Provider liefern keine Fake-Daten.

## Commit-Plan

### Commit 1 – Build- und Qualitätsbasis

- Root-README mit lokalen Voraussetzungen und Befehlen ergänzen.
- Maven Enforcer für Java-/Maven-Version und Dependency Convergence konfigurieren.
- Compiler-Warnings aktivieren und als Fehler behandeln, soweit für Java 25/Spring kompatibel.
- Checkstyle oder Spotless und SpotBugs einrichten; bestehende Befunde beheben.
- Frontend-Scripts `typecheck` und `lint:ci` ergänzen; Lint-Warnings führen in CI zum Fehler.
- `<img>` nach Prüfung der Next-16-Dokumentation durch `next/image` oder bewusst konfigurierten Image-Loader ersetzen.
- GitHub Actions für Backend und Frontend ergänzen.

Abnahme:

```bash
./mvnw -B clean verify
npm ci
npm run lint -- --max-warnings=0
npm run typecheck
npm run build
git diff --check
```

### Commit 2 – Infrastruktur und Schema

- PostgreSQL, Flyway, Dockerfiles, Compose, Healthchecks und `.env.example` implementieren.
- H2-Runtime und `ddl-auto=update` entfernen.
- Erstes vollständiges Flyway-Schema anlegen; JPA bleibt auf `validate`.
- Testcontainers-Basis schaffen.

Abnahme: Quality Gates aus Commit 1 plus erfolgreicher Compose-Start, Backend-Healthcheck, Frontend-Healthcheck und Flyway-Validierung.

### Commit 3 – API-Fehlervertrag und URL-Validierung

- Fehlercodes, RFC-9457-Handler und Trace-ID implementieren.
- Request-Validierung einschließlich maximaler URL-Länge.
- CORS konfigurierbar einschränken.
- Rate Limiting für die drei Analyse-Endpunkte hinzufügen.

Abnahme: MockMvc-Tests für alle relevanten Statuscodes und vollständiges Problemformat.

### Commit 4 – SSRF-sicherer Listing Client

- Bestehenden URL-Abruf ersetzen; sichere Allowlist, IP-Prüfung, Redirect-Validierung, Timeouts, Größenlimit und Parser einführen.
- Fehler nicht mehr verschlucken.
- Inseratspreis bleibt `null`, wenn er nicht belegt extrahiert werden kann.

Abnahme: isolierte Client-/SSRF-Tests ohne Internetzugriff; keine Regression bei gültigen Fixture-Seiten.

### Commit 5 – Asynchrone Analyse und State Machine

- Vollständiges Analysis-Modell und Statusübergänge migrieren.
- Start-, Status- und Ergebnis-API trennen.
- Begrenzten Executor, transaktionsfreien Orchestrator, kurze Persistenzservices und Timeout implementieren.
- Doppelverarbeitung verhindern.

Abnahme: Async-, Status-, Transaktions- und Concurrency-Tests.

### Commit 6 – Provider-Abstraktionen ohne Halluzinationen

- OCR-, Vision-, Search- und PricingProvider samt Domain-DTOs und Konfiguration einführen.
- Disabled-/Local-Stub-Provider liefern explizit „nicht verfügbar“, niemals fachliche Treffer.
- Alle Hardcodings aus Produkt- und Preisservice entfernen.

Abnahme: Anwendung startet ohne Keys; Analyse endet nachvollziehbar mit Teilergebnis/Fehler und ohne erfundene Werte.

### Commit 7 – Kandidaten, Matching und Evidenz

- Product, ExtractedAttribute, ProductMatch und PriceEvidence implementieren und migrieren.
- Konfigurierbaren Matching-Algorithmus samt Identifier- und Auswahlregeln implementieren.
- Quellenpriorisierung und Preisvalidierung implementieren.

Abnahme: deterministische Unit Tests für Normalisierung, Ranking, Schwellen, Zweitplatz-Abstand, EAN/SKU und Preisakzeptanz.

### Commit 8 – Frontend auf echten Async-Vertrag umstellen

- Zentrale API-Typen, Problem-Details-Client, Polling-Hook und `/results/[id]` ergänzen.
- Alle clientseitigen Fake-Berechnungen und Mock-Defaults aus dem produktiven Pfad entfernen.
- Null-/Unsicherheitszustände, Warnungen, Kandidaten und Quellen korrekt darstellen.
- `NEXT_PUBLIC_API_URL` konsistent verwenden.

Abnahme: Lint, TypeScript und Build warning-frei; Komponenten-/Hook-Tests erfolgreich.

### Commit 9 – „Lohnt sich“-Modul als eigene Ausbaustufe

- Erst jetzt persönliche Mobilität, Routing, Fahrzeuggeometrie, Kraftstoff, Mietanbieter, Zeitkosten und Deal-Entscheidung implementieren.
- Provider-Ausfälle und fehlende Werte sichtbar machen.
- Aktuelle Mietpreise nur als belegte Provider-Ergebnisse mit Abrufzeitpunkt verwenden.

Abnahme: Rechenbeispiele als Tests, insbesondere Münster–Hamburg; keine Netzabhängigkeit in Tests; alle Kostenbestandteile nachvollziehbar.

### Commit 10 – End-to-End-Härtung und Dokumentation

- Gesamten Workflow mit kontrollierten Fixtures testen.
- Observability, strukturierte Logs ohne Secrets, Betriebsanleitung und Architekturentscheidung dokumentieren.
- Dependency-/Security-Scan in CI aufnehmen und Findings beheben.

## Verbindliche Coding-Regeln

- Constructor Injection; kein manuelles `new` für Spring Services.
- Keine Feldinjektion.
- Keine JPA-Entities an Controller oder Provider-Grenzen ausgeben.
- Geld ausschließlich als `BigDecimal` plus ISO-4217-Währung; keine `double`-Fachberechnung.
- Unbekannt ist `null`/Optional beziehungsweise ein expliziter Status, niemals `0`, leerer Text oder erfundener Default.
- Zeitwerte als `Instant` in UTC.
- Konfiguration über validierte `@ConfigurationProperties`; keine verstreuten `@Value`-Strings.
- Externe HTTP-Clients als injizierbare, testbare Adapter.
- Keine Catch-all-Exception, die Fehler in Erfolg oder Platzhalterdaten verwandelt.
- Keine Secrets, API-Keys, Zugriffstokens oder personenbezogenen Adressen loggen.
- Keine echten externen Dienste in Unit-/Integrationstests.
- Keine Änderung einer bereits angewendeten Flyway-Migration; neue Migration anlegen.
- Keine `eslint-disable`, `@SuppressWarnings`, ausgeschalteten Tests oder abgesenkten Quality Gates ohne dokumentierte technische Begründung und ausdrückliche Freigabe.

## Definition of Done des Refactorings

Das Refactoring ist erst abgeschlossen, wenn:

- `POST /api/analyses` schnell mit HTTP 202 und Analyse-ID antwortet.
- Status und Fortschritt per Polling abrufbar und nach Neustart nachvollziehbar sind.
- Netzwerkzugriffe außerhalb von DB-Transaktionen erfolgen.
- URL- und Redirect-Abrufe SSRF-sicher und begrenzt sind.
- Produkt, Modell, Confidence und jeder Preis auf gespeicherte Evidenz zurückgeführt werden können.
- Ohne Provider/API-Key keine Erfolge, Produkte, Preise oder Quellen erfunden werden.
- Ein verifizierter Originalpreis klar vom Gebrauchtmarktwert getrennt ist.
- Die UI Unsicherheit unter 75 % deutlich kennzeichnet.
- Transport- und Deal-Berechnungen nur mit echten Eingaben oder belegten Providerdaten erfolgen.
- PostgreSQL/Flyway die einzige Produktions-Schemaquelle sind.
- Docker Compose alle drei Services gesund startet.
- Backend-, Frontend-, Lint-, Typecheck-, Build-, Test- und Security-Gates ohne Fehler und ohne Warnungen laufen.

## Aktuell gemessene Quality-Gate-Ergebnisse

- Frontend `npm ci`: erfolgreich, jedoch npm-/Dependency-Warnungen vorhanden.
- Frontend ESLint: **nicht warning-frei**; `@next/next/no-img-element` in `src/components/analysis-form.tsx`.
- Frontend Production Build: erfolgreich.
- Backend `./mvnw clean verify`: in der Prüfumgebung nicht ausführbar, weil Maven Central per DNS nicht erreichbar war. Das ist kein Beweis für einen erfolgreichen oder fehlerhaften Backend-Build. CI muss den Build in einer Umgebung mit Dependency-Zugriff erneut ausführen.
- Docker Compose: nicht prüfbar, da keine Compose-Datei vorhanden ist.
- Backend-Fachtests: praktisch nicht vorhanden.

Copilot darf den Backend-Build daher nicht als grün markieren, bevor `./mvnw -B clean verify` tatsächlich erfolgreich gelaufen ist.
