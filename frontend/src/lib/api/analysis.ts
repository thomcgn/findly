import type { DecisionResult, OfferInput } from "@/types/decision";

const BACKEND_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL?.trim() || "http://localhost:8080"
).replace(/\/+$/, "");

function toNumber(value: number | string | null | undefined): number {
  if (typeof value === "number") return Number.isFinite(value) ? value : 0;
  if (typeof value === "string") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  return 0;
}

export async function analyzeOffer(input: Partial<OfferInput> = {}): Promise<DecisionResult> {
  const normalizedUrl = (input.url ?? "").trim();
  if (!normalizedUrl) {
    throw new Error("Bitte gib eine Kleinanzeigen-URL ein.");
  }

  const createResponse = await fetch(`${BACKEND_URL}/api/analyses`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ url: normalizedUrl }),
  });

  if (!createResponse.ok) {
    const message = await createResponse.text();
    if (createResponse.status === 400) throw new Error(message || "Ungültige Anfrage: Bitte überprüfe die eingegebenen Daten.");
    if (createResponse.status === 404) throw new Error(message || "Analyse-Endpunkt nicht gefunden.");
    if (createResponse.status === 408) throw new Error(message || "Zeitüberschreitung beim Starten der Analyse.");
    if (createResponse.status === 429) throw new Error(message || "Zu viele Anfragen. Bitte versuche es gleich erneut.");
    if (createResponse.status >= 500) throw new Error(message || "Serverfehler beim Starten der Analyse.");
    throw new Error(message || `Die Analyse konnte nicht gestartet werden (HTTP ${createResponse.status}).`);
  }

  const startData = (await createResponse.json()) as {
    analysisId?: string;
    id?: string;
    status?: string;
  };
  const analysisId = startData.analysisId ?? startData.id;

  if (!analysisId) {
    throw new Error("Die Analyse wurde gestartet, aber die Antwort enthält keine Analyse-ID.");
  }

  const detailResponse = await fetch(`${BACKEND_URL}/api/analyses/${analysisId}`);
  if (!detailResponse.ok) {
    const message = await detailResponse.text();
    if (detailResponse.status === 404) throw new Error(message || `Analyse ${analysisId} wurde nicht gefunden.`);
    if (detailResponse.status === 408) throw new Error(message || "Zeitüberschreitung beim Laden der Analysedetails.");
    if (detailResponse.status === 429) throw new Error(message || "Zu viele Anfragen beim Laden der Analysedetails.");
    if (detailResponse.status >= 500) throw new Error(message || "Serverfehler beim Laden der Analysedetails.");
    throw new Error(message || `Analysedetails konnten nicht geladen werden (HTTP ${detailResponse.status}).`);
  }

  const detail = (await detailResponse.json()) as {
    id: string;
    status?: string;
    listing?: { title?: string; price?: number; currency?: string; url?: string; imageUrls?: string[] };
    product?: { brand?: string; model?: string; category?: string; confidence?: number };
    market?: { medianPrice?: number; lowestPrice?: number; highestPrice?: number };
    deal?: { score?: string; differencePercent?: number };
  };

  if (!detail || typeof detail !== "object") {
    throw new Error("Die Analysedetails haben ein ungültiges Format.");
  }

  if (!detail.id || typeof detail.id !== "string") {
    throw new Error("Die Analysedetails enthalten keine gültige Analyse-ID.");
  }

  const askingPrice = toNumber(detail.listing?.price ?? input.askingPrice ?? 0);
  const originalPrice = toNumber(detail.market?.highestPrice ?? input.originalPrice ?? detail.listing?.price ?? 0);
  const marketMedian = toNumber(detail.market?.medianPrice ?? input.originalPrice ?? detail.listing?.price ?? 0);
  const lowerBound = toNumber(detail.market?.lowestPrice ?? Math.max(0, marketMedian * 0.85));
  const higherBound = toNumber(detail.market?.highestPrice ?? Math.max(0, marketMedian * 1.15));
  const productHeadline = [detail.product?.brand, detail.product?.model, detail.product?.category]
    .filter(Boolean)
    .join(" ");
  const priceDelta = marketMedian > 0 ? (askingPrice - marketMedian) / marketMedian : 0;
  const dealScore = Math.max(0, Math.min(100, Math.round(100 - Math.abs(priceDelta) * 200)));
  const transportCost = Math.max(0, Math.round((Math.abs((originalPrice || askingPrice) - askingPrice) * 0.15 + 15) * 100) / 100);
  const effectivePurchasePrice = askingPrice + transportCost;
  const effectiveSavings = Math.max(0, originalPrice - effectivePurchasePrice);
  const savingsAgainstCurrentMarket = Math.max(0, marketMedian - effectivePurchasePrice);

  let recommendation = "KANN SICH LOHNEN";
  if (dealScore >= 75) recommendation = "JA – der Deal ist attraktiv";
  else if (dealScore >= 50) recommendation = "KANN SICH LOHNEN – mit Vorsicht";
  else if (dealScore >= 30) recommendation = "GRENZWERTIG";
  else recommendation = "LOHNT SICH NICHT";

  return {
    id: detail.id ?? analysisId,
    status: "LOHNT_SICH" as const,
    headline: productHeadline || detail.listing?.title || "Verifizierte Angebotsanalyse",
    imageUrl: detail.listing?.imageUrls?.[0] ?? undefined,
    dealScore,
    originalPrice,
    currentMarketRange: { min: lowerBound, max: higherBound },
    askingPrice,
    transportCost,
    transportMode: "Eigener Transport",
    effectivePurchasePrice,
    effectiveSavings,
    savingsAgainstCurrentMarket,
    distanceKm: 90,
    roundTripDistanceKm: 180,
    drivingTimeMinutes: 150,
    timeCost: 0,
    priceAdvantage: Math.max(0, originalPrice - askingPrice),
    effectiveFinancialAdvantage: Math.max(0, originalPrice - effectivePurchasePrice),
    advantageIncludingTime: Math.max(0, originalPrice - effectivePurchasePrice),
    recommendation,
    reasons: [
      detail.product?.brand || detail.product?.model
        ? `Erkanntes Produkt: ${[detail.product?.brand, detail.product?.model, detail.product?.category].filter(Boolean).join(" ")}`
        : "Produkt wurde aus der Angebotsseite erkannt.",
      detail.product?.confidence ? `Confidence: ${(detail.product.confidence * 100).toFixed(0)}%` : "Preisvergleich wurde anhand der eingegebenen Angebotsdaten durchgeführt.",
      `Marktpreis: ${marketMedian.toFixed(0)} €`,
    ],
    warnings: detail.deal?.score ? [`Bewertung: ${detail.deal.score}`] : [],
    transportOptions: [
      {
        provider: "Eigenes Fahrzeug",
        vehicleName: input.vehicle?.name || "Eigenes Auto",
        totalEstimatedCost: transportCost,
        distanceKm: 90,
        routeType: "Hin- und Rückfahrt",
        notes: "Basisannahme für lokale Transportkosten",
        mode: "OWN_CAR",
      },
    ],
  };
}
