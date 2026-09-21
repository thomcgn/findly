import type {
  AnalysisDetail,
  AnalysisStatus,
  AnalysisStatusResponse,
} from "@/types/analysis";
export const id = "00000000-0000-4000-8000-000000000001";
export const now = "2026-09-19T10:00:00Z";
export function status(
  state: AnalysisStatus = "CREATED",
): AnalysisStatusResponse {
  return {
    id,
    status: state,
    progress: state === "COMPLETED" ? 100 : 10,
    createdAt: now,
    updatedAt: now,
    startedAt: now,
    completedAt: null,
    failedAt: null,
    errorCode: null,
    errorMessage: null,
    warnings: [],
  };
}
export const partial: AnalysisDetail = {
  id,
  status: "COMPLETED",
  listing: {
    title: "Fixture chair",
    price: null,
    currency: null,
    url: "https://kleinanzeigen.de/fixture",
    imageUrls: [],
  },
  product: null,
  market: { medianPrice: null, lowestPrice: null, highestPrice: null },
  deal: { score: null, differencePercent: null },
  warnings: ["PRODUCT_NOT_IDENTIFIED", "PRICE_NOT_VERIFIED"],
  candidates: [],
  attributes: [],
  priceEvidence: [],
  comparisons: [],
};
export const response = (data: unknown, code = 200, headers?: HeadersInit) =>
  new Response(JSON.stringify(data), { status: code, headers });
