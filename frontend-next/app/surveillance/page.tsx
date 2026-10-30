"use client";

import { useCallback, useEffect, useState } from "react";
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
      <section className="hero">
        <p className="eyebrow">SURVEILLANCE · CASE REVIEW</p>
        <h1>監控與案件</h1>
        <p>檢視規則式警示、提報案件並完成獨立審核。警示僅供人工調查，不代表不當行為定論。</p>
      </section>
      {(error || notice) && <p className={`notice ${error ? "error" : "success"}`} role={error ? "alert" : "status"}>{error || notice}</p>}
      <section className="panel list-panel">
        <div className="panel-heading"><div><p className="eyebrow">RULE-BASED SIGNALS</p><h2>最新警示</h2></div><button className="button-quiet" onClick={() => void load()} type="button">重新整理</button></div>
        {loading ? <p className="empty-row">載入警示…</p> : alerts.length === 0 ? <p className="empty-row">目前沒有警示。</p> : (
          <div className="table-scroll"><table><thead><tr><th>時間</th><th>等級</th><th>規則</th><th>商品</th><th>成交編號</th><th>說明</th><th>操作</th></tr></thead><tbody>
            {alerts.map((alert) => <tr key={alert.id}><td>{new Date(alert.occurredAt).toLocaleString("zh-TW")}</td><td><span className={`state-pill severity-${alert.severity.toLowerCase()}`}>{alert.severity}</span></td><td>{alert.ruleCode}</td><td className="symbol">{alert.symbol}</td><td>#{alert.tradeId}</td><td className="description-cell">{alert.description}</td><td>{canAct ? caseAlertIds.has(alert.id) ? <span className="state-pill state-muted">已提報</span> : <button className="button-quiet" disabled={busyId === alert.id} onClick={() => void proposeCase(alert)} type="button">提報案件</button> : "唯讀"}</td></tr>)}
          </tbody></table></div>
        )}
      </section>
      <section className="panel list-panel">
        <div className="panel-heading"><div><p className="eyebrow">INVESTIGATION QUEUE</p><h2>案件清單</h2></div><span className="count-badge">{pending.length} 件待審</span></div>
        {loading ? <p className="empty-row">載入案件…</p> : cases.length === 0 ? <p className="empty-row">尚無案件。</p> : (
          <div className="case-list">{cases.map((item) => {
            const canReview = canAct && item.status === "PROPOSED" && item.proposedBy !== user?.username;
            return <article className="case-card" key={item.id}>
              <div className="case-heading"><div><span className="case-id">CASE / {String(item.id).padStart(5, "0")}</span><span className={`state-pill ${item.status === "APPROVED" ? "state-ok" : item.status === "REJECTED" ? "state-muted" : "state-pending"}`}>{item.status}</span></div><span className="muted-text">關聯警示 #{item.alertId} · {new Date(item.createdAt).toLocaleString("zh-TW")}</span></div>
              <p>提報人：<strong>{item.proposedBy}</strong>{item.reviewedBy && <>　審核人：<strong>{item.reviewedBy}</strong></>}</p>
              {item.reviewNote && <p className="review-note">審核備註：{item.reviewNote}</p>}
              {canReview && <div className="review-controls"><label className="field"><span>審核備註（必填）</span><input maxLength={500} onChange={(event) => setNotes({ ...notes, [item.id]: event.target.value })} placeholder="說明調查結果與決定理由" value={notes[item.id] ?? ""} /></label><div className="button-row"><button className="button-link" disabled={busyId === item.id || !notes[item.id]?.trim()} onClick={() => void reviewCase(item, true)} type="button">核准</button><button className="button-quiet danger-action" disabled={busyId === item.id || !notes[item.id]?.trim()} onClick={() => void reviewCase(item, false)} type="button">駁回</button></div></div>}
              {canAct && item.status === "PROPOSED" && item.proposedBy === user?.username && <p className="form-hint">案件提報者不得審核自己的案件。</p>}
            </article>;
          })}</div>
        )}
      </section>
    </>
  );
}

export default function SurveillancePage() {
  return <RoleGate roles={["SURVEILLANCE", "AUDITOR", "ADMIN"]}><SurveillanceWorkspace /></RoleGate>;
}
