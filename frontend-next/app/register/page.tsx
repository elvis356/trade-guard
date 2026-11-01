"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { api, errorMessage, type CurrentUser } from "@/lib/client-api";

export default function RegisterPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [registeredUsername, setRegisteredUsername] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    if (password !== confirmPassword) {
      setError("兩次輸入的密碼不一致。");
      return;
    }

    setBusy(true);
    try {
      const user = await api<Pick<CurrentUser, "username" | "role">>("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      });
      setRegisteredUsername(user.username);
    } catch (submitError) {
      setError(errorMessage(submitError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Box className="login-layout">
      <Box className="login-intro" component="section">
        <Typography className="eyebrow" component="p" variant="overline">TRADEGUARD · OPEN ACCOUNT</Typography>
        <Typography component="h1" variant="h1">建立你的<br />模擬交易席位。</Typography>
        <Typography color="text.secondary" component="p" sx={{ mt: 2 }} variant="body1">新帳號僅開放交易員角色。監控、稽核與管理權限仍由管理員建立，註冊不會授予這些權限。</Typography>
      </Box>
      {registeredUsername ? (
        <Paper aria-live="polite" className="panel login-panel" component="section" elevation={0}>
          <Typography className="eyebrow" component="p" variant="overline">ACCOUNT READY</Typography>
          <Typography component="h2" sx={{ mb: 2 }} variant="h5">帳號已建立</Typography>
          <Alert severity="success">交易員帳號 <strong>{registeredUsername}</strong> 已建立，現在可以登入。</Alert>
          <Button className="register-login-link" component={Link} href="/login" variant="contained">前往登入</Button>
        </Paper>
      ) : (
        <Paper className="panel login-panel" component="form" elevation={0} onSubmit={submit}>
          <Typography className="eyebrow" component="p" variant="overline">NEW TRADER</Typography>
          <Typography component="h2" sx={{ mb: 3 }} variant="h5">建立帳號</Typography>
          <Stack spacing={2}>
            <TextField
              autoCapitalize="none"
              autoComplete="username"
              fullWidth
              label="帳號"
              onChange={(event) => setUsername(event.target.value)}
              required
              slotProps={{ htmlInput: { maxLength: 50, minLength: 3, pattern: "[a-zA-Z0-9._-]{3,50}", spellCheck: false, title: "3 至 50 個英數字、點、底線或連字號" } }}
              value={username}
            />
            <TextField
              autoComplete="new-password"
              fullWidth
              label="密碼"
              onChange={(event) => setPassword(event.target.value)}
              required
              slotProps={{ htmlInput: { maxLength: 72, minLength: 12 } }}
              type="password"
              value={password}
            />
            <TextField
              autoComplete="new-password"
              fullWidth
              label="確認密碼"
              onChange={(event) => setConfirmPassword(event.target.value)}
              required
              slotProps={{ htmlInput: { maxLength: 72 } }}
              type="password"
              value={confirmPassword}
            />
          </Stack>
          {error && <Alert role="alert" severity="error" sx={{ mt: 2 }}>{error}</Alert>}
          <Button className="button-submit" disabled={busy} fullWidth sx={{ mt: 2 }} type="submit" variant="contained">
            {busy ? "建立中…" : "建立交易員帳號"}
          </Button>
          <Typography className="form-hint" component="p" sx={{ mt: 2 }} variant="body2">密碼至少 12 個字元。已有帳號？<Link className="form-link" href="/login">返回登入</Link></Typography>
        </Paper>
      )}
    </Box>
  );
}
