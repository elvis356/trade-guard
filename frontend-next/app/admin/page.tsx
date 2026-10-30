"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { RoleGate } from "@/components/role-gate";
import { api, errorMessage, type Instrument, type Page, type Role } from "@/lib/client-api";

type User = {
  id: number;
  username: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
};

const userRoles: Role[] = ["TRADER", "SURVEILLANCE", "AUDITOR", "ADMIN"];

function AdminWorkspace() {
  const [instruments, setInstruments] = useState<Instrument[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [instrument, setInstrument] = useState({ symbol: "", name: "", referencePrice: "", active: true });
  const [editingSymbol, setEditingSymbol] = useState<string | null>(null);
  const [account, setAccount] = useState({ username: "", password: "", role: "TRADER" as Role });

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [instrumentPage, userPage] = await Promise.all([
        api<Page<Instrument>>("/api/market/instruments?page=0&size=100&sort=symbol,asc"),
        api<Page<User>>("/api/admin/users?page=0&size=100&sort=username,asc"),
      ]);
      setInstruments(instrumentPage.content);
      setUsers(userPage.content);
    } catch (loadError) {
      setError(errorMessage(loadError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  async function createInstrument(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      if (editingSymbol) {
        await api<Instrument>(`/api/market/instruments/${encodeURIComponent(editingSymbol)}`, {
          method: "PATCH",
          body: JSON.stringify({
            name: instrument.name.trim(),
            active: instrument.active,
            referencePrice: Number(instrument.referencePrice),
          }),
        });
      } else {
        await api<Instrument>("/api/market/instruments", {
          method: "POST",
          body: JSON.stringify({
            symbol: instrument.symbol.trim().toUpperCase(),
            name: instrument.name.trim(),
            active: instrument.active,
            referencePrice: Number(instrument.referencePrice),
          }),
        });
      }
      setInstrument({ symbol: "", name: "", referencePrice: "", active: true });
      setNotice(editingSymbol ? `${editingSymbol} 已更新。` : "商品已建立，市場總覽將會顯示啟用中的商品。");
      setEditingSymbol(null);
      await load();
    } catch (createError) {
      setError(errorMessage(createError));
    } finally {
      setBusy(false);
    }
  }

  async function toggleInstrument(item: Instrument) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await api<Instrument>(`/api/market/instruments/${encodeURIComponent(item.symbol)}`, {
        method: "PATCH",
        body: JSON.stringify({ active: !item.active }),
      });
      setNotice(`${item.symbol} 已${item.active ? "停用" : "啟用"}。`);
      await load();
    } catch (updateError) {
      setError(errorMessage(updateError));
    } finally {
      setBusy(false);
    }
  }

  function editInstrument(item: Instrument) {
    setEditingSymbol(item.symbol);
    setInstrument({
      symbol: item.symbol,
      name: item.name,
      referencePrice: String(item.referencePrice),
      active: item.active,
    });
    setError("");
    setNotice("");
  }

  function cancelEdit() {
    setEditingSymbol(null);
    setInstrument({ symbol: "", name: "", referencePrice: "", active: true });
  }

  async function deleteInstrument(item: Instrument) {
    if (!window.confirm(`確定刪除 ${item.symbol}（${item.name}）？若已有委託或成交，後端可能會拒絕刪除。`)) return;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await api<Instrument>(`/api/market/instruments/${encodeURIComponent(item.symbol)}`, { method: "DELETE" });
      if (editingSymbol === item.symbol) cancelEdit();
      setNotice(`${item.symbol} 已刪除。`);
      await load();
    } catch (deleteError) {
      setError(errorMessage(deleteError));
    } finally {
      setBusy(false);
    }
  }

  async function createUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await api<User>("/api/admin/users", {
        method: "POST",
        body: JSON.stringify(account),
      });
      setAccount({ username: "", password: "", role: "TRADER" });
      setNotice("帳號已建立。請透過安全管道將初始密碼提供給使用者。");
      await load();
    } catch (createError) {
      setError(errorMessage(createError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">ADMINISTRATION · CONTROL PLANE</p>
        <h1>管理中心</h1>
        <p>管理模擬標的與角色帳號。所有寫入操作仍由後端 ADMIN 權限驗證。</p>
      </section>
      {(error || notice) && <p className={`notice ${error ? "error" : "success"}`} role={error ? "alert" : "status"}>{error || notice}</p>}
      <div className="workspace-grid">
        <section className="panel">
          <div className="panel-heading"><div><p className="eyebrow">MARKET SETUP</p><h2>{editingSymbol ? `編輯商品 · ${editingSymbol}` : "建立商品"}</h2></div>{editingSymbol && <button className="button-quiet" onClick={cancelEdit} type="button">取消編輯</button>}</div>
          <form className="form-grid" onSubmit={createInstrument}>
            <label className="field"><span>商品代碼</span><input autoCapitalize="characters" maxLength={16} onChange={(event) => setInstrument({ ...instrument, symbol: event.target.value.toUpperCase() })} pattern="[A-Z0-9._-]+" placeholder="例如 TG01" readOnly={Boolean(editingSymbol)} required value={instrument.symbol} /></label>
            <label className="field"><span>商品名稱</span><input maxLength={100} onChange={(event) => setInstrument({ ...instrument, name: event.target.value })} required value={instrument.name} /></label>
            <label className="field"><span>參考價格</span><input min="0.01" onChange={(event) => setInstrument({ ...instrument, referencePrice: event.target.value })} required step="0.0001" type="number" value={instrument.referencePrice} /></label>
            <label className="check-field"><input checked={instrument.active} onChange={(event) => setInstrument({ ...instrument, active: event.target.checked })} type="checkbox" /> 建立後立即啟用</label>
            <button className="button-link button-submit" disabled={busy} type="submit">{busy ? "處理中…" : editingSymbol ? "儲存變更" : "新增商品"}</button>
          </form>
        </section>
        <section className="panel">
          <div className="panel-heading"><div><p className="eyebrow">IDENTITY & ACCESS</p><h2>建立角色帳號</h2></div></div>
          <form className="form-grid" onSubmit={createUser}>
            <label className="field"><span>帳號</span><input autoComplete="off" maxLength={50} minLength={3} onChange={(event) => setAccount({ ...account, username: event.target.value })} pattern="[a-zA-Z0-9._-]+" required value={account.username} /></label>
            <label className="field"><span>初始密碼（12–72 字元）</span><input autoComplete="new-password" maxLength={72} minLength={12} onChange={(event) => setAccount({ ...account, password: event.target.value })} required type="password" value={account.password} /></label>
            <label className="field"><span>角色</span><select onChange={(event) => setAccount({ ...account, role: event.target.value as Role })} value={account.role}>{userRoles.map((role) => <option key={role} value={role}>{role}</option>)}</select></label>
            <button className="button-link button-submit" disabled={busy} type="submit">{busy ? "處理中…" : "建立帳號"}</button>
          </form>
        </section>
      </div>
      <section className="panel list-panel">
        <div className="panel-heading"><div><p className="eyebrow">ACTIVE CATALOG</p><h2>商品清單</h2></div><button className="button-quiet" onClick={() => void load()} type="button">重新整理</button></div>
        {loading ? <p className="empty-row">載入商品資料…</p> : instruments.length === 0 ? <p className="empty-row">尚無商品。新增並啟用後，商品行情才會出現在市場總覽。</p> : (
          <div className="table-scroll"><table><thead><tr><th>代碼</th><th>名稱</th><th>參考價</th><th>狀態</th><th>操作</th></tr></thead><tbody>
            {instruments.map((item) => <tr key={item.id}><td className="symbol">{item.symbol}</td><td>{item.name}</td><td>{Number(item.referencePrice).toLocaleString("zh-TW")}</td><td><span className={`state-pill ${item.active ? "state-ok" : "state-muted"}`}>{item.active ? "啟用" : "停用"}</span></td><td><div className="button-row compact-actions"><button className="button-quiet" disabled={busy} onClick={() => editInstrument(item)} type="button">編輯</button><button className="button-quiet" disabled={busy} onClick={() => void toggleInstrument(item)} type="button">{item.active ? "停用" : "啟用"}</button><button className="button-quiet danger-action" disabled={busy} onClick={() => void deleteInstrument(item)} type="button">刪除</button></div></td></tr>)}
          </tbody></table></div>
        )}
      </section>
      <section className="panel list-panel">
        <div className="panel-heading"><div><p className="eyebrow">TEAM DIRECTORY</p><h2>角色帳號</h2></div><span className="count-badge">{users.length} 位</span></div>
        {loading ? <p className="empty-row">載入帳號資料…</p> : users.length === 0 ? <p className="empty-row">尚無帳號。</p> : (
          <div className="table-scroll"><table><thead><tr><th>帳號</th><th>角色</th><th>狀態</th><th>建立時間</th></tr></thead><tbody>
            {users.map((item) => <tr key={item.id}><td>{item.username}</td><td><span className="state-pill">{item.role}</span></td><td>{item.enabled ? "啟用" : "停用"}</td><td>{new Date(item.createdAt).toLocaleString("zh-TW")}</td></tr>)}
          </tbody></table></div>
        )}
      </section>
    </>
  );
}

export default function AdminPage() {
  return <RoleGate roles={["ADMIN"]}><AdminWorkspace /></RoleGate>;
}
