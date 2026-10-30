"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { RoleGate } from "@/components/role-gate";
import { api, errorMessage, type Instrument, type Order, type Page } from "@/lib/client-api";

function TradingWorkspace() {
  const [instruments, setInstruments] = useState<Instrument[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [symbol, setSymbol] = useState("");
  const [side, setSide] = useState<"B" | "S">("B");
  const [price, setPrice] = useState("");
  const [qty, setQty] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [instrumentPage, orderPage] = await Promise.all([
        api<Page<Instrument>>("/api/market/instruments?page=0&size=100&sort=symbol,asc"),
        api<Page<Order>>("/api/orders/mine?page=0&size=100&sort=id,desc"),
      ]);
      const activeInstruments = instrumentPage.content.filter((item) => item.active);
      setInstruments(activeInstruments);
      setOrders(orderPage.content);
      setSymbol((current) => activeInstruments.some((item) => item.symbol === current)
        ? current : activeInstruments[0]?.symbol ?? "");
    } catch (loadError) {
      setError(errorMessage(loadError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const order = await api<Order>("/api/orders", {
        method: "POST",
        body: JSON.stringify({
          client_order_id: crypto.randomUUID(),
          symbol,
          side,
          price: Number(price),
          qty: Number(qty),
        }),
      });
      setNotice(`委託已送出：${order.symbol} ${order.side === "B" ? "買進" : "賣出"}，委託編號 ${order.order_id}。`);
      setPrice("");
      setQty("");
      await load();
    } catch (submitError) {
      setError(errorMessage(submitError));
    } finally {
      setBusy(false);
    }
  }

  async function cancel(order: Order) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await api<Order>(`/api/orders/${order.order_id}`, { method: "DELETE" });
      setNotice(`委託 ${order.order_id} 已取消。`);
      await load();
    } catch (cancelError) {
      setError(errorMessage(cancelError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <section className="hero">
        <p className="eyebrow">ORDER ENTRY · SIMULATED MARKET</p>
        <h1>委託交易</h1>
        <p>送出與管理自己的模擬限價委託。請留意商品參考價與市場交易規則。</p>
      </section>
      {(error || notice) && <p className={`notice ${error ? "error" : "success"}`} role={error ? "alert" : "status"}>{error || notice}</p>}
      <div className="workspace-grid trade-layout">
        <section className="panel">
          <div className="panel-heading"><div><p className="eyebrow">NEW ORDER</p><h2>建立限價委託</h2></div><span className="state-pill">模擬</span></div>
          {instruments.length === 0 && !loading ? <p className="notice stale">市場目前沒有啟用商品，請聯絡管理員新增商品。</p> : (
            <form className="form-grid" onSubmit={submit}>
              <label className="field"><span>商品</span><select onChange={(event) => setSymbol(event.target.value)} required value={symbol}>{instruments.map((item) => <option key={item.id} value={item.symbol}>{item.symbol} · {item.name}</option>)}</select></label>
              <label className="field"><span>買賣方向</span><select onChange={(event) => setSide(event.target.value as "B" | "S")} value={side}><option value="B">買進</option><option value="S">賣出</option></select></label>
              <label className="field"><span>限價</span><input min="0.01" onChange={(event) => setPrice(event.target.value)} required step="0.0001" type="number" value={price} /></label>
              <label className="field"><span>數量</span><input max="1000000000" min="1" onChange={(event) => setQty(event.target.value)} required step="1" type="number" value={qty} /></label>
              {symbol && <p className="form-hint field-span">參考價格：{Number(instruments.find((item) => item.symbol === symbol)?.referencePrice ?? 0).toLocaleString("zh-TW")}。後端會驗證價格區間、升降單位、整股數量與交易時段。</p>}
              <button className="button-link button-submit" disabled={busy || loading || instruments.length === 0} type="submit">{busy ? "送出中…" : "送出委託"}</button>
            </form>
          )}
        </section>
        <aside className="panel market-note">
          <p className="eyebrow">ORDER LIFECYCLE</p>
          <h2>交易規則</h2>
          <p>委託送出後由後端執行價格／時間優先撮合，並可能部分成交。只要尚有未成交數量，即可嘗試取消。</p>
          <p className="disclaimer">本平台僅供教育模擬，不涉及真實資金或證券。</p>
        </aside>
      </div>
      <section className="panel list-panel">
        <div className="panel-heading"><div><p className="eyebrow">YOUR ORDER FLOW</p><h2>我的委託</h2></div><button className="button-quiet" onClick={() => void load()} type="button">重新整理</button></div>
        {loading ? <p className="empty-row">載入委託資料…</p> : orders.length === 0 ? <p className="empty-row">尚無委託紀錄。</p> : (
          <div className="table-scroll"><table><thead><tr><th>委託編號</th><th>商品</th><th>方向</th><th>限價</th><th>委託量</th><th>已成交</th><th>狀態</th><th>時間</th><th>操作</th></tr></thead><tbody>
            {orders.map((order) => {
              const cancellable = order.status === "NEW" || order.status === "PARTIALLY_FILLED";
              return <tr key={order.order_id}><td>#{order.order_id}</td><td className="symbol">{order.symbol}</td><td className={order.side === "B" ? "buy-label" : "sell-label"}>{order.side === "B" ? "買進" : "賣出"}</td><td>{Number(order.price).toLocaleString("zh-TW")}</td><td>{order.qty.toLocaleString("zh-TW")}</td><td>{order.filled_qty.toLocaleString("zh-TW")}</td><td><span className={`state-pill ${order.status === "FILLED" ? "state-ok" : ""}`}>{order.status}</span></td><td>{new Date(order.createdAt).toLocaleString("zh-TW")}</td><td>{cancellable ? <button className="button-quiet danger-action" disabled={busy} onClick={() => void cancel(order)} type="button">取消</button> : "—"}</td></tr>;
            })}
          </tbody></table></div>
        )}
      </section>
    </>
  );
}

export default function TradePage() {
  return <RoleGate roles={["TRADER"]}><TradingWorkspace /></RoleGate>;
}
