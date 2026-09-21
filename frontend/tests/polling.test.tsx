import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { useAnalysis, watchAnalysis } from "@/hooks/use-analysis";
import { id, partial, response, status } from "./fixtures";
beforeEach(() => vi.useFakeTimers());
afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});
it("polls sequentially with 1/2/5 seconds then loads a completed result once", async () => {
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(response(status()))
    .mockResolvedValueOnce(response(status("EXTRACTING_LISTING")))
    .mockResolvedValueOnce(response(status("IDENTIFYING_PRODUCT")))
    .mockResolvedValueOnce(response(status("COMPLETED")))
    .mockResolvedValueOnce(response(partial));
  vi.stubGlobal("fetch", fetch);
  const update = vi.fn();
  const done = watchAnalysis(id, new AbortController().signal, update);
  await vi.advanceTimersByTimeAsync(0);
  expect(fetch).toHaveBeenCalledTimes(1);
  await vi.advanceTimersByTimeAsync(999);
  expect(fetch).toHaveBeenCalledTimes(1);
  await vi.advanceTimersByTimeAsync(1);
  expect(fetch).toHaveBeenCalledTimes(2);
  await vi.advanceTimersByTimeAsync(1999);
  expect(fetch).toHaveBeenCalledTimes(2);
  await vi.advanceTimersByTimeAsync(1);
  expect(fetch).toHaveBeenCalledTimes(3);
  await vi.advanceTimersByTimeAsync(5000);
  await done;
  expect(fetch).toHaveBeenCalledTimes(5);
  expect(update).toHaveBeenLastCalledWith({
    status: status("COMPLETED"),
    result: partial,
  });
  await vi.advanceTimersByTimeAsync(30000);
  expect(fetch).toHaveBeenCalledTimes(5);
});
it("stops at FAILED without requesting results", async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(
      response({ ...status("FAILED"), errorCode: "ANALYSIS_TIMEOUT" }),
    );
  vi.stubGlobal("fetch", fetch);
  const update = vi.fn();
  await watchAnalysis(id, new AbortController().signal, update);
  await vi.advanceTimersByTimeAsync(30000);
  expect(fetch).toHaveBeenCalledTimes(1);
  expect(update.mock.lastCall?.[0].status.status).toBe("FAILED");
});
it("honors Retry-After without parallel polling", async () => {
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      response({ code: "RATE_LIMIT_EXCEEDED", status: 429 }, 429, {
        "Retry-After": "8",
      }),
    )
    .mockResolvedValueOnce(response(status("FAILED")));
  vi.stubGlobal("fetch", fetch);
  const done = watchAnalysis(id, new AbortController().signal, vi.fn());
  await vi.advanceTimersByTimeAsync(7999);
  expect(fetch).toHaveBeenCalledTimes(1);
  await vi.advanceTimersByTimeAsync(1);
  await done;
  expect(fetch).toHaveBeenCalledTimes(2);
});
it("aborts in-flight reads on unmount and ignores late replies", async () => {
  let resolve!: (response: Response) => void;
  const fetch = vi.fn().mockImplementation(
    () =>
      new Promise<Response>((r) => {
        resolve = r;
      }),
  );
  vi.stubGlobal("fetch", fetch);
  const hook = renderHook(() => useAnalysis(id));
  const signal: AbortSignal = fetch.mock.calls[0][1].signal;
  hook.unmount();
  expect(signal.aborted).toBe(true);
  await act(async () => {
    resolve(response(status("COMPLETED")));
    await vi.advanceTimersByTimeAsync(10000);
  });
  expect(fetch).toHaveBeenCalledTimes(1);
});
it("cleans up pending polling timers on unmount", async () => {
  const fetch = vi.fn().mockResolvedValue(response(status()));
  vi.stubGlobal("fetch", fetch);
  const hook = renderHook(() => useAnalysis(id));
  await act(async () => {
    await vi.advanceTimersByTimeAsync(0);
  });
  hook.unmount();
  await act(async () => {
    await vi.advanceTimersByTimeAsync(20000);
  });
  expect(fetch).toHaveBeenCalledTimes(1);
  expect(vi.getTimerCount()).toBe(0);
});
it("does not display stale results when switching analysis IDs", async () => {
  const other = "00000000-0000-4000-8000-000000000002";
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(response(status("COMPLETED")))
    .mockResolvedValueOnce(response(partial))
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetch);
  const hook = renderHook(({ analysisId }) => useAnalysis(analysisId), {
    initialProps: { analysisId: id },
  });
  await act(async () => {
    await vi.advanceTimersByTimeAsync(0);
  });
  expect(hook.result.current.result?.id).toBe(id);
  hook.rerender({ analysisId: other });
  expect(hook.result.current.result).toBeUndefined();
});
