import type { Metadata } from "next";
import { DailyReportWorkspace } from "@/components/daily-report-workspace";

export const metadata: Metadata = { title: "日終報表" };
export const dynamic = "force-dynamic";

export default async function DailyReportPage({
  searchParams,
}: {
  searchParams: Promise<{ date?: string }>;
}) {
  const { date: requestedDate } = await searchParams;
  const date = requestedDate && /^\d{4}-\d{2}-\d{2}$/.test(requestedDate)
    ? requestedDate : undefined;
  if (requestedDate && !date) {
    return (
      <>
        <section className="hero"><p className="eyebrow">END-OF-DAY</p><h1>日終成交報表</h1></section>
        <p className="notice error" role="alert">日期格式無效，請使用 YYYY-MM-DD。</p>
      </>
    );
  }
  const reportDate = date ?? new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Taipei" });
  return <DailyReportWorkspace date={reportDate} />;
}
