export type TransportMode = "OWN_CAR" | "RENTAL" | "ONE_WAY_RENTAL";
export type DealDecision = "EXTREM_GUTER_DEAL" | "LOHNT_SICH" | "KANNSICHLOHNTEN" | "GRENZWERTIG" | "LOHNT_SICH_NICHT";

export interface VehicleProfile {
  id: string;
  name: string;
  fuelType: "PETROL" | "DIESEL" | "ELECTRIC" | "HYBRID";
  consumptionPer100Km?: number;
  fuelPricePerUnit?: number;
  cargoVolumeLiters?: number;
  maxPayloadKg?: number;
}

export interface OfferInput {
  url: string;
  productName: string;
  originalPrice: number;
  askingPrice: number;
  homeLocation: string;
  sellerLocation: string;
  vehicle: VehicleProfile;
  timeValuePerHour?: number;
  includeTimeCosts: boolean;
}

export interface DecisionMetric {
  label: string;
  value: string;
}

export interface TransportOption {
  provider: string;
  vehicleName: string;
  totalEstimatedCost: number;
  distanceKm: number;
  routeType: string;
  notes: string;
  mode: TransportMode;
}

export interface DecisionResult {
  id: string;
  status: DealDecision;
  headline: string;
  dealScore: number;
  originalPrice: number;
  currentMarketRange: {
    min: number;
    max: number;
  };
  askingPrice: number;
  transportCost: number;
  transportMode: string;
  effectivePurchasePrice: number;
  effectiveSavings: number;
  savingsAgainstCurrentMarket: number;
  distanceKm: number;
  roundTripDistanceKm: number;
  drivingTimeMinutes: number;
  timeCost: number;
  priceAdvantage: number;
  effectiveFinancialAdvantage: number;
  advantageIncludingTime: number;
  recommendation: string;
  reasons: string[];
  warnings: string[];
  transportOptions: TransportOption[];
}
