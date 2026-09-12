export type AnalysisStatus = "PENDING" | "ANALYZING" | "COMPLETED" | "FAILED";
export type DealScore = "VERY_GOOD" | "GOOD" | "FAIR" | "EXPENSIVE" | "VERY_EXPENSIVE";
export type PriceCondition = "NEW" | "USED" | "UNKNOWN";

export interface CreateAnalysisRequest {
  url: string;
}

export interface AnalysisStartResponse {
  analysisId?: string;
  id?: string;
  status: AnalysisStatus;
}

export interface ListingSummary {
  title: string;
  price: number;
  currency: string;
  url: string;
  imageUrl?: string;
  imageUrls?: string[];
}

export interface ProductSummary {
  brand: string;
  model: string;
  category: string;
  confidence: number;
}

export interface MarketSummary {
  medianPrice: number;
  lowestPrice: number;
  highestPrice: number;
}

export interface PriceSource {
  sourceName: string;
  productTitle: string;
  price: number;
  currency: string;
  url: string;
  condition: PriceCondition;
}

export interface AnalysisDetail {
  id: string;
  status: AnalysisStatus;
  listing: ListingSummary;
  product: ProductSummary;
  market: MarketSummary;
  deal: {
    score: DealScore;
    differencePercent: number;
  };
  sources: PriceSource[];
}
