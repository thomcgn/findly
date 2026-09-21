"use client";
import { useEffect, useState } from "react";
import {
  AnalysisApiError,
  getAnalysisResult,
  getAnalysisStatus,
} from "@/lib/api/analysis";
import type { AnalysisDetail, AnalysisStatusResponse } from "@/types/analysis";

export interface AnalysisState {
  status?: AnalysisStatusResponse;
  result?: AnalysisDetail;
  error?: unknown;
}
export function pollDelay(attempt: number) {
  return attempt === 0 ? 1000 : attempt === 1 ? 2000 : 5000;
}
export function waitForPoll(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    signal.throwIfAborted();
    const abort = () => {
      clearTimeout(timer);
      reject(signal.reason);
    };
    const timer = setTimeout(() => {
      signal.removeEventListener("abort", abort);
      resolve();
    }, ms);
    signal.addEventListener("abort", abort, { once: true });
  });
}
export async function watchAnalysis(
  id: string,
  signal: AbortSignal,
  update: (state: AnalysisState) => void,
) {
  let attempt = 0;
  while (!signal.aborted) {
    try {
      const status = await getAnalysisStatus(id, signal);
      signal.throwIfAborted();
      update({ status });
      if (status.status === "FAILED") return;
      if (status.status === "COMPLETED") {
        const result = await getAnalysisResult(id, signal);
        signal.throwIfAborted();
        update({ status, result });
        return;
      }
    } catch (error) {
      if (signal.aborted) return;
      // A throttled read may be retried; POST is never retried automatically.
      if (
        error instanceof AnalysisApiError &&
        (error.status === 429 || error.status === 409)
      ) {
        await waitForPoll(
          Math.max(pollDelay(attempt++), error.retryAfterMs ?? 0),
          signal,
        );
        continue;
      }
      throw error;
    }
    await waitForPoll(pollDelay(attempt++), signal);
  }
}
export function useAnalysis(id: string, refresh = 0) {
  const [entry, setEntry] = useState<{
    id: string;
    refresh: number;
    state: AnalysisState;
  }>();
  useEffect(() => {
    const controller = new AbortController();
    const update = (state: AnalysisState) => {
      if (!controller.signal.aborted) setEntry({ id, refresh, state });
    };
    void watchAnalysis(id, controller.signal, update).catch((error) => {
      if (!controller.signal.aborted) update({ error });
    });
    return () => controller.abort();
  }, [id, refresh]);
  return entry?.id === id && entry.refresh === refresh ? entry.state : {};
}
