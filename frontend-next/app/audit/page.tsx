"use client";

import { useState } from "react";
import Link from "next/link";
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
      <section className="hero">
        <p className="eyebrow">AUDIT · INTEGRITY & RECONCILIATION</p>
        <h1>稽核工作台</h1>
        <p>即時檢查稽核雜湊鏈及帳本一致性，並查看具權限保護的日終成交報告。</p>
      </section>
      {error && <p className="notice error" role="alert">{error}</p>}
      <div className="workspace-grid">
        <section className="panel audit-action">
          <div className="panel-heading"><div><p className="eyebrow">HASH-CHAIN INTEGRITY</p><h2>驗證稽核紀錄</h2></div><span className="state-pill">唯讀檢查</span></div>
          <p>逐筆驗證稽核紀錄雜湊、前後連結及目前鏈頭。</p>
          <button className="button-link" disabled={busy !== null} onClick={() => void run("verify")} type="button">{busy === "verify" ? "驗證中…" : "執行鏈驗證"}</button>
          {verification && <div className={`result-card ${verification.valid ? "result-good" : "result-bad"}`} role="status">
            <strong>{verification.valid ? "雜湊鏈有效" : "發現鏈完整性問題"}</strong>
            <p>檢查紀錄：{verification.entriesChecked.toLocaleString("zh-TW")} 筆</p>
            {verification.brokenEntryId !== null && <p>異常紀錄 ID：{verification.brokenEntryId}</p>}
            {verification.reason && <p>{verification.reason}</p>}
            <small>驗證時間：{new Date(verification.verifiedAt).toLocaleString("zh-TW")}</small>
          </div>}
        </section>
        <section className="panel audit-action">
          <div className="panel-heading"><div><p className="eyebrow">LEDGER RECONCILIATION</p><h2>帳本對帳</h2></div><span className="state-pill">三方核對</span></div>
          <p>交叉核對委託、成交與委託事件，找出帳本差異及序號問題。</p>
          <button className="button-link" disabled={busy !== null} onClick={() => void run("reconcile")} type="button">{busy === "reconcile" ? "對帳中…" : "執行對帳"}</button>
          {reconciliation && <div className={`result-card ${reconciliation.balanced ? "result-good" : "result-bad"}`} role="status">
            <strong>{reconciliation.balanced ? "帳本一致" : `發現 ${reconciliation.issues.length} 項差異`}</strong>
            <div className="metric-row"><span>委託 {reconciliation.ordersChecked}</span><span>成交 {reconciliation.tradesChecked}</span><span>事件 {reconciliation.eventsChecked}</span></div>
            {reconciliation.issues.length > 0 && <ul className="issue-list">{reconciliation.issues.map((issue, index) => <li key={`${issue.code}-${issue.symbol}-${index}`}><strong>{issue.code}{issue.symbol && ` · ${issue.symbol}`}</strong><span>{issue.message}</span></li>)}</ul>}
            <small>對帳時間：{new Date(reconciliation.reconciledAt).toLocaleString("zh-TW")}</small>
          </div>}
        </section>
      </div>
      <section className="callout report-callout">
        <div><p className="eyebrow">END-OF-DAY · XML</p><h2>每日成交報告</h2><p>選擇業務日期，在報表頁預覽並下載後端產生的 XML。</p></div>
        <Link className="button-link" href="/reports/daily">開啟日終報表</Link>
      </section>
    </>
  );
}

export default function AuditPage() {
  return <RoleGate roles={["AUDITOR", "ADMIN"]}><AuditWorkspace /></RoleGate>;
}
