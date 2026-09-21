import { afterEach, describe, expect, it, vi } from "vitest";
import {
  AnalysisApiError,
  getAnalysisResult,
  startAnalysis,
  errorMessage,
} from "@/lib/api/analysis";
import { id, partial, response } from "./fixtures";
afterEach(() => vi.unstubAllGlobals());
describe("analysis API", () => {
  it("starts once and returns an ID without loading a result", async () => {
    const fetch = vi
      .fn()
      .mockResolvedValue(response({ id, status: "CREATED" }, 202));
    vi.stubGlobal("fetch", fetch);
    const signal = new AbortController().signal;
    expect(
      await startAnalysis(" https://kleinanzeigen.de/test ", signal),
    ).toEqual({ id, status: "CREATED" });
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch.mock.calls[0][1]).toMatchObject({
      method: "POST",
      signal,
      body: JSON.stringify({ url: "https://kleinanzeigen.de/test" }),
    });
  });
  it("uses /result and preserves nulls", async () => {
    const fetch = vi.fn().mockResolvedValue(response(partial));
    vi.stubGlobal("fetch", fetch);
    expect(await getAnalysisResult(id, new AbortController().signal)).toEqual(
      partial,
    );
    expect(fetch.mock.calls[0][0]).toMatch(new RegExp(`/${id}/result$`));
  });
  it("parses Problem Details but never exposes internal response text", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          response(
            {
              code: "RATE_LIMIT_EXCEEDED",
              status: 429,
              detail: "secret stacktrace",
              traceId: "trace-1",
            },
            429,
            { "Retry-After": "8" },
          ),
        ),
    );
    try {
      await startAnalysis("x", new AbortController().signal);
      throw new Error("expected rejection");
    } catch (error) {
      expect(error).toBeInstanceOf(AnalysisApiError);
      expect(error).toMatchObject({
        code: "RATE_LIMIT_EXCEEDED",
        status: 429,
        traceId: "trace-1",
        retryAfterMs: 8000,
      });
      expect(errorMessage(error)).not.toContain("secret");
    }
  });
  it("handles non-JSON errors and malformed success without fabricating data", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(
          new Response("<html>secret</html>", { status: 502 }),
        )
        .mockResolvedValueOnce(
          response({
            ...partial,
            listing: { ...partial.listing, price: "unknown" },
          }),
        ),
    );
    await expect(
      getAnalysisResult(id, new AbortController().signal),
    ).rejects.toMatchObject({ code: "REQUEST_FAILED" });
    await expect(
      getAnalysisResult(id, new AbortController().signal),
    ).rejects.toMatchObject({ code: "INVALID_RESPONSE" });
  });
  it("rejects responses for another analysis", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          response({ ...partial, id: "00000000-0000-4000-8000-000000000002" }),
        ),
    );
    await expect(
      getAnalysisResult(id, new AbortController().signal),
    ).rejects.toMatchObject({ code: "INVALID_RESPONSE" });
  });
  it("rejects invalid IDs before fetching", async () => {
    const fetch = vi.fn();
    vi.stubGlobal("fetch", fetch);
    expect(() =>
      getAnalysisResult("javascript:alert(1)", new AbortController().signal),
    ).toThrow(AnalysisApiError);
    expect(fetch).not.toHaveBeenCalled();
  });
});
