import type { Metadata } from "next";
import Alert from "@mui/material/Alert";
import { DailyReportWorkspace } from "@/components/daily-report-workspace";
import { PageHeading } from "@/components/page-heading";

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
        <PageHeading eyebrow="END-OF-DAY" title="日終成交報表" />
        <Alert role="alert" severity="error">日期格式無效，請使用 YYYY-MM-DD。</Alert>
      </>
    );
  }
  const reportDate = date ?? new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Taipei" });
  return <DailyReportWorkspace date={reportDate} />;
}
