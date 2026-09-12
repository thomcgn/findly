# 36. Lohnt-sich-Kalkulation

ProductScout soll nicht nur feststellen, wie viel ein Produkt neu gekostet hat.

Das System soll zusätzlich beantworten:

> **„Lohnt es sich für mich tatsächlich, dieses Angebot zu kaufen und dafür zum Verkäufer zu fahren?“**

Dafür müssen sämtliche relevanten Beschaffungskosten berechnet werden.

---

# 37. Eingaben für die persönliche Kalkulation

Der Nutzer kann ein persönliches Mobilitätsprofil hinterlegen.

## Wohnort

```text
Heimatort:
Münster
```

Alternativ:

- PLZ
- Stadt
- Adresse

Die exakte Adresse soll nur gespeichert werden, wenn der Nutzer dies ausdrücklich erlaubt.

Für die Routenberechnung reicht grundsätzlich:

```text
PLZ / Ort
```

---

# 38. Eigenes Fahrzeug

Der Nutzer kann sein eigenes Fahrzeug hinterlegen.

```text
Fahrzeug:
VW Golf 7

Kraftstoff:
Diesel

Verbrauch:
5,2 l / 100 km

Kraftstoffpreis:
1,89 €/l

Laderaum:
380 Liter

Nutzbare Ladefläche:
optional

Maximale Zuladung:
optional
```

Alternativ kann der Nutzer auswählen:

```text
[ ] Ich fahre mit meinem eigenen Auto
[ ] Ich benötige einen Transporter
[ ] Ich weiß es noch nicht
```

Wenn das eigene Fahrzeug nicht ausreichend groß ist, soll das System automatisch auf Miettransporter wechseln.

---

# 39. Fahrzeugprofil

```typescript
interface VehicleProfile {
  id: string;
  name: string;

  fuelType:
    | "PETROL"
    | "DIESEL"
    | "ELECTRIC"
    | "HYBRID";

  consumptionPer100Km?: number;

  fuelPricePerUnit?: number;

  batteryConsumptionPer100Km?: number;

  electricityPricePerKwh?: number;

  cargoVolumeLiters?: number;

  cargoLengthMm?: number;
  cargoWidthMm?: number;
  cargoHeightMm?: number;

  maxPayloadKg?: number;
}
```

---

# 40. Produktgröße

Die Produktanalyse muss ebenfalls die benötigte Transportgröße bestimmen.

Aus Produktdaten und Bildern sollen möglichst ermittelt werden:

```text
Breite
Höhe
Tiefe
Gewicht
Volumen
zerlegbar: ja/nein
```

Beispiel:

```text
Kommode

Breite: 180 cm
Höhe: 90 cm
Tiefe: 45 cm

Geschätztes Volumen:
0,73 m³

Zerlegbarkeit:
wahrscheinlich ja
```

Die Volumenberechnung muss zwischen:

```text
Produktvolumen
```

und

```text
benötigtem Laderaum
```

unterscheiden.

Ein 180 × 90 × 45 cm großes Möbelstück benötigt nicht zwingend 0,729 m³ nutzbaren Laderaum, wenn es nicht zerlegt werden kann.

Deshalb soll zusätzlich die Geometrie berücksichtigt werden.

---

# 41. Transportfähigkeit

Das System soll prüfen:

```text
Passt das Produkt in mein Fahrzeug?
```

Beispiel:

```text
Produkt:
180 × 90 × 45 cm

Fahrzeug:
VW Golf

Laderaum:
380 l

→ wahrscheinlich nicht transportierbar
```

Dann:

```text
Empfehlung:

Transporter erforderlich
```

Bei Unsicherheit:

```text
Transportfähigkeit nicht eindeutig bestimmbar.

Bitte Maße des zerlegten Möbelstücks prüfen.
```

Das System darf nicht behaupten, dass ein Möbelstück definitiv passt, wenn keine ausreichenden Maße vorliegen.

---

# 42. Transporter-Auswahl

Wenn das eigene Fahrzeug nicht ausreicht, werden Mietfahrzeuge gesucht.

Kategorien:

```text
Kleintransporter
Mittelgroßer Transporter
XL-Transporter
Sprinter / Crafter
LKW
```

Die Auswahl erfolgt nach:

```text
benötigtem Ladevolumen
Länge
Breite
Höhe
Zuladung
Fahrzeugverfügbarkeit
Mietpreis
Kilometerpreis
Kraftstoffverbrauch
```

Es soll **nicht automatisch der größte Transporter** gewählt werden.

Ziel:

> kleinstes geeignetes Fahrzeug mit niedrigsten Gesamtkosten.

---

# 43. Mietwagen-Provider

Die Anwendung benötigt ein einheitliches Provider-Interface.

```java
public interface RentalProvider {

    List<RentalOffer> search(RentalSearchRequest request);
}
```

Beispielimplementierungen:

```text
RentalProvider
├── CarlundCarlaProvider
├── SixtProvider
├── EuropcarProvider
├── AvisProvider
├── HertzProvider
└── AdacRentalProvider
```

Nicht jeder Provider muss zwingend eine eigene API besitzen.

Wenn ein Anbieter keine geeignete öffentliche Schnittstelle bereitstellt, darf das System dessen öffentlich zugängliche Angebote nur über technisch zulässige Methoden berücksichtigen.

Keine CAPTCHA- oder Anti-Bot-Umgehung.

---

# 44. Mietangebot

```typescript
interface RentalOffer {
  provider: string;

  vehicleName: string;
  vehicleCategory: string;

  rentalStart: string;
  rentalEnd: string;

  rentalPrice: number;
  currency: string;

  includedKilometers: number;
  extraKilometerPrice?: number;

  estimatedConsumptionPer100Km?: number;
  fuelType?: string;

  cargoVolumeM3?: number;

  oneWayAvailable: boolean;
  oneWayFee?: number;

  insuranceIncluded: boolean;
  deductible?: number;

  sourceUrl: string;

  totalEstimatedCost: number;
}
```

---

# 45. Aktuelle Mietpreise

Mietpreise dürfen niemals fest im Code hinterlegt werden.

Bei jeder Kalkulation sollen aktuelle Angebote abgefragt werden.

Beispiel CarlundCarla:

```text
Carl
49 €/Tag
100 km inklusive
0,21 €/Zusatzkilometer

Carlos XL
69 €/Tag
100 km inklusive
0,25 €/Zusatzkilometer

Carla
79 €/Tag
100 km inklusive
0,21 €/Zusatzkilometer
```

Diese Preise sind nur Beispiele für die Provider-Datenstruktur und müssen zur tatsächlichen Buchungszeit erneut abgefragt werden.

CarlundCarla bietet außerdem Schutzpakete zur Reduzierung der Selbstbeteiligung sowie weitere Optionen an.

---

# 46. Mietpreisberechnung

```text
Grundpreis
+
Zusatzkilometer
+
Einweggebühr
+
Schutzpakete
+
Zusatzfahrer
+
sonstige Pflichtgebühren
+
Kraftstoff
=
effektive Transportkosten
```

Freikilometer müssen berücksichtigt werden.

```java
extraKm =
    Math.max(0, totalKm - includedKm);

rentalCost =
    basePrice
    + extraKm * extraKmPrice
    + mandatoryFees;
```

---

# 47. Kraftstoffkosten

Der Nutzer kann seinen Kraftstoffpreis selbst festlegen.

Alternativ kann das System einen aktuellen Durchschnittspreis verwenden.

Eingabe:

```text
Verbrauch:
8,5 l / 100 km

Diesel:
1,89 €/l
```

Berechnung:

```java
fuelLiters =
    totalKm / 100 * consumption;

fuelCost =
    fuelLiters * fuelPrice;
```

Beispiel:

```text
564 km
8,5 l / 100 km
1,89 €/l

Verbrauch:
47,94 l

Kraftstoff:
90,61 €
```

Der verwendete Kraftstoffpreis muss im Ergebnis sichtbar sein.

---

# 48. Entfernung

Die Entfernung wird automatisch berechnet:

```text
Heimatort
      ↓
Verkäufer
```

Anzuzeigen:

```text
Entfernung einfach:
282 km

Entfernung Hin und Zurück:
564 km

Fahrzeit:
ca. 5 h 46 min
```

Die konkrete Route soll über einen konfigurierbaren Routing-Provider ermittelt werden.

Gespeichert werden:

```text
oneWayDistanceKm
roundTripDistanceKm
estimatedDrivingTimeMinutes
```

---

# 49. Zeitkosten

Optional kann der Nutzer seine persönliche Zeit bewerten.

```text
Zeitwert:
15 €/Stunde
```

Das System berechnet:

```text
Fahrzeit
+
Abholung
+
Beladen
+
Rückgabe Mietfahrzeug
```

Beispiel:

```text
Gesamtzeit:
7 Stunden

Zeitwert:
15 €/h

Zeitkosten:
105 €
```

Diese Kosten werden **separat** dargestellt.

Der Nutzer kann wählen:

```text
[✓] Zeitkosten berücksichtigen
```

oder

```text
[ ] Zeitkosten nicht berücksichtigen
```

Dadurch werden zwei Ergebnisse möglich:

```text
Finanzieller Gewinn
Gewinn inklusive Zeitaufwand
```

---

# 50. Opportunitätskosten

Optional:

```text
Was hätte ich in dieser Zeit sonst gemacht?
```

Der Nutzer kann einen individuellen Stundenwert einstellen.

Standard:

```text
0 €
```

Dadurch wird niemand gezwungen, seine Freizeit monetär zu bewerten.

---

# 51. Effektive Ersparnis

Die wichtigste Kennzahl ist:

```text
Originalpreis
-
Kaufpreis
-
Transportkosten
=
effektive Ersparnis
```

Beispiel:

```text
Originalpreis:          790 €
Kleinanzeigenpreis:      80 €
Transportkosten:        190 €
──────────────────────────────
Effektive Ersparnis:    520 €
```

---

# 52. Effektiver Kaufpreis

Zusätzlich:

```text
Kaufpreis
+
Transportkosten
=
effektiver Kaufpreis
```

Beispiel:

```text
Kaufpreis:               80 €
Transport:              190 €
────────────────────────────
Effektiver Kaufpreis:   270 €
```

Vergleich:

```text
Neu:
790 €

Effektiv gebraucht:
270 €

Effektiver Rabatt:
65,8 %
```

---

# 53. Lohnt-sich-Score

ProductScout soll zusätzlich eine leicht verständliche Entscheidung liefern.

Mögliche Ergebnisse:

```text
🔥 EXTREM GUTER DEAL

🟢 LOHNT SICH

🟡 KANN SICH LOHNEN

🟠 GRENZWERTIG

🔴 LOHNT SICH NICHT
```

Die Entscheidung basiert auf:

```text
Originalpreis
Kaufpreis
Transportkosten
Marktwert
Zustand
Fahrzeit
Entfernung
Transportaufwand
```

---

# 54. Entscheidungslogik

Nicht nur der Rabatt zählt.

Beispiel:

```text
Originalpreis: 790 €
Kaufpreis: 80 €
Transport: 250 €

Effektiver Kaufpreis:
330 €

Ersparnis:
460 €
```

→ kann sich trotzdem lohnen.

Anderes Beispiel:

```text
Originalpreis: 150 €
Kaufpreis: 40 €
Transport: 140 €

Effektiver Kaufpreis:
180 €
```

→ lohnt sich wahrscheinlich nicht.

---

# 55. Beispiel Münster → Hamburg

Beispielangebot:

```text
Kommode
Kleinanzeigen:
80 €

Originalpreis:
790 €

Standort:
Hamburg

Heimatort:
Münster
```

Routenplanung:

```text
ca. 282 km einfach
ca. 564 km Hin- und Rückfahrt
```

---

# 56. Beispiel mit eigenem Auto

Angenommen:

```text
Verbrauch:
6,0 l / 100 km

Kraftstoff:
1,90 €/l
```

Dann:

```text
564 km / 100 × 6,0
= 33,84 Liter

33,84 × 1,90 €
= 64,30 €
```

Effektive Kosten:

```text
Kaufpreis:               80 €
Fahrt:                   64 €
────────────────────────────
Effektiver Kaufpreis:   144 €
```

Gegenüber 790 €:

```text
Effektive Ersparnis:
646 €
```

---

# 57. Beispiel mit Miettransporter

Für CarlundCarla kann das System beispielsweise einen Carl-Tagespreis von 49 € plus die tatsächlichen Zusatzkilometer berücksichtigen.

Bei 564 km Gesamtstrecke und 100 km inklusive:

```text
564 km
- 100 km
= 464 Zusatzkilometer

464 × 0,21 €
= 97,44 €

49 €
+ 97,44 €
= 146,44 € Mietkosten
```

Danach kommen die tatsächlichen Kraftstoffkosten hinzu.

Bei beispielsweise:

```text
8,0 l / 100 km
1,90 €/l
```

ergibt sich:

```text
564 km
× 8 / 100
= 45,12 l

45,12 × 1,90 €
= 85,73 €

Miete:              146,44 €
Kraftstoff:          85,73 €
─────────────────────────────
Transport:          232,17 €

Kaufpreis:           80,00 €
─────────────────────────────
Effektiver Kaufpreis:
312,17 €
```

Damit:

```text
790 €
- 312,17 €
= 477,83 €

Effektive Ersparnis:
477,83 €
```

Die konkreten Mietpreise müssen bei der tatsächlichen Analyse aktuell abgefragt werden.

---

# 58. Vergleich mehrerer Mietanbieter

Die Ergebnisansicht soll beispielsweise zeigen:

```text
TRANSPORTVERGLEICH

CarlundCarla Carl
49 € + km
geschätzt gesamt: 232 €

ADAC / Partner
61 € / Tag + Konditionen
geschätzt gesamt: 245 €

SIXT
aktuelles Angebot
geschätzt gesamt: 268 €

Europcar
aktuelles Angebot
geschätzt gesamt: 251 €
```

Die Anbieter sollen nach:

```text
Gesamtkosten
```

sortiert werden.

Nicht nach dem reinen Tagespreis.

ADAC bietet in Münster aktuell einen Transporterpreisvergleich mit Partnern wie Sixt, Hertz und Avis an; dort werden die Preise ausdrücklich als tagesabhängig ausgewiesen.

---

# 59. One-Way-Miete

One-Way-Mieten müssen besonders berücksichtigt werden.

Beispiel:

```text
Münster
   ↓
Hamburg
```

Transporter wird in Hamburg zurückgegeben.

Dann:

```text
Hinweg:
282 km

Rückweg:
entfällt
```

Das kann eine enorme Kostenersparnis bedeuten.

Die Kalkulation muss deshalb mindestens diese Varianten vergleichen:

```text
A: eigenes Auto
B: Miettransporter Hin + Zurück
C: Miettransporter One-Way
D: Miettransporter + Bahn/ÖPNV zurück
```

SIXT bietet beispielsweise Transporter-One-Way-Mieten an; eine mögliche Einweggebühr wird im Buchungsprozess ausgewiesen.

---

# 60. Transportergröße

Die Anwendung muss prüfen, ob das Produkt überhaupt in den Transporter passt.

Beispiel:

```text
Kommode:
180 × 90 × 45 cm
```

Dann:

```text
Carl:
wahrscheinlich ausreichend

Carlos XL:
sicherer / mehr Reserve
```

Für XL-Transporter können konkrete Ladevolumen gespeichert werden.

Beispiel CarlundCarla Carlos:

```text
Ladevolumen:
ca. 11,3 m³
```

Der Anbieter beschreibt Carlos als XL-Transporter, beispielsweise VW Crafter, Ford Transit, Sprinter oder ähnlich.

---

# 61. Fahrzeugauswahl

Das System soll dem Nutzer nicht nur sagen:

> „Du brauchst einen Transporter.“

Sondern:

> „Der günstigste passende Transporter ist Anbieter X, Fahrzeug Y.“

Beispiel:

```text
EMPFEHLUNG

Carlos XL
11,3 m³

✓ Kommode passt
✓ ausreichend Ladehöhe
✓ ausreichend Ladevolumen

Gesamtkosten:
232 €

Günstiger als:
SIXT: 268 €
Europcar: 251 €
```

---

# 62. Kaufentscheidung

Die finale Ergebnisansicht erhält einen eigenen Bereich:

```text
┌────────────────────────────────────────────┐
│ 🟢 KAUF LOHNT SICH                         │
│                                            │
│ Produkt neu:             790 €             │
│ Kaufpreis:                80 €             │
│ Transport:               232 €             │
│ ─────────────────────────────              │
│ Effektiver Preis:        312 €             │
│                                            │
│ Effektive Ersparnis:     478 €             │
│ Effektiver Rabatt:       60,5 %             │
│                                            │
│ Entfernung:              282 km             │
│ Fahrt gesamt:            564 km             │
│                                            │
│ Empfehlung:                                │
│ JA – trotz Entfernung wirtschaftlich       │
└────────────────────────────────────────────┘
```

---

# 63. Entscheidungsbegründung

Das System soll die Entscheidung erklären.

Beispiel:

```text
Warum lohnt es sich?

✓ Originalprodukt ist nicht mehr erhältlich
✓ verifizierter historischer Neupreis: 790 €
✓ Kaufpreis nur 80 €
✓ effektiver Kaufpreis inkl. Transport: 312 €
✓ Ersparnis gegenüber Originalpreis: 478 €
✓ Produkt passt in verfügbaren Transporter
```

Zusätzlich:

```text
Zu beachten:

⚠ 564 km Gesamtstrecke
⚠ ca. 6 Stunden reine Fahrzeit
⚠ Zustand des gebrauchten Produkts
⚠ eventuelle Schäden
```

---

# 64. Nicht nur Originalpreis verwenden

Besonders bei nicht mehr erhältlichen Produkten ist der Originalpreis allein kein ausreichender Entscheidungswert.

Beispiel:

```text
Originalpreis:
790 €

Aber:

Produkt nicht mehr erhältlich
↓
historischer Preis
↓
kein aktueller Neupreis
```

Deshalb soll zusätzlich versucht werden:

```text
aktueller Gebrauchtmarktwert
```

zu bestimmen.

Wenn beispielsweise vergleichbare gebrauchte Angebote bei:

```text
250–350 €
```

liegen, soll das Ergebnis entsprechend angepasst werden.

---

# 65. Drei Preiswerte

Die UI soll grundsätzlich unterscheiden:

```text
Originalpreis:
790 €

aktueller Gebrauchtmarktwert:
250–350 €

effektiver eigener Kaufpreis:
312 €
```

Dann:

```text
Gegenüber Original:
478 € gespart

Gegenüber aktuellem Gebrauchtmarkt:
wahrscheinlich kein Schnäppchen
```

Das ist entscheidend.

Ein hoher historischer Originalpreis darf einen schlechten Gebrauchtkauf nicht künstlich als Schnäppchen darstellen.

---

# 66. Finaler Deal-Score

Der Score soll deshalb mehrere Werte kombinieren:

```text
Originalpreis-Vorteil
+
Gebrauchtmarkt-Vorteil
+
Transportkosten
+
Zustand
+
Entfernung
+
Zeitaufwand
+
Transportaufwand
```

Beispiel:

```text
Deal Score: 87 / 100

🟢 Sehr guter Kauf
```

Der Score darf niemals die zugrunde liegenden Zahlen ersetzen.

Immer anzeigen:

```text
Warum?
```

---

# 67. Ergebnisvergleich

Die Anwendung soll am Ende eine Tabelle erzeugen:

```text
| Wert                         | Ergebnis |
|------------------------------|----------|
| Originalpreis                | 790 €    |
| Gebrauchtmarkt               | 250–350 €|
| Kaufpreis                    | 80 €     |
| Transport                    | 232 €    |
| Effektiver Kaufpreis         | 312 €    |
| Ersparnis Originalpreis      | 478 €    |
| Entfernung einfach           | 282 km   |
| Entfernung gesamt            | 564 km   |
| Fahrzeit                     | ~5–6 h   |
| Transporter                  | Carlos   |
| Laderaum                     | 11,3 m³  |
| Deal Score                   | 87/100   |
```

---

# 68. Nutzerprofil speichern

Optional kann der Nutzer dauerhaft hinterlegen:

```text
Heimatort
Eigenes Fahrzeug
Verbrauch
Kraftstoff
Kraftstoffpreis
Zeitwert
```

Dadurch kann jedes neue Kleinanzeigen-Inserat automatisch aus seiner persönlichen Perspektive bewertet werden.

Beispiel:

```text
[Mein Profil]

Münster
VW Golf
6,0 l Diesel
1,90 €/l
Zeitwert: 15 €/h
```

Danach:

```text
„Lohnt sich dieses Inserat für mich?“
```

wird automatisch berechnet.

---

# 69. Wichtige Berechnungsregel

Es müssen **drei verschiedene Ergebnisse** angezeigt werden:

### 1. Preislicher Vorteil

```text
Originalpreis - Kaufpreis
```

### 2. Effektiver finanzieller Vorteil

```text
Originalpreis
- Kaufpreis
- Transportkosten
```

### 3. Vorteil inklusive Zeitaufwand

```text
Originalpreis
- Kaufpreis
- Transportkosten
- Zeitkosten
```

So kann der Nutzer selbst entscheiden, welche Betrachtung für ihn relevant ist.

---

# 70. Ziel

ProductScout soll am Ende nicht nur sagen:

> „Das Produkt hat neu 790 € gekostet.“

Sondern:

> **„Du zahlst effektiv 312 €, musst dafür 564 km fahren und rund 6 Stunden investieren. Gegenüber dem historischen Neupreis sparst du 478 €. Gegenüber dem aktuellen Gebrauchtmarkt ist der Deal jedoch nur durchschnittlich/gut/sehr gut.“**

Damit wird aus einer Produkterkennung ein echter **Kaufentscheidungs-Assistent für Kleinanzeigen**.