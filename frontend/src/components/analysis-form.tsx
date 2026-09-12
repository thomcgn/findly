"use client";

import { useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { analyzeOffer } from "@/lib/api/analysis";
import { defaultOfferInput, defaultVehicle } from "@/lib/mock-data";
import type { DecisionResult, OfferInput } from "@/types/decision";

export function AnalysisForm() {
  const [form, setForm] = useState<OfferInput>(defaultOfferInput);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<DecisionResult | null>(null);

  const handleChange = (field: keyof OfferInput, value: string | number | boolean) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const nextResult = await analyzeOffer(form);
      setResult(nextResult);
    } catch (submissionError) {
      setError(
        submissionError instanceof Error
          ? submissionError.message
          : "Beim Analysieren ist ein Fehler aufgetreten.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      <form onSubmit={handleSubmit} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
        <div className="grid gap-5 md:grid-cols-2">
          <Field label="Kleinanzeigen-Link">
            <input
              type="url"
              value={form.url}
              onChange={(event) => handleChange("url", event.target.value)}
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-cyan-500 focus:bg-white focus:ring-4 focus:ring-cyan-100"
              placeholder="https://www.kleinanzeigen.de/s-anzeige/..."
            />
          </Field>

          <Field label="Produktname">
            <input
              value={form.productName}
              onChange={(event) => handleChange("productName", event.target.value)}
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-cyan-500 focus:bg-white focus:ring-4 focus:ring-cyan-100"
            />
          </Field>

          <Field label="Heimatort">
            <input
              value={form.homeLocation}
              onChange={(event) => handleChange("homeLocation", event.target.value)}
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-cyan-500 focus:bg-white focus:ring-4 focus:ring-cyan-100"
            />
          </Field>

          <Field label="Verkäuferort">
            <input
              value={form.sellerLocation}
              onChange={(event) => handleChange("sellerLocation", event.target.value)}
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-cyan-500 focus:bg-white focus:ring-4 focus:ring-cyan-100"
            />
          </Field>

          <Field label="Originalpreis (€)">
            <input
              type="number"
              value={form.originalPrice}
              onChange={(event) => handleChange("originalPrice", Number(event.target.value))}
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-cyan-500 focus:bg-white focus:ring-4 focus:ring-cyan-100"
            />
          </Field>

          <Field label="Kaufpreis (€)">
            <input
              type="number"
              value={form.askingPrice}
              onChange={(event) => handleChange("askingPrice", Number(event.target.value))}
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-cyan-500 focus:bg-white focus:ring-4 focus:ring-cyan-100"
            />
          </Field>

          <Field label="Eigenes Fahrzeug">
            <select
              value={form.vehicle.name}
              onChange={(event) => {
                if (event.target.value === "VW Golf 7") {
                  setForm((current) => ({ ...current, vehicle: defaultVehicle }));
                }
              }}
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-cyan-500 focus:bg-white focus:ring-4 focus:ring-cyan-100"
            >
              <option>VW Golf 7</option>
            </select>
          </Field>

          <Field label="Zeitwert pro Stunde (€)">
            <input
              type="number"
              value={form.timeValuePerHour ?? 15}
              onChange={(event) => handleChange("timeValuePerHour", Number(event.target.value))}
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-cyan-500 focus:bg-white focus:ring-4 focus:ring-cyan-100"
            />
          </Field>
        </div>

        <div className="mt-5 flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input
              type="checkbox"
              checked={form.includeTimeCosts}
              onChange={(event) => handleChange("includeTimeCosts", event.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-cyan-600 focus:ring-cyan-500"
            />
            Zeitkosten berücksichtigen
          </label>
        </div>

        <div className="mt-6 flex justify-end">
          <button
            type="submit"
            disabled={loading}
            className="inline-flex items-center justify-center rounded-2xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? "Berechnung läuft..." : "Kalkulation starten"}
          </button>
        </div>
      </form>

      {error ? (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      {result ? <DecisionResultCard result={result} /> : null}
    </div>
  );
}

function DecisionResultCard({ result }: { result: DecisionResult }) {
  return (
    <div className="space-y-8">
      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-cyan-700">
              ProductScout
            </p>
            <h2 className="mt-2 text-3xl font-black text-slate-900">{result.headline}</h2>
          </div>
          <div className="rounded-full bg-cyan-50 px-4 py-2 text-sm font-semibold text-cyan-900">
            Deal Score: {result.dealScore}/100
          </div>
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Metric label="Originalpreis" value={`${result.originalPrice.toFixed(0)} €`} />
          <Metric label="Kaufpreis" value={`${result.askingPrice.toFixed(0)} €`} />
          <Metric label="Transport" value={`${result.transportCost.toFixed(0)} €`} />
          <Metric label="Effektiver Kaufpreis" value={`${result.effectivePurchasePrice.toFixed(0)} €`} />
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-[1.4fr_1fr]">
        <InfoCard title="Kaufentscheidung" icon="✅">
          <div className="grid gap-4 sm:grid-cols-2">
            <Metric label="Effektive Ersparnis" value={`${result.effectiveSavings.toFixed(0)} €`} />
            <Metric label="Gegenüber aktuellem Gebrauchtmarkt" value={`${result.savingsAgainstCurrentMarket.toFixed(0)} €`} />
            <Metric label="Entfernung einfach" value={`${result.distanceKm} km`} />
            <Metric label="Gesamtstrecke" value={`${result.roundTripDistanceKm} km`} />
            <Metric label="Fahrzeit" value={`${Math.round(result.drivingTimeMinutes / 60)} h`} />
            <Metric label="Transportmodus" value={result.transportMode} />
          </div>

          <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <p className="text-sm font-medium text-slate-600">Empfehlung</p>
            <p className="mt-2 text-xl font-bold text-slate-900">{result.recommendation}</p>
          </div>
        </InfoCard>

        <InfoCard title="Entscheidungslogik" icon="📊">
          <dl className="space-y-3 text-sm text-slate-700">
            <DefinitionItem label="Preislicher Vorteil" value={`${result.priceAdvantage.toFixed(0)} €`} />
            <DefinitionItem label="Effektiv finanzieller Vorteil" value={`${result.effectiveFinancialAdvantage.toFixed(0)} €`} />
            <DefinitionItem label="Vorteil inkl. Zeitaufwand" value={`${result.advantageIncludingTime.toFixed(0)} €`} />
            <DefinitionItem label="Zeitkosten" value={`${result.timeCost.toFixed(0)} €`} />
          </dl>
        </InfoCard>
      </div>

      <InfoCard title="Transportvergleich" icon="🚚">
        <div className="grid gap-4 md:grid-cols-2">
          {result.transportOptions.map((option) => (
            <div key={`${option.provider}-${option.vehicleName}`} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
                    {option.provider}
                  </p>
                  <h4 className="mt-2 text-lg font-bold text-slate-900">{option.vehicleName}</h4>
                </div>
                <span className="rounded-full bg-slate-900 px-2.5 py-1 text-xs font-semibold text-white">
                  {option.routeType}
                </span>
              </div>
              <p className="mt-3 text-2xl font-black text-slate-900">{option.totalEstimatedCost.toFixed(0)} €</p>
              <p className="mt-2 text-sm text-slate-600">{option.notes}</p>
            </div>
          ))}
        </div>
      </InfoCard>

      <div className="grid gap-6 lg:grid-cols-2">
        <InfoCard title="Warum lohnt es sich?" icon="💡">
          <ul className="space-y-3 text-sm text-slate-700">
            {result.reasons.map((reason) => (
              <li key={reason} className="flex gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3">
                <span className="mt-0.5 text-cyan-700">✓</span>
                <span>{reason}</span>
              </li>
            ))}
          </ul>
        </InfoCard>

        <InfoCard title="Zu beachten" icon="⚠️">
          <ul className="space-y-3 text-sm text-slate-700">
            {result.warnings.map((warning) => (
              <li key={warning} className="flex gap-3 rounded-xl border border-amber-200 bg-amber-50 p-3">
                <span className="mt-0.5 text-amber-700">!</span>
                <span>{warning}</span>
              </li>
            ))}
          </ul>
        </InfoCard>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block space-y-2 text-sm font-medium text-slate-700">
      <span>{label}</span>
      {children}
    </label>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
      <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500">{label}</p>
      <p className="mt-2 text-xl font-black text-slate-900">{value}</p>
    </div>
  );
}

function DefinitionItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 px-3 py-2">
      <dt className="text-slate-500">{label}</dt>
      <dd className="font-semibold text-slate-900">{value}</dd>
    </div>
  );
}

function InfoCard({
  title,
  icon,
  children,
}: {
  title: string;
  icon: string;
  children: ReactNode;
}) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <h3 className="mb-4 flex items-center gap-2 text-lg font-semibold text-slate-900">
        <span>{icon}</span>
        {title}
      </h3>
      {children}
    </section>
  );
}
