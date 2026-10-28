import type { Metadata } from "next";
import { cookies } from "next/headers";
import { ApiError, getDailyReport } from "@/lib/api";

export const metadata: Metadata = { title: "日終報表" };
export const dynamic = "force-dynamic";

export default async function DailyReportPage() {
  let xml: string;
  try {
    const cookieName = process.env.JWT_COOKIE_NAME ?? "tradeguard_session";
    const sessionCookie = (await cookies()).get(cookieName);
    const cookieHeader = sessionCookie
      ? `${cookieName}=${sessionCookie.value}`
      : "";
    xml = await getDailyReport(cookieHeader);
  } catch (error) {
    const message = error instanceof ApiError && error.status === 401
      ? "請登入後查看日終報表。"
      : error instanceof ApiError && error.status === 403
        ? "此報表僅限 AUDITOR 或 ADMIN 角色查看。"
        : "日終報表目前無法取得，請確認後端服務狀態。";
    return (
      <>
        <section className="hero">
          <p className="eyebrow">END-OF-DAY</p>
          <h1>日終成交報表</h1>
        </section>
        <p className="notice error">{message}</p>
      </>
    );
  }
  return (
    <>
      <section className="hero">
        <p className="eyebrow">END-OF-DAY · SECURE REPORT</p>
        <h1>日終成交報表</h1>
        <p>報表資料由後端依登入權限提供，不會進入跨使用者共用快取。</p>
      </section>
      <section className="panel">
        <pre className="xml-preview">{xml}</pre>
        <a className="button-link" href="/api/reports/daily.xml">下載 XML</a>
      </section>
    </>
  );
}
