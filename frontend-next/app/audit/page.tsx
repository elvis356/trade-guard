"use client";

import { useState } from "react";
import Link from "next/link";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { PageHeading, PanelHeading } from "@/components/page-heading";
import { RoleGate } from "@/components/role-gate";
import { api, errorMessage, type AuditVerification, type ReconciliationReport } from "@/lib/client-api";

function AuditWorkspace() {
  const [verification, setVerification] = useState<AuditVerification | null>(null);
  const [reconciliation, setReconciliation] = useState<ReconciliationReport | null>(null);
  const [busy, setBusy] = useState<"verify" | "reconcile" | null>(null);
  const [error, setError] = useState("");

  async function run(action: "verify" | "reconcile") {
    setBusy(action);
    setError("");
    try {
      if (action === "verify") {
        setVerification(await api<AuditVerification>("/api/audit/verify", {}, true));
      } else {
        setReconciliation(await api<ReconciliationReport>("/api/audit/reconcile", {}, true));
      }
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBusy(null);
    }
  }

  return (
    <>
      <PageHeading
        description="即時檢查稽核雜湊鏈及帳本一致性，並查看具權限保護的日終成交報告。"
        eyebrow="AUDIT · INTEGRITY & RECONCILIATION"
        title="稽核工作台"
      />
      {error && <Alert role="alert" severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <Box className="workspace-grid">
        <Paper className="panel audit-action" component="section" elevation={0} variant="outlined">
          <PanelHeading eyebrow="HASH-CHAIN INTEGRITY" title="驗證稽核紀錄" trailing={<Chip label="唯讀檢查" size="small" variant="outlined" />} />
          <Typography color="text.secondary" component="p" variant="body2">逐筆驗證稽核紀錄雜湊、前後連結及目前鏈頭。</Typography>
          <Button disabled={busy !== null} onClick={() => void run("verify")} type="button" variant="contained">{busy === "verify" ? "驗證中…" : "執行鏈驗證"}</Button>
          {verification && <Paper className={`result-card ${verification.valid ? "result-good" : "result-bad"}`} elevation={0} role="status" variant="outlined">
            <Typography component="strong" variant="subtitle1">{verification.valid ? "雜湊鏈有效" : "發現鏈完整性問題"}</Typography>
            <Typography component="p" variant="body2">檢查紀錄：{verification.entriesChecked.toLocaleString("zh-TW")} 筆</Typography>
            {verification.brokenEntryId !== null && <Typography component="p" variant="body2">異常紀錄 ID：{verification.brokenEntryId}</Typography>}
            {verification.reason && <Typography component="p" variant="body2">{verification.reason}</Typography>}
            <Typography color="text.secondary" component="small" variant="caption">驗證時間：{new Date(verification.verifiedAt).toLocaleString("zh-TW")}</Typography>
          </Paper>}
        </Paper>
        <Paper className="panel audit-action" component="section" elevation={0} variant="outlined">
          <PanelHeading eyebrow="LEDGER RECONCILIATION" title="帳本對帳" trailing={<Chip label="三方核對" size="small" variant="outlined" />} />
          <Typography color="text.secondary" component="p" variant="body2">交叉核對委託、成交與委託事件，找出帳本差異及序號問題。</Typography>
          <Button disabled={busy !== null} onClick={() => void run("reconcile")} type="button" variant="contained">{busy === "reconcile" ? "對帳中…" : "執行對帳"}</Button>
          {reconciliation && <Paper className={`result-card ${reconciliation.balanced ? "result-good" : "result-bad"}`} elevation={0} role="status" variant="outlined">
            <Typography component="strong" variant="subtitle1">{reconciliation.balanced ? "帳本一致" : `發現 ${reconciliation.issues.length} 項差異`}</Typography>
            <Stack className="metric-row" direction="row">
              <Chip label={`委託 ${reconciliation.ordersChecked}`} size="small" variant="outlined" />
              <Chip label={`成交 ${reconciliation.tradesChecked}`} size="small" variant="outlined" />
              <Chip label={`事件 ${reconciliation.eventsChecked}`} size="small" variant="outlined" />
            </Stack>
            {reconciliation.issues.length > 0 && <Stack className="issue-list" component="ul" spacing={1}>{reconciliation.issues.map((issue, index) => <Box component="li" key={`${issue.code}-${issue.symbol}-${index}`}><Typography component="strong" variant="subtitle2">{issue.code}{issue.symbol && ` · ${issue.symbol}`}</Typography><Typography component="span" variant="body2">{issue.message}</Typography></Box>)}</Stack>}
            <Typography color="text.secondary" component="small" variant="caption">對帳時間：{new Date(reconciliation.reconciledAt).toLocaleString("zh-TW")}</Typography>
          </Paper>}
        </Paper>
      </Box>
      <Paper className="callout report-callout" component="section" elevation={0} variant="outlined">
        <Box>
          <Typography className="eyebrow" component="p" variant="overline">END-OF-DAY · XML</Typography>
          <Typography component="h2" variant="h5">每日成交報告</Typography>
          <Typography color="text.secondary" component="p" sx={{ mt: 1 }} variant="body2">選擇業務日期，在報表頁預覽並下載後端產生的 XML。</Typography>
        </Box>
        <Button component={Link} href="/reports/daily" variant="contained">開啟日終報表</Button>
      </Paper>
    </>
  );
}

export default function AuditPage() {
  return <RoleGate roles={["AUDITOR", "ADMIN"]}><AuditWorkspace /></RoleGate>;
}
