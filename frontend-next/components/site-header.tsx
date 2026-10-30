"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { api, roleLabel } from "@/lib/client-api";
import { useAuth } from "@/components/auth-provider";

const commonLinks = [
  { href: "/", label: "市場總覽" },
  { href: "/rules/matching", label: "撮合規則" },
];

export function SiteHeader() {
  const { user, loading, authError, clear, reportError } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  const links = [
    ...commonLinks,
    ...(user?.role === "TRADER" ? [{ href: "/trade", label: "委託交易" }] : []),
    ...(user?.role === "ADMIN" ? [{ href: "/admin", label: "管理中心" }] : []),
    ...(user && ["SURVEILLANCE", "AUDITOR", "ADMIN"].includes(user.role)
      ? [{ href: "/surveillance", label: "監控案件" }] : []),
    ...(user && ["AUDITOR", "ADMIN"].includes(user.role)
      ? [{ href: "/audit", label: "稽核報表" }] : []),
  ];

  async function logout() {
    try {
      await api<void>("/api/auth/logout", { method: "POST" });
      clear();
      router.push("/");
      router.refresh();
    } catch {
      reportError("登出失敗，請檢查連線後重試。");
    }
  }

  return (
    <header className="site-header">
      <Link aria-label="TradeGuard 首頁" className="brand" href="/">
        <span className="brand-mark">TG</span>
        <span>TradeGuard</span>
      </Link>
      <nav aria-label="主要導覽">
        {links.map((link) => (
          <Link aria-current={pathname === link.href ? "page" : undefined} href={link.href} key={link.href}>
            {link.label}
          </Link>
        ))}
        {user && ["AUDITOR", "ADMIN"].includes(user.role) && (
          <Link aria-current={pathname === "/reports/daily" ? "page" : undefined} href="/reports/daily">
            日終報表
          </Link>
        )}
      </nav>
      <div className="header-account">
        <span className="market-badge"><span /> 模擬市場</span>
        {loading ? <span className="account-loading">檢查登入中</span> : user ? (
          <>
            <span className="account-name">{user.username}<small>{roleLabel(user.role)}</small></span>
            <button className="button-quiet" onClick={logout} type="button">登出</button>
          </>
        ) : (
          <Link className="button-quiet" href="/login">登入</Link>
        )}
      </div>
      {authError && <p className="header-error" role="alert">{authError}</p>}
    </header>
  );
}
