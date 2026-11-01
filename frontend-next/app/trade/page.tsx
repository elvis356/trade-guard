"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Typography from "@mui/material/Typography";
import TextField from "@mui/material/TextField";
import { PageHeading, PanelHeading } from "@/components/page-heading";
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
      <PageHeading
        description="送出與管理自己的模擬限價委託。請留意商品參考價與市場交易規則。"
        eyebrow="ORDER ENTRY · SIMULATED MARKET"
        title="委託交易"
      />
      {(error || notice) && <Alert role={error ? "alert" : "status"} severity={error ? "error" : "success"} sx={{ mb: 2 }}>{error || notice}</Alert>}
      <Box className="workspace-grid trade-layout">
        <Paper className="panel" component="section" elevation={0} variant="outlined">
          <PanelHeading eyebrow="NEW ORDER" title="建立限價委託" trailing={<Chip color="success" label="模擬" size="small" variant="outlined" />} />
          {instruments.length === 0 && !loading ? <Alert severity="warning">市場目前沒有啟用商品，請聯絡管理員新增商品。</Alert> : (
            <Box className="form-grid" component="form" onSubmit={submit}>
              <TextField label="商品" onChange={(event) => setSymbol(event.target.value)} required select value={symbol}>
                {instruments.map((item) => <MenuItem key={item.id} value={item.symbol}>{item.symbol} · {item.name}</MenuItem>)}
              </TextField>
              <TextField label="買賣方向" onChange={(event) => setSide(event.target.value as "B" | "S")} select value={side}>
                <MenuItem value="B">買進</MenuItem><MenuItem value="S">賣出</MenuItem>
              </TextField>
              <TextField label="限價" onChange={(event) => setPrice(event.target.value)} required slotProps={{ htmlInput: { min: 0.01, step: 0.0001 } }} type="number" value={price} />
              <TextField label="數量" onChange={(event) => setQty(event.target.value)} required slotProps={{ htmlInput: { max: 1000000000, min: 1, step: 1 } }} type="number" value={qty} />
              {symbol && <Typography className="form-hint field-span" color="text.secondary" component="p" variant="body2">參考價格：{Number(instruments.find((item) => item.symbol === symbol)?.referencePrice ?? 0).toLocaleString("zh-TW")}。後端會驗證價格區間、升降單位、整股數量與交易時段。</Typography>}
              <Button className="button-submit" disabled={busy || loading || instruments.length === 0} type="submit" variant="contained">{busy ? "送出中…" : "送出委託"}</Button>
            </Box>
          )}
        </Paper>
        <Paper className="panel market-note" component="aside" elevation={0} variant="outlined">
          <Typography className="eyebrow" component="p" variant="overline">ORDER LIFECYCLE</Typography>
          <Typography component="h2" variant="h6">交易規則</Typography>
          <Typography color="text.secondary" component="p" sx={{ mt: 1 }} variant="body2">委託送出後由後端執行價格／時間優先撮合，並可能部分成交。只要尚有未成交數量，即可嘗試取消。</Typography>
          <Alert className="disclaimer" severity="info" variant="outlined">本平台僅供教育模擬，不涉及真實資金或證券。</Alert>
        </Paper>
      </Box>
      <Paper className="panel list-panel" component="section" elevation={0} variant="outlined">
        <PanelHeading eyebrow="YOUR ORDER FLOW" title="我的委託" trailing={<Button onClick={() => void load()} type="button" variant="outlined">重新整理</Button>} />
        {loading ? <Typography className="empty-row" color="text.secondary" component="p">載入委託資料…</Typography> : orders.length === 0 ? <Typography className="empty-row" color="text.secondary" component="p">尚無委託紀錄。</Typography> : (
          <TableContainer className="table-scroll">
            <Table aria-label="我的委託" size="small">
              <TableHead><TableRow><TableCell>委託編號</TableCell><TableCell>商品</TableCell><TableCell>方向</TableCell><TableCell>限價</TableCell><TableCell>委託量</TableCell><TableCell>已成交</TableCell><TableCell>狀態</TableCell><TableCell>時間</TableCell><TableCell>操作</TableCell></TableRow></TableHead>
              <TableBody>{orders.map((order) => {
                const cancellable = order.status === "NEW" || order.status === "PARTIALLY_FILLED";
                return <TableRow key={order.order_id}><TableCell>#{order.order_id}</TableCell><TableCell className="symbol">{order.symbol}</TableCell><TableCell className={order.side === "B" ? "buy-label" : "sell-label"}>{order.side === "B" ? "買進" : "賣出"}</TableCell><TableCell>{Number(order.price).toLocaleString("zh-TW")}</TableCell><TableCell>{order.qty.toLocaleString("zh-TW")}</TableCell><TableCell>{order.filled_qty.toLocaleString("zh-TW")}</TableCell><TableCell><Chip color={order.status === "FILLED" ? "success" : "default"} label={order.status} size="small" variant="outlined" /></TableCell><TableCell>{new Date(order.createdAt).toLocaleString("zh-TW")}</TableCell><TableCell>{cancellable ? <Button color="error" disabled={busy} onClick={() => void cancel(order)} size="small" type="button" variant="outlined">取消</Button> : "—"}</TableCell></TableRow>;
              })}</TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>
    </>
  );
}

export default function TradePage() {
  return <RoleGate roles={["TRADER"]}><TradingWorkspace /></RoleGate>;
}
