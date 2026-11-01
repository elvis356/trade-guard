"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import Chip from "@mui/material/Chip";
import FormControlLabel from "@mui/material/FormControlLabel";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
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
      <PageHeading
        description="管理模擬標的與角色帳號。所有寫入操作仍由後端 ADMIN 權限驗證。"
        eyebrow="ADMINISTRATION · CONTROL PLANE"
        title="管理中心"
      />
      {(error || notice) && <Alert role={error ? "alert" : "status"} severity={error ? "error" : "success"} sx={{ mb: 2 }}>{error || notice}</Alert>}
      <Box className="workspace-grid">
        <Paper className="panel" component="section" elevation={0} variant="outlined">
          <PanelHeading
            eyebrow="MARKET SETUP"
            title={editingSymbol ? `編輯商品 · ${editingSymbol}` : "建立商品"}
            trailing={editingSymbol && <Button onClick={cancelEdit} type="button" variant="outlined">取消編輯</Button>}
          />
          <Box className="form-grid" component="form" onSubmit={createInstrument}>
            <TextField label="商品代碼" onChange={(event) => setInstrument({ ...instrument, symbol: event.target.value.toUpperCase() })} placeholder="例如 TG01" required slotProps={{ htmlInput: { autoCapitalize: "characters", maxLength: 16, pattern: "[A-Z0-9._-]+", readOnly: Boolean(editingSymbol) } }} value={instrument.symbol} />
            <TextField label="商品名稱" onChange={(event) => setInstrument({ ...instrument, name: event.target.value })} required slotProps={{ htmlInput: { maxLength: 100 } }} value={instrument.name} />
            <TextField label="參考價格" onChange={(event) => setInstrument({ ...instrument, referencePrice: event.target.value })} required slotProps={{ htmlInput: { min: 0.01, step: 0.0001 } }} type="number" value={instrument.referencePrice} />
            <FormControlLabel control={<Checkbox checked={instrument.active} onChange={(event) => setInstrument({ ...instrument, active: event.target.checked })} />} label="建立後立即啟用" />
            <Button className="button-submit" disabled={busy} type="submit" variant="contained">{busy ? "處理中…" : editingSymbol ? "儲存變更" : "新增商品"}</Button>
          </Box>
        </Paper>
        <Paper className="panel" component="section" elevation={0} variant="outlined">
          <PanelHeading eyebrow="IDENTITY & ACCESS" title="建立角色帳號" />
          <Box className="form-grid" component="form" onSubmit={createUser}>
            <TextField autoComplete="off" label="帳號" onChange={(event) => setAccount({ ...account, username: event.target.value })} required slotProps={{ htmlInput: { maxLength: 50, minLength: 3, pattern: "[a-zA-Z0-9._-]+" } }} value={account.username} />
            <TextField autoComplete="new-password" label="初始密碼（12–72 字元）" onChange={(event) => setAccount({ ...account, password: event.target.value })} required slotProps={{ htmlInput: { maxLength: 72, minLength: 12 } }} type="password" value={account.password} />
            <TextField label="角色" onChange={(event) => setAccount({ ...account, role: event.target.value as Role })} select value={account.role}>{userRoles.map((role) => <MenuItem key={role} value={role}>{role}</MenuItem>)}</TextField>
            <Button className="button-submit" disabled={busy} type="submit" variant="contained">{busy ? "處理中…" : "建立帳號"}</Button>
          </Box>
        </Paper>
      </Box>
      <Paper className="panel list-panel" component="section" elevation={0} variant="outlined">
        <PanelHeading eyebrow="ACTIVE CATALOG" title="商品清單" trailing={<Button onClick={() => void load()} type="button" variant="outlined">重新整理</Button>} />
        {loading ? <Typography className="empty-row" color="text.secondary" component="p">載入商品資料…</Typography> : instruments.length === 0 ? <Typography className="empty-row" color="text.secondary" component="p">尚無商品。新增並啟用後，商品行情才會出現在市場總覽。</Typography> : (
          <TableContainer className="table-scroll">
            <Table aria-label="商品清單" size="small">
              <TableHead><TableRow><TableCell>代碼</TableCell><TableCell>名稱</TableCell><TableCell>參考價</TableCell><TableCell>狀態</TableCell><TableCell>操作</TableCell></TableRow></TableHead>
              <TableBody>
                {instruments.map((item) => <TableRow key={item.id}><TableCell className="symbol">{item.symbol}</TableCell><TableCell>{item.name}</TableCell><TableCell>{Number(item.referencePrice).toLocaleString("zh-TW")}</TableCell><TableCell><Chip color={item.active ? "success" : "default"} label={item.active ? "啟用" : "停用"} size="small" variant="outlined" /></TableCell><TableCell><Box className="button-row compact-actions"><Button disabled={busy} onClick={() => editInstrument(item)} size="small" type="button" variant="outlined">編輯</Button><Button disabled={busy} onClick={() => void toggleInstrument(item)} size="small" type="button" variant="outlined">{item.active ? "停用" : "啟用"}</Button><Button color="error" disabled={busy} onClick={() => void deleteInstrument(item)} size="small" type="button" variant="outlined">刪除</Button></Box></TableCell></TableRow>)}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>
      <Paper className="panel list-panel" component="section" elevation={0} variant="outlined">
        <PanelHeading eyebrow="TEAM DIRECTORY" title="角色帳號" trailing={<Chip label={`${users.length} 位`} size="small" />} />
        {loading ? <Typography className="empty-row" color="text.secondary" component="p">載入帳號資料…</Typography> : users.length === 0 ? <Typography className="empty-row" color="text.secondary" component="p">尚無帳號。</Typography> : (
          <TableContainer className="table-scroll">
            <Table aria-label="角色帳號" size="small">
              <TableHead><TableRow><TableCell>帳號</TableCell><TableCell>角色</TableCell><TableCell>狀態</TableCell><TableCell>建立時間</TableCell></TableRow></TableHead>
              <TableBody>
                {users.map((item) => <TableRow key={item.id}><TableCell>{item.username}</TableCell><TableCell><Chip label={item.role} size="small" variant="outlined" /></TableCell><TableCell>{item.enabled ? "啟用" : "停用"}</TableCell><TableCell>{new Date(item.createdAt).toLocaleString("zh-TW")}</TableCell></TableRow>)}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>
    </>
  );
}

export default function AdminPage() {
  return <RoleGate roles={["ADMIN"]}><AdminWorkspace /></RoleGate>;
}
