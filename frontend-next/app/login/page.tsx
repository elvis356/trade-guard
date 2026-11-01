"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useAuth } from "@/components/auth-provider";
import { api, errorMessage, type CurrentUser } from "@/lib/client-api";

export default function LoginPage() {
  const { refresh } = useAuth();
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      await api<{ user: CurrentUser; expiresAt: string }>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      });
      const user = await refresh();
      const destination = user?.role === "ADMIN" ? "/admin"
        : user?.role === "TRADER" ? "/trade"
          : user?.role === "SURVEILLANCE" ? "/surveillance"
            : user?.role === "AUDITOR" ? "/audit" : "/";
      router.replace(destination);
      router.refresh();
    } catch (submitError) {
      setError(errorMessage(submitError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Box className="login-layout">
      <Box className="login-intro" component="section">
        <Typography className="eyebrow" component="p" variant="overline">TRADEGUARD · SECURE ACCESS</Typography>
        <Typography component="h1" variant="h1">市場工作台，<br />各司其職。</Typography>
        <Typography color="text.secondary" component="p" sx={{ mt: 2 }} variant="body1">以你的角色進入模擬交易、監控或稽核工作區。登入狀態由安全 Cookie 管理，不會暴露在瀏覽器程式碼中。</Typography>
      </Box>
      <Paper className="panel login-panel" component="form" elevation={0} onSubmit={submit}>
        <Typography className="eyebrow" component="p" variant="overline">ACCOUNT</Typography>
        <Typography component="h2" sx={{ mb: 3 }} variant="h5">登入 TradeGuard</Typography>
        <Stack spacing={2}>
          <TextField autoComplete="username" fullWidth label="帳號" onChange={(event) => setUsername(event.target.value)} required value={username} />
          <TextField autoComplete="current-password" fullWidth label="密碼" onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
        </Stack>
        {error && <Alert role="alert" severity="error" sx={{ mt: 2 }}>{error}</Alert>}
        <Button className="button-submit" disabled={busy} fullWidth sx={{ mt: 2 }} type="submit" variant="contained">
          {busy ? "登入中…" : "登入"}
        </Button>
        <Typography className="form-hint" component="p" sx={{ mt: 2 }} variant="body2">還沒有帳號？<Link className="form-link" href="/register">建立交易員帳號</Link></Typography>
      </Paper>
    </Box>
  );
}
