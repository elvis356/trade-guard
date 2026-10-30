"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
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
    <div className="login-layout">
      <section className="login-intro">
        <p className="eyebrow">TRADEGUARD · OPEN ACCOUNT</p>
        <h1>建立你的<br />模擬交易席位。</h1>
        <p>新帳號僅開放交易員角色。監控、稽核與管理權限仍由管理員建立，註冊不會授予這些權限。</p>
      </section>
      {registeredUsername ? (
        <section aria-live="polite" className="panel login-panel">
          <p className="eyebrow">ACCOUNT READY</p>
          <h2>帳號已建立</h2>
          <p className="notice success">交易員帳號 <strong>{registeredUsername}</strong> 已建立，現在可以登入。</p>
          <Link className="button-link button-submit register-login-link" href="/login">前往登入</Link>
        </section>
      ) : (
        <form className="panel login-panel" onSubmit={submit}>
          <p className="eyebrow">NEW TRADER</p>
          <h2>建立帳號</h2>
          <label className="field">
            <span>帳號</span>
            <input
              autoCapitalize="none"
              autoComplete="username"
              maxLength={50}
              minLength={3}
              onChange={(event) => setUsername(event.target.value)}
              pattern="[a-zA-Z0-9._-]{3,50}"
              required
              spellCheck={false}
              title="3 至 50 個英數字、點、底線或連字號"
              value={username}
            />
          </label>
          <label className="field">
            <span>密碼</span>
            <input
              autoComplete="new-password"
              maxLength={72}
              minLength={12}
              onChange={(event) => setPassword(event.target.value)}
              required
              type="password"
              value={password}
            />
          </label>
          <label className="field">
            <span>確認密碼</span>
            <input
              autoComplete="new-password"
              maxLength={72}
              onChange={(event) => setConfirmPassword(event.target.value)}
              required
              type="password"
              value={confirmPassword}
            />
          </label>
          {error && <p className="notice error" role="alert">{error}</p>}
          <button className="button-link button-submit" disabled={busy} type="submit">
            {busy ? "建立中…" : "建立交易員帳號"}
          </button>
          <p className="form-hint">密碼至少 12 個字元。已有帳號？<Link className="form-link" href="/login">返回登入</Link></p>
        </form>
      )}
    </div>
  );
}
