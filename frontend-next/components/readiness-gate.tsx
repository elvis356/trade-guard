"use client";

import { useEffect, useState, type ReactNode } from "react";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

type ServiceState = "UP" | "DOWN" | "WAITING";
type ServiceName = "api" | "database" | "redis";
type ReadinessResponse = {
  status: string;
  checks: Partial<Record<ServiceName, string>>;
};

const services: { name: ServiceName; label: string; detail: string }[] = [
  { name: "api", label: "TradeGuard API", detail: "後端服務" },
  { name: "database", label: "PostgreSQL", detail: "交易資料庫" },
  { name: "redis", label: "Redis", detail: "登入與快取服務" },
];

function serviceState(value: string | undefined): ServiceState {
  if (value === "UP" || value === "DOWN") return value;
  return "WAITING";
}

function stateLabel(state: ServiceState): string {
  if (state === "UP") return "已就緒";
  if (state === "DOWN") return "等待服務";
  return "檢查中";
}

export function ReadinessGate({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [checks, setChecks] = useState<Record<ServiceName, ServiceState>>({
    api: "WAITING",
    database: "WAITING",
    redis: "WAITING",
  });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let cancelled = false;
    let retryTimer: number | undefined;
    const controller = new AbortController();
    const requestTimer = window.setTimeout(() => controller.abort(), 10000);

    async function checkReadiness() {
      let allReady = false;
      try {
        const response = await fetch("/api/health/readiness", {
          headers: { Accept: "application/json" },
          cache: "no-store",
          signal: controller.signal,
        });
        const result = await response.json() as ReadinessResponse;
        if (cancelled) return;

        const nextChecks = {
          api: serviceState(result.checks?.api),
          database: serviceState(result.checks?.database),
          redis: serviceState(result.checks?.redis),
        };
        setChecks(nextChecks);
        allReady = response.ok
          && result.status === "UP"
          && services.every(({ name }) => nextChecks[name] === "UP");
        if (allReady) setReady(true);
      } catch {
        if (!cancelled) {
          setChecks({ api: "DOWN", database: "WAITING", redis: "WAITING" });
        }
      } finally {
        window.clearTimeout(requestTimer);
        if (!cancelled && !allReady) {
          retryTimer = window.setTimeout(() => {
            setAttempt((current) => current + 1);
          }, 3000);
        }
      }
    }

    void checkReadiness();
    return () => {
      cancelled = true;
      controller.abort();
      window.clearTimeout(requestTimer);
      window.clearTimeout(retryTimer);
    };
  }, [attempt]);

  if (ready) return children;

  return (
    <Box
      component="main"
      sx={{
        alignItems: "center",
        background:
          "radial-gradient(ellipse at 82% 10%, rgba(196,220,205,.48), transparent 34rem), linear-gradient(180deg,#fafbf7,#f1f4ef)",
        display: "grid",
        minHeight: "100svh",
        px: 2.5,
        py: 6,
      }}
    >
      <Paper
        elevation={0}
        sx={{
          border: "1px solid #dce5df",
          borderRadius: 2,
          borderTop: "4px solid #104c36",
          justifySelf: "center",
          maxWidth: 620,
          overflow: "hidden",
          p: { xs: 3, sm: 5 },
          width: "100%",
        }}
      >
        <Stack spacing={3.5}>
          <Box>
            <Typography
              color="success.main"
              sx={{ fontFamily: "Consolas, monospace", fontSize: 11, fontWeight: 700, letterSpacing: ".2em" }}
              variant="overline"
            >
              TRADEGUARD · SYSTEM CHECK
            </Typography>
            <Typography component="h1" sx={{ fontSize: { xs: 30, sm: 38 }, fontWeight: 900, mt: 1 }}>
              正在確認服務狀態
            </Typography>
            <Typography color="text.secondary" sx={{ lineHeight: 1.8, mt: 1.25 }}>
              首次連線可能需要喚醒後端。待 API、資料庫與 Redis 都正常後，將自動載入您要前往的頁面。
            </Typography>
          </Box>

          <Stack
            aria-live="polite"
            component="ul"
            spacing={0}
            sx={{ borderBottom: "1px solid #e3e3e3", borderTop: "1px solid #e3e3e3", listStyle: "none", m: 0, p: 0 }}
          >
            {services.map(({ name, label, detail }) => {
              const state = checks[name];
              const color = state === "UP" ? "#1f7a4d" : state === "DOWN" ? "#8a6100" : "#858585";
              return (
                <Box
                  component="li"
                  key={name}
                  sx={{
                    alignItems: "center",
                    borderBottom: "1px solid #edf0ed",
                    display: "flex",
                    gap: 1.75,
                    minHeight: 68,
                    "&:last-child": { borderBottom: 0 },
                  }}
                >
                  <Box sx={{ bgcolor: color, borderRadius: "50%", flex: "0 0 8px", height: 8 }} />
                  <Box sx={{ flex: 1 }}>
                    <Typography sx={{ fontSize: 14, fontWeight: 750 }}>{label}</Typography>
                    <Typography color="text.secondary" sx={{ fontSize: 12 }}>{detail}</Typography>
                  </Box>
                  <Typography sx={{ color, fontFamily: "Consolas, monospace", fontSize: 11, fontWeight: 700, letterSpacing: ".08em" }}>
                    {stateLabel(state)}
                  </Typography>
                </Box>
              );
            })}
          </Stack>

          <Stack
            spacing={2}
            sx={{
              alignItems: { xs: "stretch", sm: "center" },
              flexDirection: { xs: "column", sm: "row" },
              justifyContent: "space-between",
            }}
          >
            <Stack direction="row" spacing={1.25} sx={{ alignItems: "center" }}>
              <CircularProgress aria-label="正在檢查服務" color="success" size={18} />
              <Typography color="text.secondary" sx={{ fontSize: 12 }}>
                每 3 秒自動重新檢查
              </Typography>
            </Stack>
            <Button onClick={() => setAttempt((current) => current + 1)} variant="outlined">
              立即重試
            </Button>
          </Stack>
        </Stack>
      </Paper>
    </Box>
  );
}
