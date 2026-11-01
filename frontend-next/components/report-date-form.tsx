"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import Button from "@mui/material/Button";
import Box from "@mui/material/Box";
import TextField from "@mui/material/TextField";

export function ReportDateForm({ initialDate }: { initialDate: string }) {
  const [date, setDate] = useState(initialDate);
  const router = useRouter();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    router.push(`/reports/daily?date=${encodeURIComponent(date)}`);
  }

  const downloadHref = `/api/reports/daily.xml?date=${encodeURIComponent(initialDate)}`;

  return (
    <Box className="report-controls">
      <Box className="report-date-form" component="form" onSubmit={submit}>
        <TextField label="報表日期" onChange={(event) => setDate(event.target.value)} required slotProps={{ htmlInput: { max: new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Taipei" }) }, inputLabel: { shrink: true } }} type="date" value={date} />
        <Button type="submit" variant="contained">載入報表</Button>
      </Box>
      <Button component="a" href={downloadHref} variant="outlined">下載 XML</Button>
    </Box>
  );
}
