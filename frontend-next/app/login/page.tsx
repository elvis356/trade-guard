"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
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
    <div className="login-layout">
      <section className="login-intro">
        <p className="eyebrow">TRADEGUARD · SECURE ACCESS</p>
        <h1>市場工作台，<br />各司其職。</h1>
        <p>以你的角色進入模擬交易、監控或稽核工作區。登入狀態由安全 Cookie 管理，不會暴露在瀏覽器程式碼中。</p>
      </section>
      <form className="panel login-panel" onSubmit={submit}>
        <p className="eyebrow">ACCOUNT</p>
        <h2>登入 TradeGuard</h2>
        <label className="field">
          <span>帳號</span>
          <input autoComplete="username" onChange={(event) => setUsername(event.target.value)} required value={username} />
        </label>
        <label className="field">
          <span>密碼</span>
          <input autoComplete="current-password" onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
        </label>
        {error && <p className="notice error" role="alert">{error}</p>}
        <button className="button-link button-submit" disabled={busy} type="submit">
          {busy ? "登入中…" : "登入"}
        </button>
        <p className="form-hint">還沒有帳號？<Link className="form-link" href="/register">建立交易員帳號</Link></p>
      </form>
    </div>
  );
}
