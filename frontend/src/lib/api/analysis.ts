import { z } from "zod";
import {
  problemSchema,
  resultSchema,
  startSchema,
  statusResponseSchema,
} from "@/types/analysis";

const BACKEND_URL = (
  process.env.NEXT_PUBLIC_API_URL?.trim() || "http://localhost:8080"
).replace(/\/+$/, "");
const messages: Record<string, string> = {
  INVALID_REQUEST: "Bitte überprüfe deine Eingabe.",
  INVALID_URL: "Bitte gib eine gültige HTTPS-Kleinanzeigen-URL ein.",
  ANALYSIS_NOT_FOUND: "Diese Analyse wurde nicht gefunden.",
  RESULT_NOT_READY: "Das Ergebnis ist noch nicht verfügbar.",
  ANALYSIS_FAILED: "Die Analyse konnte nicht abgeschlossen werden.",
  ANALYSIS_TIMEOUT: "Die Analyse hat ihr Zeitlimit erreicht.",
  ANALYSIS_QUEUE_FULL:
    "Die Analyse-Warteschlange ist voll. Bitte versuche es später erneut.",
  RATE_LIMIT_EXCEEDED: "Zu viele Anfragen. Bitte versuche es später erneut.",
  LISTING_ACCESS_BLOCKED:
    "Das Inserat ist gesperrt oder verlangt eine Zugriffsprüfung.",
  LISTING_PARSE_FAILED: "Das Inserat konnte nicht ausgelesen werden.",
  LISTING_TARGET_BLOCKED: "Diese Inseratsadresse ist nicht zulässig.",
  LISTING_FETCH_FAILED: "Das Inserat konnte nicht abgerufen werden.",
  INVALID_RESPONSE: "Der Server hat eine ungültige Antwort geliefert.",
  NETWORK_ERROR: "Der Server ist nicht erreichbar. Bitte versuche es erneut.",
};
export class AnalysisApiError extends Error {
  constructor(
    public readonly code: string,
    public readonly status = 0,
    public readonly traceId?: string,
    public readonly retryAfterMs?: number,
  ) {
    super(
      messages[code] ??
        "Die Anfrage konnte nicht abgeschlossen werden. Bitte versuche es erneut.",
    );
    this.name = "AnalysisApiError";
  }
}
export function errorMessage(error: unknown): string {
  return error instanceof AnalysisApiError
    ? error.message
    : "Die Anfrage konnte nicht abgeschlossen werden. Bitte versuche es erneut.";
}
async function request<T>(
  path: string,
  schema: z.ZodType<T>,
  signal: AbortSignal,
  body?: { url: string },
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${BACKEND_URL}/api/analyses${path}`, {
      method: body ? "POST" : "GET",
      signal,
      cache: "no-store",
      headers: {
        Accept: "application/json, application/problem+json",
        ...(body ? { "Content-Type": "application/json" } : {}),
      },
      ...(body ? { body: JSON.stringify(body) } : {}),
    });
  } catch (error) {
    if (signal.aborted) throw error;
    throw new AnalysisApiError("NETWORK_ERROR");
  }
  const data: unknown = await response.json().catch(() => null);
  signal.throwIfAborted();
  if (!response.ok) {
    const problem = problemSchema.safeParse(data);
    const retryHeader = response.headers.get("Retry-After");
    const retry =
      retryHeader === null
        ? NaN
        : /^\d+$/.test(retryHeader)
          ? Number(retryHeader) * 1000
          : Date.parse(retryHeader) - Date.now();
    throw new AnalysisApiError(
      problem.success
        ? problem.data.code
        : response.status === 429
          ? "RATE_LIMIT_EXCEEDED"
          : "REQUEST_FAILED",
      response.status,
      problem.success ? problem.data.traceId : undefined,
      Number.isFinite(retry) ? Math.max(0, retry) : undefined,
    );
  }
  const parsed = schema.safeParse(data);
  if (!parsed.success)
    throw new AnalysisApiError("INVALID_RESPONSE", response.status);
  return parsed.data;
}
function path(id: string) {
  if (!z.string().uuid().safeParse(id).success)
    throw new AnalysisApiError("ANALYSIS_NOT_FOUND", 404);
  return `/${id}`;
}
export const startAnalysis = (url: string, signal: AbortSignal) =>
  request("", startSchema, signal, { url: url.trim() });
function sameAnalysis<T extends { id: string }>(id: string, value: T): T {
  if (value.id.toLowerCase() !== id.toLowerCase())
    throw new AnalysisApiError("INVALID_RESPONSE");
  return value;
}
export const getAnalysisStatus = (id: string, signal: AbortSignal) =>
  request(path(id), statusResponseSchema, signal).then((value) =>
    sameAnalysis(id, value),
  );
export const getAnalysisResult = (id: string, signal: AbortSignal) =>
  request(`${path(id)}/result`, resultSchema, signal).then((value) =>
    sameAnalysis(id, value),
  );
