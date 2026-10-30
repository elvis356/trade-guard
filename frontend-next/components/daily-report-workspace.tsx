"use client";

import { useEffect, useState } from "react";
import { ReportDateForm } from "@/components/report-date-form";
import { RoleGate } from "@/components/role-gate";
import { ApiError, type Role } from "@/lib/client-api";

function DailyReportContent({ date }: { date: string }) {
  const [xml, setXml] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setXml("");

    async function loadReport() {
      try {
        const response = await fetch(
          `/api/reports/daily.xml?date=${encodeURIComponent(date)}`,
          {
            cache: "no-store",
            credentials: "same-origin",
            signal: controller.signal,
          },
        );
        if (!response.ok) {
          let message = `日終報表無法取得 (HTTP ${response.status})`;
          try {
            const body = await response.json() as { message?: string };
            if (body.message) message = body.message;
          } catch {
            // Use the status message for a non-JSON response.
          }
          throw new ApiError(response.status, message);
        }
        setXml(await response.text());
      } catch (requestError) {
        if (controller.signal.aborted) return;
        if (requestError instanceof ApiError && requestError.status === 401) {
          setError("請登入後查看日終報表。");
        } else if (requestError instanceof ApiError && requestError.status === 403) {
          setError("此報表僅限 AUDITOR 或 ADMIN 角色查看。");
        } else {
          setError(requestError instanceof Error
            ? requestError.message
            : "日終報表目前無法取得，請確認後端服務狀態。");
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadReport();
    return () => controller.abort();
  }, [date]);

  return (
    <>
      <section className="hero">
        <p className="eyebrow">END-OF-DAY · SECURE REPORT</p>
        <h1>日終成交報表</h1>
        <p>報表日期：{date}。資料由後端依登入權限提供，不會進入跨使用者共用快取。</p>
      </section>
      <section className="panel">
        <ReportDateForm initialDate={date} key={date} />
        {loading ? <p className="notice" role="status">正在載入日終報表…</p>
          : error ? <p className="notice error" role="alert">{error}</p>
            : <pre className="xml-preview">{xml}</pre>}
      </section>
    </>
  );
}

export function DailyReportWorkspace({ date }: { date: string }) {
  const roles: Role[] = ["AUDITOR", "ADMIN"];
  return <RoleGate roles={roles}><DailyReportContent date={date} /></RoleGate>;
}
