import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AnalysisForm } from "@/components/analysis-form";
import {
  AnalysisResultPage,
  ResultDetails,
} from "@/components/analysis-result";
import { id, now, partial, response, status } from "./fixtures";
const { push } = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
afterEach(() => {
  vi.unstubAllGlobals();
  push.mockReset();
});
it("submits only the URL and navigates to a persistent results route", async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(response({ id, status: "CREATED" }, 202));
  vi.stubGlobal("fetch", fetch);
  render(<AnalysisForm />);
  expect(screen.getByRole("textbox").getAttribute("value")).toBe("");
  fireEvent.change(screen.getByRole("textbox"), {
    target: { value: "https://kleinanzeigen.de/fixture" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Analyse starten" }));
  await waitFor(() => expect(push).toHaveBeenCalledWith(`/results/${id}`));
  expect(fetch).toHaveBeenCalledTimes(1);
  expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({
    url: "https://kleinanzeigen.de/fixture",
  });
});
it("renders unknown values and unavailable transport without synthetic numbers", () => {
  const { container } = render(<ResultDetails result={partial} />);
  expect(
    screen.getByText("Keine verifizierten Preisquellen verfügbar."),
  ).toBeDefined();
  expect(screen.getByText("Unbekannt")).toBeDefined();
  expect(screen.getByText(/Noch nicht verfügbar. Entfernung/)).toBeDefined();
  expect(container.textContent).not.toMatch(/0\s*€|90 km|Deal Score/);
});
it("renders signed surcharge, original-price separation and source attribution", () => {
  const quote = {
    amount: 100,
    currency: "EUR",
    sourceName: "Fixture market",
    source: "https://fixture.test/price",
    retrievedAt: now,
    kind: "CURRENT_USED" as const,
    confidence: 0.9,
    sourceType: "MARKETPLACE" as const,
  };
  render(
    <ResultDetails
      result={{
        ...partial,
        listing: { ...partial.listing, price: 120, currency: "EUR" },
        priceEvidence: [quote],
        comparisons: [
          {
            kind: "CURRENT_USED",
            currency: "EUR",
            referencePrice: 100,
            savings: -20,
            savingsPercent: -20,
            outcome: "SURCHARGE",
            sources: [quote],
          },
        ],
      }}
    />,
  );
  expect(
    screen.getByText(/Aufpreis \(negative Ersparnis\): -20/),
  ).toBeDefined();
  expect(screen.getAllByText(/Unbekannt – kein passender/)).toHaveLength(2);
  expect(
    screen
      .getAllByRole("link", { name: "Fixture market" })[0]
      .getAttribute("href"),
  ).toBe(quote.source);
});
it("marks low-confidence candidates and never creates executable evidence links", () => {
  render(
    <ResultDetails
      result={{
        ...partial,
        candidates: [
          {
            product: {
              name: "Possible chair",
              brand: null,
              model: null,
              category: null,
              identifiers: {},
              evidence: [
                {
                  attribute: "model",
                  value: "Chair",
                  source: "javascript:alert(1)",
                  retrievedAt: now,
                },
              ],
            },
            confidence: 0.7,
            components: { model: 1 },
            weights: { model: 3 },
            identifierConflict: false,
            eligible: false,
          },
        ],
      }}
    />,
  );
  expect(screen.getByText(/Unsicher: unter 75/)).toBeDefined();
  expect(screen.queryByRole("link", { name: "Quelle" })).toBeNull();
  expect(
    screen.getByText(
      /Produkt nicht eindeutig identifiziert. Eine Kandidatenliste/,
    ),
  ).toBeDefined();
});
it("shows terminal errors safely and does not fetch a failed result", async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(
      response({
        ...status("FAILED"),
        errorCode: "ANALYSIS_TIMEOUT",
        errorMessage: "secret raw failure",
      }),
    );
  vi.stubGlobal("fetch", fetch);
  render(<AnalysisResultPage id={id} />);
  await waitFor(() =>
    expect(screen.getByRole("alert").textContent).toContain("Zeitlimit"),
  );
  expect(screen.queryByText(/secret raw/)).toBeNull();
  expect(fetch).toHaveBeenCalledTimes(1);
});
it("reloads an existing analysis after a network error without a second POST", async () => {
  const fetch = vi
    .fn()
    .mockRejectedValueOnce(new TypeError("private network detail"))
    .mockResolvedValueOnce(response(status("COMPLETED")))
    .mockResolvedValueOnce(response(partial));
  vi.stubGlobal("fetch", fetch);
  render(<AnalysisResultPage id={id} />);
  await screen.findByRole("alert");
  fireEvent.click(screen.getByRole("button", { name: "Erneut laden" }));
  await screen.findByRole("heading", { name: "Fixture chair" });
  expect(fetch.mock.calls.every((call) => call[1].method === "GET")).toBe(true);
});

it("prevents duplicate starts and aborts the request on unmount", () => {
  const fetch = vi.fn().mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetch);
  const { container, unmount } = render(<AnalysisForm />);
  fireEvent.change(screen.getByRole("textbox"), {
    target: { value: "https://kleinanzeigen.de/fixture" },
  });
  const form = container.querySelector("form");
  if (!form) throw new Error("Missing form");
  fireEvent.submit(form);
  fireEvent.submit(form);
  expect(fetch).toHaveBeenCalledTimes(1);
  unmount();
  expect(fetch.mock.calls[0][1].signal.aborted).toBe(true);
});
