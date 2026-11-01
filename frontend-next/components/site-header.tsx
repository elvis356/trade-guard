"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import ShowChartRoundedIcon from "@mui/icons-material/ShowChartRounded";
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
    <AppBar className="site-header" color="inherit" component="header" elevation={0} position="sticky">
      <Toolbar className="site-toolbar" disableGutters>
        <Link aria-label="TradeGuard 首頁" className="brand" href="/">
          <span className="brand-mark"><ShowChartRoundedIcon fontSize="small" /></span>
          <Typography component="span" variant="subtitle1">TradeGuard</Typography>
        </Link>
        <Box aria-label="主要導覽" component="nav" className="primary-nav">
          {links.map((link) => (
            <Button
              aria-current={pathname === link.href ? "page" : undefined}
              component={Link}
              href={link.href}
              key={link.href}
              size="small"
              variant="text"
            >
              {link.label}
            </Button>
          ))}
          {user && ["AUDITOR", "ADMIN"].includes(user.role) && (
            <Button
              aria-current={pathname === "/reports/daily" ? "page" : undefined}
              component={Link}
              href="/reports/daily"
              size="small"
              variant="text"
            >
              日終報表
            </Button>
          )}
        </Box>
        <Stack className="header-account" direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <Chip className="market-badge" color="default" label="模擬市場" size="small" variant="outlined" />
          {loading ? <Typography color="text.secondary" variant="caption">檢查登入中</Typography> : user ? (
            <>
              <Box className="account-name">
                <Typography component="span" variant="body2">{user.username}</Typography>
                <Typography color="text.secondary" component="small" variant="caption">{roleLabel(user.role)}</Typography>
              </Box>
              <Button onClick={logout} size="small" variant="outlined">登出</Button>
            </>
          ) : (
            <Button component={Link} href="/login" size="small" variant="outlined">登入</Button>
          )}
        </Stack>
      </Toolbar>
      {authError && <Box className="header-error" role="alert">{authError}</Box>}
    </AppBar>
  );
}
