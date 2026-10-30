"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";

export function ReportDateForm({ initialDate }: { initialDate: string }) {
  const [date, setDate] = useState(initialDate);
  const router = useRouter();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    router.push(`/reports/daily?date=${encodeURIComponent(date)}`);
  }

  const downloadHref = `/api/reports/daily.xml?date=${encodeURIComponent(initialDate)}`;

  return (
    <div className="report-controls">
      <form className="report-date-form" onSubmit={submit}>
        <label className="field"><span>報表日期</span><input max={new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Taipei" })} onChange={(event) => setDate(event.target.value)} required type="date" value={date} /></label>
        <button className="button-link" type="submit">載入報表</button>
      </form>
      <a className="button-quiet" href={downloadHref}>下載 XML</a>
    </div>
  );
}
