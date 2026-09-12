import type { DecisionResult, OfferInput } from "@/types/decision";

const BACKEND_URL = "http://localhost:8080";

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
    throw new Error(message || "Die Analyse konnte nicht gestartet werden.");
  }

  const startData = (await createResponse.json()) as { id?: string; status?: string };
  const analysisId = startData.id;

  if (!analysisId) {
    throw new Error("Die Analyseantwort war ungültig.");
  }

  const detailResponse = await fetch(`${BACKEND_URL}/api/analyses/${analysisId}`);
  if (!detailResponse.ok) {
    const message = await detailResponse.text();
    throw new Error(message || "Die Analyse wurde gestartet, aber die Details konnten nicht geladen werden.");
  }

  const detail = (await detailResponse.json()) as {
    id: string;
    status?: string;
    listing?: { title?: string; price?: number; currency?: string; url?: string };
    product?: { brand?: string; model?: string; category?: string; confidence?: number };
    market?: { medianPrice?: number; lowestPrice?: number; highestPrice?: number };
    deal?: { score?: string; differencePercent?: number };
  };

  const askingPrice = toNumber(input.askingPrice ?? detail.listing?.price ?? 0);
  const originalPrice = toNumber(input.originalPrice ?? detail.market?.highestPrice ?? detail.listing?.price ?? 0);
  const marketMedian = toNumber(detail.market?.medianPrice ?? detail.listing?.price ?? 0);
  const lowerBound = toNumber(detail.market?.lowestPrice ?? Math.max(0, marketMedian * 0.85));
  const higherBound = toNumber(detail.market?.highestPrice ?? Math.max(0, marketMedian * 1.15));
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
    headline: detail.listing?.title || "Verifizierte Angebotsanalyse",
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
      detail.product?.brand ? `Erkanntes Produkt: ${detail.product.brand}` : "Produkt wurde aus der Angebotsseite erkannt.",
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
