"use client";
import Link from "next/link";
import { useState } from "react";
import type { ReactNode } from "react";
import { useAnalysis } from "@/hooks/use-analysis";
import { AnalysisApiError, errorMessage } from "@/lib/api/analysis";
import type {
  AnalysisDetail,
  AnalysisStatus,
  PriceQuote,
  ProductEvidence,
} from "@/types/analysis";

const stages: Record<AnalysisStatus, string> = {
  CREATED: "Analyse wartet",
  FETCHING_LISTING: "Inserat wird geladen",
  EXTRACTING_LISTING: "Inserat wird ausgelesen",
  IDENTIFYING_PRODUCT: "Produkt wird geprüft",
  RESEARCHING_PRICES: "Preise werden recherchiert",
  COMPLETED: "Analyse abgeschlossen",
  FAILED: "Analyse fehlgeschlagen",
};
const kinds = {
  ORIGINAL: "Originalpreis",
  HISTORICAL_ORIGINAL: "Historischer Originalpreis",
  CURRENT_USED: "Aktueller Gebrauchtmarkt",
};
const warningLabels: Record<string, string> = {
  OCR_UNAVAILABLE: "Texterkennung nicht verfügbar.",
  VISION_UNAVAILABLE: "Bilderkennung nicht verfügbar.",
  SEARCH_UNAVAILABLE: "Produktsuche nicht verfügbar.",
  PRODUCT_NOT_IDENTIFIED: "Produkt nicht eindeutig identifiziert.",
  PRICE_NOT_VERIFIED: "Kein verifizierter Preis verfügbar.",
  PRICE_EVIDENCE_REJECTED: "Einige Preisbelege erfüllen die Prüfregeln nicht.",
  LEGACY_RESULT_UNVERIFIED:
    "Historisches Ergebnis: Produkt- und Preisangaben sind nicht verifiziert.",
};
export function money(value: number | null, currency: string | null) {
  if (value === null || currency === null) return "Unbekannt";
  try {
    return new Intl.NumberFormat("de-DE", {
      style: "currency",
      currency,
      maximumFractionDigits: 4,
    }).format(value);
  } catch {
    return "Unbekannt";
  }
}
function percent(value: number | null) {
  return value === null
    ? "Unbekannt"
    : `${new Intl.NumberFormat("de-DE", { maximumFractionDigits: 2 }).format(value)} %`;
}
function safeSource(value: string) {
  try {
    const url = new URL(value);
    return url.protocol === "https:" && !url.username && !url.password
      ? url.href
      : undefined;
  } catch {
    return undefined;
  }
}
function Source({ url, children }: { url: string; children: ReactNode }) {
  const href = safeSource(url);
  return href ? (
    <a
      className="text-cyan-800 underline"
      href={href}
      target="_blank"
      rel="noopener noreferrer"
    >
      {children}
    </a>
  ) : (
    <span>{children} (Link nicht verfügbar)</span>
  );
}
function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="space-y-3 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-xl font-bold">{title}</h2>
      {children}
    </section>
  );
}
function Warnings({ warnings }: { warnings: string[] }) {
  return warnings.length ? (
    <aside
      aria-label="Hinweise"
      className="rounded-2xl border border-amber-200 bg-amber-50 p-5"
    >
      <ul className="list-inside list-disc">
        {[...new Set(warnings)].map((w) => (
          <li key={w}>{warningLabels[w] ?? `Hinweis: ${w}`}</li>
        ))}
      </ul>
    </aside>
  ) : null;
}
function Attributes({ items }: { items: ProductEvidence[] }) {
  return (
    <ul className="space-y-2">
      {items.map((e, i) => (
        <li key={i}>
          {e.attribute}: {e.value} · <Source url={e.source}>Quelle</Source> ·{" "}
          <time dateTime={e.retrievedAt}>
            {new Date(e.retrievedAt).toLocaleString("de-DE", {
              timeZone: "UTC",
            })}{" "}
            UTC
          </time>
        </li>
      ))}
    </ul>
  );
}
function Quotes({ quotes }: { quotes: PriceQuote[] }) {
  return (
    <ul className="space-y-3">
      {quotes.map((q, i) => (
        <li key={i} className="rounded-xl bg-slate-50 p-3">
          <strong>{money(q.amount, q.currency)}</strong> · {kinds[q.kind]} ·{" "}
          <Source url={q.source}>{q.sourceName}</Source>
          <p className="text-sm text-slate-600">
            Quellenklasse: {q.sourceType} · Evidenz-Confidence:{" "}
            {percent(q.confidence * 100)} · Abruf:{" "}
            <time dateTime={q.retrievedAt}>
              {new Date(q.retrievedAt).toLocaleString("de-DE", {
                timeZone: "UTC",
              })}{" "}
              UTC
            </time>
          </p>
        </li>
      ))}
    </ul>
  );
}
export function ResultDetails({ result }: { result: AnalysisDetail }) {
  return (
    <div className="space-y-6">
      <Warnings warnings={result.warnings} />
      <Card title={result.listing.title}>
        <p className="text-lg">
          Inseratspreis:{" "}
          <strong>
            {money(result.listing.price, result.listing.currency)}
          </strong>
        </p>
        <Source url={result.listing.url}>Inserat öffnen</Source>
        {result.listing.imageUrls.length > 0 && (
          <details>
            <summary className="cursor-pointer">Inseratsbilder</summary>
            <ul>
              {result.listing.imageUrls.map((url, i) => (
                <li key={i}>
                  <Source url={url}>Bild {i + 1} öffnen</Source>
                </li>
              ))}
            </ul>
          </details>
        )}
      </Card>
      <Card title="Produktidentifikation">
        {result.product ? (
          <>
            <p>
              {[
                result.product.brand,
                result.product.model,
                result.product.category,
              ]
                .filter(Boolean)
                .join(" · ") || "Produkt über Identifier zugeordnet"}
            </p>
            <p>
              Evidenzübereinstimmung: {percent(result.product.confidence * 100)}
            </p>
            {result.product.confidence < 0.75 && (
              <p className="font-semibold text-amber-900">
                Unsichere Zuordnung – unter 75 %.
              </p>
            )}
          </>
        ) : (
          <p>
            Produkt nicht eindeutig identifiziert. Eine Kandidatenliste ist
            keine bestätigte Zuordnung.
          </p>
        )}
        <p className="text-sm text-slate-600">
          Der Wert beschreibt die Übereinstimmung der Belege, keine statistische
          Wahrscheinlichkeit.
        </p>
        {result.candidates.length === 0 ? (
          <p>Keine belegten Kandidaten verfügbar.</p>
        ) : (
          <ol className="space-y-4">
            {result.candidates.map((c, i) => (
              <li key={i} className="rounded-xl border border-slate-200 p-4">
                <h3 className="font-semibold">
                  {i + 1}. {c.product.name} · {percent(c.confidence * 100)}
                </h3>
                {c.confidence < 0.75 && (
                  <p className="text-amber-900">
                    Unsicher: unter 75 % Evidenzübereinstimmung.
                  </p>
                )}
                {c.identifierConflict && (
                  <p className="text-rose-800">
                    Widersprüchliche Identifier verhindern eine Auswahl.
                  </p>
                )}
                {!c.eligible && (
                  <p>Die Belege reichen nicht für eine Auswahl aus.</p>
                )}
                <p>
                  {Object.entries(c.product.identifiers)
                    .map(([key, value]) => `${key}: ${value}`)
                    .join(" · ")}
                </p>
                <details>
                  <summary className="cursor-pointer">
                    Einzelwerte und Belege
                  </summary>
                  <ul>
                    {Object.entries(c.components).map(([key, value]) => (
                      <li key={key}>
                        {key}: {percent(value * 100)} · Gewicht:{" "}
                        {c.weights[key] ?? "Unbekannt"}
                      </li>
                    ))}
                  </ul>
                  <Attributes items={c.product.evidence} />
                </details>
              </li>
            ))}
          </ol>
        )}
        {result.attributes.length > 0 && (
          <details>
            <summary className="cursor-pointer">Extrahierte Attribute</summary>
            <Attributes items={result.attributes} />
          </details>
        )}
      </Card>
      <Card title="Preisvergleich">
        <p className="text-sm text-slate-600">
          Die Vergleiche verwenden ausschließlich belegte Referenzen in der
          Inseratswährung. Transport und Zeitkosten sind nicht enthalten.
        </p>
        {Object.entries(kinds).map(([kind, label]) => {
          const comparison = result.comparisons.find((c) => c.kind === kind);
          return (
            <section key={kind} className="rounded-xl bg-slate-50 p-4">
              <h3 className="font-semibold">{label}</h3>
              {comparison ? (
                <>
                  <p>
                    Referenzpreis:{" "}
                    {money(comparison.referencePrice, comparison.currency)}
                  </p>
                  <p>
                    {comparison.outcome === "SURCHARGE"
                      ? "Aufpreis (negative Ersparnis)"
                      : comparison.outcome === "EQUAL"
                        ? "Preisgleichheit"
                        : "Ersparnis"}
                    : {money(comparison.savings, comparison.currency)} ·{" "}
                    {percent(comparison.savingsPercent)}
                  </p>
                  <details>
                    <summary className="cursor-pointer">
                      Verwendete Quellen
                    </summary>
                    <Quotes quotes={comparison.sources} />
                  </details>
                </>
              ) : (
                <p>
                  Unbekannt – kein passender verifizierter Vergleich verfügbar.
                </p>
              )}
            </section>
          );
        })}
      </Card>
      <Card title="Preisbelege">
        {result.priceEvidence.length ? (
          <Quotes quotes={result.priceEvidence} />
        ) : (
          <p>Keine verifizierten Preisquellen verfügbar.</p>
        )}
      </Card>
      <Card title="Transport und Kaufentscheidung">
        <p>
          Noch nicht verfügbar. Entfernung, Fahrtkosten und persönliche
          Kaufempfehlung werden derzeit nicht berechnet.
        </p>
      </Card>
    </div>
  );
}
export function AnalysisResultPage({ id }: { id: string }) {
  const [refresh, setRefresh] = useState(0);
  const { status, result, error } = useAnalysis(id, refresh);
  const failed = status?.status === "FAILED";
  return (
    <main className="mx-auto max-w-5xl space-y-6 px-4 py-8">
      <Link href="/" className="text-cyan-800 underline">
        Neue Analyse
      </Link>
      <h1 className="text-3xl font-bold">Angebotsanalyse</h1>
      <div role="status" aria-live="polite">
        <p>
          {status
            ? stages[status.status]
            : error
              ? "Analyse konnte nicht geladen werden"
              : "Analyse wird geladen …"}
        </p>
        {status && (
          <>
            <progress
              className="w-full"
              aria-label="Analysefortschritt"
              max={100}
              value={status.progress}
            />
            <p>{status.progress} %</p>
          </>
        )}
      </div>
      {failed && (
        <div role="alert">
          <p>
            {errorMessage(
              new AnalysisApiError(status.errorCode ?? "ANALYSIS_FAILED"),
            )}
          </p>
          <Warnings warnings={status.warnings} />
        </div>
      )}
      {error !== undefined && (
        <div role="alert" className="space-y-3">
          <p>{errorMessage(error)}</p>
          {error instanceof AnalysisApiError && error.traceId && (
            <p>Referenz: {error.traceId}</p>
          )}
          <button
            onClick={() => setRefresh((n) => n + 1)}
            className="rounded-xl bg-slate-900 px-4 py-2 text-white"
          >
            Erneut laden
          </button>
        </div>
      )}
      {status && !result && !failed && <Warnings warnings={status.warnings} />}
      {result && <ResultDetails result={result} />}
    </main>
  );
}
