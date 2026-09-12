import { AnalysisForm } from "@/components/analysis-form";

export default function Home() {
  return (
    <main className="min-h-screen bg-slate-100 text-slate-900">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <header className="mb-8 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.22em] text-cyan-700">
                Findly MVP
              </p>
              <h1 className="mt-3 text-4xl font-black tracking-tight text-slate-900 sm:text-5xl">
                Kleinanzeigen-Angebot analysieren
              </h1>
            </div>
            <div className="rounded-2xl border border-cyan-100 bg-cyan-50 px-4 py-3 text-sm text-cyan-900">
              Produktidentifikation • Preisvergleich • Confidence Score
            </div>
          </div>
        </header>

        <AnalysisForm />
      </div>
    </main>
  );
}
