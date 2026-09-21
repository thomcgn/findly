import { z } from "zod";

export const statusSchema = z.enum([
  "CREATED",
  "FETCHING_LISTING",
  "EXTRACTING_LISTING",
  "IDENTIFYING_PRODUCT",
  "RESEARCHING_PRICES",
  "COMPLETED",
  "FAILED",
]);
const money = z.number().finite();
const confidence = z.number().min(0).max(1);
const instant = z.string().datetime({ offset: true });
export const startSchema = z.object({
  id: z.string().uuid(),
  status: statusSchema,
});
export const statusResponseSchema = startSchema.extend({
  progress: z.number().int().min(0).max(100),
  createdAt: instant,
  updatedAt: instant,
  startedAt: instant.nullable(),
  completedAt: instant.nullable(),
  failedAt: instant.nullable(),
  errorCode: z.string().nullable(),
  errorMessage: z.string().nullable(),
  warnings: z.array(z.string()),
});
const evidenceSchema = z.object({
  attribute: z.string(),
  value: z.string(),
  source: z.string(),
  retrievedAt: instant,
});
const priceKind = z.enum(["ORIGINAL", "HISTORICAL_ORIGINAL", "CURRENT_USED"]);
const quoteSchema = z.object({
  amount: money,
  currency: z.string(),
  sourceName: z.string(),
  source: z.string(),
  retrievedAt: instant,
  kind: priceKind,
  confidence,
  sourceType: z.enum(["MANUFACTURER", "RETAILER", "MARKETPLACE", "OTHER"]),
});
const candidateSchema = z.object({
  product: z.object({
    name: z.string(),
    brand: z.string().nullable(),
    model: z.string().nullable(),
    category: z.string().nullable(),
    identifiers: z.record(z.string(), z.string()),
    evidence: z.array(evidenceSchema),
  }),
  confidence,
  components: z.record(z.string(), confidence),
  weights: z.record(z.string(), z.number()),
  identifierConflict: z.boolean(),
  eligible: z.boolean(),
});
export const resultSchema = z.object({
  id: z.string().uuid(),
  status: z.literal("COMPLETED"),
  listing: z.object({
    title: z.string(),
    price: money.nullable(),
    currency: z.string().nullable(),
    url: z.string(),
    imageUrls: z.array(z.string()),
  }),
  product: z
    .object({
      brand: z.string().nullable(),
      model: z.string().nullable(),
      category: z.string().nullable(),
      confidence,
    })
    .nullable(),
  market: z.object({
    medianPrice: money.nullable(),
    lowestPrice: money.nullable(),
    highestPrice: money.nullable(),
  }),
  deal: z.object({
    score: z.string().nullable(),
    differencePercent: money.nullable(),
  }),
  warnings: z.array(z.string()),
  candidates: z.array(candidateSchema),
  attributes: z.array(evidenceSchema),
  priceEvidence: z.array(quoteSchema),
  comparisons: z.array(
    z.object({
      kind: priceKind,
      currency: z.string(),
      referencePrice: money,
      savings: money.nullable(),
      savingsPercent: money.nullable(),
      outcome: z.enum(["SAVINGS", "SURCHARGE", "EQUAL"]).nullable(),
      sources: z.array(quoteSchema),
    }),
  ),
});
export const problemSchema = z.object({
  code: z.string(),
  status: z.number(),
  title: z.string().optional(),
  detail: z.string().optional(),
  traceId: z.string().optional(),
});
export type AnalysisStatus = z.infer<typeof statusSchema>;
export type AnalysisStatusResponse = z.infer<typeof statusResponseSchema>;
export type AnalysisDetail = z.infer<typeof resultSchema>;
export type PriceQuote = z.infer<typeof quoteSchema>;
export type ProductEvidence = z.infer<typeof evidenceSchema>;
