# Findly – MVP

## 1. Ziel

Findly ist eine Webanwendung zur Analyse von Kleinanzeigen-Angeboten.

Der Nutzer gibt eine URL zu einem Angebot ein. Findly versucht anschließend:

1. das angebotene Produkt möglichst genau zu identifizieren,
2. Hersteller und Modell zu bestimmen,
3. Vergleichspreise aus externen Quellen zu finden,
4. den Angebotspreis mit realistischen Marktpreisen zu vergleichen,
5. eine nachvollziehbare Einschätzung abzugeben, ob das Angebot preislich attraktiv ist.

Findly ist **kein Kleinanzeigen-Clone** und stellt selbst keine Anzeigen ein.

Der Fokus des MVP liegt auf:

- Produktidentifikation
- Preisvergleich
- Quellen
- Transparenz
- Confidence Score

---

# 2. Beispiel

Ein Nutzer fügt eine Kleinanzeigen-URL ein.

Beispiel:

```text
https://www.kleinanzeigen.de/s-anzeige/...
```

Das Angebot enthält:

```text
Titel:
Montana Eyewear Brille

Preis:
290 €

Beschreibung:
Hochwertige Brille, kaum getragen.
```

Findly analysiert das Angebot und erkennt beispielsweise:

```text
Hersteller:
Montana Eyewear

Modell:
MCR-1

Produktart:
Lesebrille / Brillenfassung
```

Externe Quellen ergeben:

```text
Originalpreis:
ca. 40–60 €

Aktuelle Vergleichspreise:
25–50 €

Angebotspreis:
290 €
```

Ergebnis:

```text
Deal Score:
Sehr schlecht

Preisabweichung:
+480 %

Confidence:
92 %
```

Die verwendeten Quellen werden angezeigt.

---

# 3. MVP User Flow

## Schritt 1

Der Nutzer öffnet die Startseite.

Er sieht ein Eingabefeld:

```text
Kleinanzeigen-Link einfügen
```

und einen Button:

```text
Angebot analysieren
```

---

## Schritt 2

Das Backend erhält die URL.

```http
POST /api/analyses
```

Request:

```json
{
  "url": "https://www.kleinanzeigen.de/s-anzeige/..."
}
```

---

## Schritt 3

Findly extrahiert soweit technisch und rechtlich möglich:

- Titel
- Beschreibung
- Preis
- Bilder
- Kategorie

Wenn Inhalte nicht automatisiert geladen werden können, muss die Architektur später auch eine manuelle Eingabe unterstützen können.

---

# 4. Produktidentifikation

Aus den vorhandenen Informationen versucht Findly folgende Daten zu ermitteln:

```text
Hersteller
Produktname
Modell
Modellnummer
Produktkategorie
Produktmerkmale
```

Zur Identifikation können später verschiedene Verfahren kombiniert werden:

```text
Listing Text
   ↓
Keyword Extraction
   ↓
Product Search
   ↓
Image Analysis
   ↓
Candidate Ranking
   ↓
Best Match
```

Das System darf bei unsicheren Ergebnissen keine falsche Sicherheit vermitteln.

Beispiel:

```text
Identifikation:
Montana Eyewear MCR-1

Confidence:
87 %
```

Alternativ:

```text
Möglicherweise:
Montana Eyewear MCR-1

Confidence:
54 %
```

---

# 5. Preisrecherche

Nach erfolgreicher Produktidentifikation sucht Findly nach Vergleichspreisen.

Mögliche Quellen:

- Hersteller
- Händler
- Online-Shops
- Preisvergleichsportale
- andere öffentlich zugängliche Produktseiten

Jeder Preisfund wird gespeichert.

Beispiel:

```json
{
  "merchant": "Example Shop",
  "price": 39.99,
  "currency": "EUR",
  "url": "https://example.com/product",
  "type": "NEW"
}
```

---

# 6. Deal Score

Das MVP benötigt zunächst keine komplexe KI-Bewertung.

Ein einfacher regelbasierter Score reicht.

Beispiel:

```text
Angebotspreis:
100 €

Median Vergleichspreis:
50 €
```

Berechnung:

```text
priceDifference =
(listingPrice - marketPrice)
/
marketPrice
```

Mögliche Klassifikation:

```text
<= -30 %     VERY_GOOD
-30 bis -10  GOOD
-10 bis +10  FAIR
+10 bis +30  EXPENSIVE
> +30        VERY_EXPENSIVE
```

Später kann die Bewertung erweitert werden um:

- gebraucht vs. neu
- Zustand
- Alter
- Zubehör
- Versand
- Seltenheit
- regionale Preise

---

# 7. Confidence Score

Produktidentifikation und Preisbewertung müssen getrennte Confidence-Werte besitzen.

Beispiel:

```json
{
  "productConfidence": 0.91,
  "priceConfidence": 0.78
}
```

Der Nutzer soll erkennen können, wie sicher Findly bei einer Analyse ist.

---

# 8. Ergebnisansicht

Nach der Analyse zeigt das Frontend:

## Angebot

```text
Titel
Preis
Bild
URL
```

## Identifiziertes Produkt

```text
Hersteller
Modell
Kategorie
Confidence
```

## Preisvergleich

```text
Angebotspreis
Median Marktpreis
niedrigster Preis
höchster Preis
Preisabweichung
```

## Bewertung

Beispiel:

```text
⚠ Deutlich über Marktpreis
```

## Quellen

Jede Quelle zeigt:

```text
Shop / Quelle
Preis
Produktname
Link
```

---

# 9. Backend Stack

```text
Java 25
Spring Boot 4
Maven

Spring Web
Spring Data JPA
Spring Security
Validation
Flyway
PostgreSQL
Lombok
```

---

# 10. Frontend Stack

```text
Next.js
React
TypeScript
Tailwind CSS
shadcn/ui
```

---

# 11. Datenbank

PostgreSQL.

Initiale Tabellen:

```text
analysis
listing
product_candidate
identified_product
price_source
```

---

# 12. Domain Model

## Listing

```java
Listing
```

Felder:

```text
id
externalUrl
title
description
listingPrice
currency
imageUrls
createdAt
```

---

## Analysis

```java
Analysis
```

Felder:

```text
id
listingId
status
createdAt
completedAt
```

Status:

```text
PENDING
ANALYZING
COMPLETED
FAILED
```

---

## IdentifiedProduct

```java
IdentifiedProduct
```

Felder:

```text
id
analysisId
brand
name
model
category
confidence
```

---

## PriceSource

```java
PriceSource
```

Felder:

```text
id
analysisId
sourceName
productTitle
price
currency
url
condition
createdAt
```

Condition:

```text
NEW
USED
UNKNOWN
```

---

# 13. API

## Analyse starten

```http
POST /api/analyses
```

Request:

```json
{
  "url": "https://www.kleinanzeigen.de/s-anzeige/..."
}
```

Response:

```json
{
  "analysisId": "uuid",
  "status": "PENDING"
}
```

---

## Analyse abrufen

```http
GET /api/analyses/{id}
```

Response:

```json
{
  "id": "uuid",
  "status": "COMPLETED",

  "listing": {
    "title": "Montana Eyewear Brille",
    "price": 290
  },

  "product": {
    "brand": "Montana Eyewear",
    "model": "MCR-1",
    "confidence": 0.92
  },

  "market": {
    "medianPrice": 45,
    "lowestPrice": 35,
    "highestPrice": 59
  },

  "deal": {
    "score": "VERY_EXPENSIVE",
    "differencePercent": 544
  }
}
```

---

# 14. Package Structure

Backend:

```text
dev.thomcgn.findly

├── analysis
│   ├── Analysis.java
│   ├── AnalysisController.java
│   ├── AnalysisService.java
│   └── AnalysisRepository.java
│
├── listing
│   ├── Listing.java
│   └── ListingService.java
│
├── product
│   ├── IdentifiedProduct.java
│   ├── ProductIdentificationService.java
│   └── ProductCandidate.java
│
├── price
│   ├── PriceSource.java
│   ├── PriceResearchService.java
│   └── DealScoreService.java
│
├── integration
│
├── config
│
└── common
```

Feature-basierte Packages sollen bevorzugt werden.

Keine globale Struktur nach:

```text
controller
service
repository
entity
```

---

# 15. Service Responsibilities

## AnalysisService

Orchestriert den gesamten Analyseprozess.

```text
Listing laden
↓
Produkt identifizieren
↓
Preise recherchieren
↓
Deal Score berechnen
↓
Analyse speichern
```

---

## ListingService

Verantwortlich für:

```text
URL prüfen
Listing laden
Listing-Daten normalisieren
```

---

## ProductIdentificationService

Verantwortlich für:

```text
Produktkandidaten erzeugen
Kandidaten bewerten
bestes Produkt bestimmen
Confidence berechnen
```

---

## PriceResearchService

Verantwortlich für:

```text
Preise suchen
Ergebnisse normalisieren
Duplikate entfernen
Preisquellen speichern
```

---

## DealScoreService

Verantwortlich für:

```text
Marktpreis berechnen
Preisabweichung bestimmen
Deal Score bestimmen
```

---

# 16. Architekturregeln

Copilot soll folgende Regeln beachten.

## Controller

Controller enthalten keine Business Logic.

Controller:

```text
Request annehmen
validieren
Service aufrufen
Response zurückgeben
```

---

## Services

Business Logic gehört in Services.

---

## Repositories

Repositories enthalten ausschließlich Datenbankzugriff.

---

## DTOs

Entities werden nicht direkt über REST ausgegeben.

Verwende:

```text
Request DTO
Response DTO
```

---

## IDs

IDs verwenden UUID.

---

## Geldbeträge

Keine Verwendung von:

```java
double
float
```

Für Geld:

```java
BigDecimal
```

---

## Zeit

Zeitstempel:

```java
Instant
```

---

# 17. Fehlerbehandlung

Globale Fehlerbehandlung über:

```java
@RestControllerAdvice
```

Beispiel:

```json
{
  "code": "LISTING_NOT_FOUND",
  "message": "The listing could not be loaded."
}
```

---

# 18. Security

Für das erste MVP ist kein Benutzerkonto notwendig.

Spring Security soll zunächst nur eine saubere API-Konfiguration bereitstellen.

Später möglich:

```text
Account
gespeicherte Analysen
Watchlists
Favoriten
Preisalarme
```

---

# 19. Nicht Teil des MVP

Folgende Funktionen sollen zunächst **nicht implementiert werden**:

```text
Benutzerregistrierung
Social Login
Bezahlsystem
Mobile App
Browser Extension
Preisalarme
Watchlists
Chat
Marketplace
automatischer Kauf
automatische Nachrichten an Verkäufer
komplexes Recommendation System
```

---

# 20. MVP Prioritäten

## Phase 1

Backend-Grundstruktur.

```text
Spring Boot Projekt
PostgreSQL
Flyway
Entities
Repositories
REST API
```

---

## Phase 2

Listing Analyse.

```text
URL Input
Listing Metadata
Listing Speicherung
```

---

## Phase 3

Produktidentifikation.

```text
Textanalyse
Produktsuche
Candidate Ranking
Confidence
```

---

## Phase 4

Preisrecherche.

```text
Price Sources
Median Price
Deal Score
```

---

## Phase 5

Frontend.

```text
URL Input
Loading State
Analyse Ergebnis
Produktdarstellung
Preisvergleich
Quellen
```

---

# 21. Definition of Done für das MVP

Das MVP gilt als funktionsfähig, wenn ein Nutzer:

1. eine Angebots-URL eingeben kann,
2. eine Analyse starten kann,
3. ein identifiziertes Produkt angezeigt bekommt,
4. die Confidence der Identifikation sieht,
5. mehrere Vergleichspreise angezeigt bekommt,
6. den geschätzten Marktpreis sieht,
7. die Abweichung zum Angebotspreis sieht,
8. einen Deal Score erhält,
9. nachvollziehen kann, aus welchen Quellen die Informationen stammen.

---

# 22. Wichtigste Produktregel

Findly soll niemals so tun, als sei ein Produkt eindeutig identifiziert, wenn die vorhandenen Informationen dies nicht rechtfertigen.

Lieber:

```text
Produkt möglicherweise identifiziert
Confidence: 61 %
```

als eine falsche eindeutige Aussage.

**Nachvollziehbarkeit und Quellen sind wichtiger als eine scheinbar intelligente Antwort.**