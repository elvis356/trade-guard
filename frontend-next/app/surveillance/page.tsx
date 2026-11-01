"use client";

import { useCallback, useEffect, useState } from "react";
import MuiAlert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { PageHeading, PanelHeading } from "@/components/page-heading";
import { RoleGate } from "@/components/role-gate";
import { useAuth } from "@/components/auth-provider";
import { api, errorMessage, type Alert, type Page, type SurveillanceCase } from "@/lib/client-api";

function SurveillanceWorkspace() {
  const { user } = useAuth();
  const canAct = user?.role === "SURVEILLANCE";
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [cases, setCases] = useState<SurveillanceCase[]>([]);
  const [notes, setNotes] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [alertPage, casePage] = await Promise.all([
        api<Page<Alert>>("/api/alerts?page=0&size=100&sort=occurredAt,desc"),
        api<Page<SurveillanceCase>>("/api/cases?page=0&size=100&sort=createdAt,desc"),
      ]);
      setAlerts(alertPage.content);
      setCases(casePage.content);
    } catch (loadError) {
      setError(errorMessage(loadError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  async function proposeCase(alert: Alert) {
    setBusyId(alert.id);
    setNotice("");
    setError("");
    try {
      await api<SurveillanceCase>(`/api/alerts/${alert.id}/case`, { method: "POST" });
      setNotice(`警示 #${alert.id} 已提報案件。`);
      await load();
    } catch (actionError) {
      setError(errorMessage(actionError));
    } finally {
      setBusyId(null);
    }
  }

  async function reviewCase(item: SurveillanceCase, approve: boolean) {
    setBusyId(item.id);
    setNotice("");
    setError("");
    try {
      await api<SurveillanceCase>(`/api/cases/${item.id}/review`, {
        method: "POST",
        body: JSON.stringify({ approve, note: notes[item.id]?.trim() }),
      });
      setNotice(`案件 #${item.id} 已${approve ? "核准" : "駁回"}。`);
      setNotes((current) => ({ ...current, [item.id]: "" }));
      await load();
    } catch (actionError) {
      setError(errorMessage(actionError));
    } finally {
      setBusyId(null);
    }
  }

  const caseAlertIds = new Set(cases.map((item) => item.alertId));
  const pending = cases.filter((item) => item.status === "PROPOSED");

  return (
    <>
      <PageHeading
        description="檢視規則式警示、提報案件並完成獨立審核。警示僅供人工調查，不代表不當行為定論。"
        eyebrow="SURVEILLANCE · CASE REVIEW"
        title="監控與案件"
      />
      {(error || notice) && <MuiAlert role={error ? "alert" : "status"} severity={error ? "error" : "success"} sx={{ mb: 2 }}>{error || notice}</MuiAlert>}
      <Paper className="panel list-panel" component="section" elevation={0} variant="outlined">
        <PanelHeading eyebrow="RULE-BASED SIGNALS" title="最新警示" trailing={<Button onClick={() => void load()} type="button" variant="outlined">重新整理</Button>} />
        {loading ? <Typography className="empty-row" color="text.secondary" component="p">載入警示…</Typography> : alerts.length === 0 ? <Typography className="empty-row" color="text.secondary" component="p">目前沒有警示。</Typography> : (
          <TableContainer className="table-scroll">
            <Table aria-label="最新警示" size="small">
              <TableHead><TableRow><TableCell>時間</TableCell><TableCell>等級</TableCell><TableCell>規則</TableCell><TableCell>商品</TableCell><TableCell>成交編號</TableCell><TableCell>說明</TableCell><TableCell>操作</TableCell></TableRow></TableHead>
              <TableBody>{alerts.map((alert) => <TableRow key={alert.id}>
                <TableCell>{new Date(alert.occurredAt).toLocaleString("zh-TW")}</TableCell>
                <TableCell><Chip className={`severity-${alert.severity.toLowerCase()}`} color={alert.severity === "HIGH" || alert.severity === "CRITICAL" ? "error" : alert.severity === "MEDIUM" ? "warning" : "info"} label={alert.severity} size="small" variant="outlined" /></TableCell>
                <TableCell>{alert.ruleCode}</TableCell><TableCell className="symbol">{alert.symbol}</TableCell><TableCell>#{alert.tradeId}</TableCell>
                <TableCell className="description-cell">{alert.description}</TableCell>
                <TableCell>{canAct ? caseAlertIds.has(alert.id) ? <Chip label="已提報" size="small" variant="outlined" /> : <Button disabled={busyId === alert.id} onClick={() => void proposeCase(alert)} size="small" type="button" variant="outlined">提報案件</Button> : <Typography color="text.secondary" variant="body2">唯讀</Typography>}</TableCell>
              </TableRow>)}</TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>
      <Paper className="panel list-panel" component="section" elevation={0} variant="outlined">
        <PanelHeading eyebrow="INVESTIGATION QUEUE" title="案件清單" trailing={<Chip color={pending.length ? "warning" : "default"} label={`${pending.length} 件待審`} size="small" variant="outlined" />} />
        {loading ? <Typography className="empty-row" color="text.secondary" component="p">載入案件…</Typography> : cases.length === 0 ? <Typography className="empty-row" color="text.secondary" component="p">尚無案件。</Typography> : (
          <Stack className="case-list" spacing={1.5}>{cases.map((item) => {
            const canReview = canAct && item.status === "PROPOSED" && item.proposedBy !== user?.username;
            return <Paper className="case-card" component="article" elevation={0} key={item.id} variant="outlined">
              <Stack className="case-heading" direction="row" sx={{ alignItems: "center", flexWrap: "wrap", justifyContent: "space-between", gap: 1 }}>
                <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                  <Typography className="case-id" component="span" variant="overline">CASE / {String(item.id).padStart(5, "0")}</Typography>
                  <Chip color={item.status === "APPROVED" ? "success" : item.status === "REJECTED" ? "default" : "warning"} label={item.status} size="small" variant="outlined" />
                </Stack>
                <Typography color="text.secondary" component="span" variant="caption">關聯警示 #{item.alertId} · {new Date(item.createdAt).toLocaleString("zh-TW")}</Typography>
              </Stack>
              <Typography component="p" sx={{ mt: 1.5 }} variant="body2">提報人：<strong>{item.proposedBy}</strong>{item.reviewedBy && <>　審核人：<strong>{item.reviewedBy}</strong></>}</Typography>
              {item.reviewNote && <MuiAlert className="review-note" severity="info" sx={{ mt: 1 }}>審核備註：{item.reviewNote}</MuiAlert>}
              {canReview && <Box className="review-controls">
                <TextField label="審核備註（必填）" onChange={(event) => setNotes({ ...notes, [item.id]: event.target.value })} placeholder="說明調查結果與決定理由" slotProps={{ htmlInput: { maxLength: 500 } }} value={notes[item.id] ?? ""} />
                <Stack className="button-row" direction="row">
                  <Button disabled={busyId === item.id || !notes[item.id]?.trim()} onClick={() => void reviewCase(item, true)} type="button" variant="contained">核准</Button>
                  <Button color="error" disabled={busyId === item.id || !notes[item.id]?.trim()} onClick={() => void reviewCase(item, false)} type="button" variant="outlined">駁回</Button>
                </Stack>
              </Box>}
              {canAct && item.status === "PROPOSED" && item.proposedBy === user?.username && <Typography className="form-hint" color="text.secondary" component="p" sx={{ mt: 1 }} variant="body2">案件提報者不得審核自己的案件。</Typography>}
            </Paper>;
          })}</Stack>
        )}
      </Paper>
    </>
  );
}

export default function SurveillancePage() {
  return <RoleGate roles={["SURVEILLANCE", "AUDITOR", "ADMIN"]}><SurveillanceWorkspace /></RoleGate>;
}
