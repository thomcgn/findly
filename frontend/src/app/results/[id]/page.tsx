import { AnalysisResultPage } from "@/components/analysis-result";
export default async function ResultPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <AnalysisResultPage key={id} id={id} />;
}
