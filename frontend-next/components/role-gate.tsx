"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
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
    return <Alert severity="info">正在確認登入狀態…</Alert>;
  }
  if (!user) {
    return (
      <Paper className="access-panel" component="section" elevation={0}>
        <Typography className="eyebrow" component="p" variant="overline">AUTHENTICATION REQUIRED</Typography>
        <Typography component="h2" variant="h5">請先登入</Typography>
        <Typography color="text.secondary" sx={{ my: 2 }}>登入後才能使用此工作區。你的登入 Cookie 僅由瀏覽器安全保存。</Typography>
        <Button component={Link} href="/login" variant="contained">前往登入</Button>
      </Paper>
    );
  }
  if (!roles.includes(user.role)) {
    return (
      <Paper className="access-panel" component="section" elevation={0}>
        <Typography className="eyebrow" component="p" variant="overline">ROLE RESTRICTED</Typography>
        <Typography component="h2" variant="h5">目前帳號沒有此功能的權限</Typography>
        <Typography color="text.secondary" sx={{ my: 2 }}>目前角色：{roleLabel(user.role)}。請使用具備授權的帳號，或返回市場總覽。</Typography>
        <Stack direction="row"><Button component={Link} href="/" variant="contained">返回市場</Button></Stack>
      </Paper>
    );
  }
  return children;
}
