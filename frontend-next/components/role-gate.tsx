"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { roleLabel, type Role } from "@/lib/client-api";
import { useAuth } from "@/components/auth-provider";

export function RoleGate({
  roles,
  children,
}: {
  roles: Role[];
  children: ReactNode;
}) {
  const { user, loading } = useAuth();

  if (loading) {
    return <p className="notice">正在確認登入狀態…</p>;
  }
  if (!user) {
    return (
      <section className="access-panel">
        <p className="eyebrow">AUTHENTICATION REQUIRED</p>
        <h2>請先登入</h2>
        <p>登入後才能使用此工作區。你的登入 Cookie 僅由瀏覽器安全保存。</p>
        <Link className="button-link" href="/login">前往登入</Link>
      </section>
    );
  }
  if (!roles.includes(user.role)) {
    return (
      <section className="access-panel">
        <p className="eyebrow">ROLE RESTRICTED</p>
        <h2>目前帳號沒有此功能的權限</h2>
        <p>目前角色：{roleLabel(user.role)}。請使用具備授權的帳號，或返回市場總覽。</p>
        <Link className="button-link" href="/">返回市場</Link>
      </section>
    );
  }
  return children;
}
