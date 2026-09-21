"use client";
import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { useRouter } from "next/navigation";
import { errorMessage, startAnalysis } from "@/lib/api/analysis";

export function AnalysisForm() {
  const router = useRouter();
  const [url, setUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();
  const pending = useRef<AbortController | null>(null);
  useEffect(() => () => pending.current?.abort(), []);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending.current) return;
    const controller = new AbortController();
    pending.current = controller;
    setLoading(true);
    setError(undefined);
    try {
      const result = await startAnalysis(url, controller.signal);
      if (!controller.signal.aborted) router.push(`/results/${result.id}`);
    } catch (error) {
      if (!controller.signal.aborted) setError(errorMessage(error));
    } finally {
      if (!controller.signal.aborted) {
        setLoading(false);
        pending.current = null;
      }
    }
  }
  return (
    <form
      onSubmit={submit}
      className="space-y-5 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"
    >
      <label className="block space-y-2 font-medium">
        Kleinanzeigen-Link
        <input
          required
          type="url"
          maxLength={2048}
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://www.kleinanzeigen.de/s-anzeige/..."
          className="block w-full rounded-xl border border-slate-300 p-3"
        />
      </label>
      <p className="text-sm text-slate-600">
        Wir prüfen das Inserat und zeigen verfügbare Produkt- und Preisbelege.
        Fehlende Daten bleiben als unbekannt gekennzeichnet.
      </p>
      <button
        disabled={loading}
        className="rounded-xl bg-slate-900 px-5 py-3 font-semibold text-white disabled:opacity-60"
      >
        {loading ? "Analyse wird gestartet …" : "Analyse starten"}
      </button>
      {error && (
        <p role="alert" className="text-rose-800">
          {error}
        </p>
      )}
      <p className="text-sm text-slate-600">
        Transport- und persönliche Kaufentscheidung: noch nicht verfügbar.
      </p>
    </form>
  );
}
