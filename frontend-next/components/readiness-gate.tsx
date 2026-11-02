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
        background: "#fff",
        color: "#0b0b0b",
        display: "flex",
        flexDirection: "column",
        minHeight: "100svh",
      }}
    >
      <Box
        component="header"
        sx={{
          background: "#0b0b0b",
          color: "#fff",
          isolation: "isolate",
          overflow: "hidden",
          padding: { xs: "34px 22px 76px", sm: "42px 32px 86px" },
          position: "relative",
          "&::before": {
            background: "repeating-conic-gradient(#0b0b0b 0 25%, #1c1c1c 0 50%) 0 0 / 56px 56px",
            content: '""',
            inset: 0,
            maskImage: "linear-gradient(180deg, #000, transparent 96%)",
            opacity: 0.9,
            position: "absolute",
            zIndex: -1,
          },
          "&::after": {
            background: "repeating-conic-gradient(#fff 0 25%, #0b0b0b 0 50%) 0 0 / 28px 28px",
            bottom: 0,
            content: '""',
            height: 14,
            left: 0,
            position: "absolute",
            right: 0,
          },
        }}
      >
        <Box sx={{ margin: "0 auto", maxWidth: 820, width: "100%" }}>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: "center", mb: 4 }}>
            <Box
              aria-hidden="true"
              sx={{
                alignItems: "center",
                border: "1px solid #fff",
                borderRadius: "50%",
                display: "flex",
                fontSize: 10,
                fontWeight: 900,
                height: 36,
                justifyContent: "center",
                letterSpacing: "-.06em",
                width: 36,
              }}
            >
              TG
            </Box>
            <Typography
              sx={{ color: "#a3a3a3", fontFamily: "Consolas, monospace", fontSize: 10, fontWeight: 700, letterSpacing: ".18em" }}
              variant="overline"
            >
              TRADEGUARD · SERVICE GATE
            </Typography>
          </Stack>
          <Typography
            component="h1"
            sx={{ fontSize: { xs: 32, sm: 44 }, fontWeight: 900, letterSpacing: "-.045em", lineHeight: 1.12 }}
          >
            正在確認服務狀態
          </Typography>
          <Typography sx={{ color: "#d0d0d0", lineHeight: 1.8, mt: 1.5, maxWidth: 620 }}>
            首次連線可能需要 1-2 分鐘喚醒後端。待 API、資料庫與 Redis 都正常後，將自動載入您要前往的頁面。
          </Typography>
        </Box>
      </Box>

      <Box
        sx={{
          margin: { xs: "-42px 20px 36px", sm: "-48px auto 48px" },
          maxWidth: 820,
          position: "relative",
          width: { xs: "calc(100% - 40px)", sm: "calc(100% - 64px)" },
        }}
      >
        <Paper
          elevation={0}
          sx={{
            border: "1px solid #e3e3e3",
            borderRadius: 0,
            boxShadow: "0 18px 54px rgba(0,0,0,.08)",
            overflow: "hidden",
            p: { xs: 2.5, sm: 4 },
            width: "100%",
          }}
        >
          <Stack spacing={3}>
            <Stack
              direction="row"
              sx={{ alignItems: "center", justifyContent: "space-between" }}
            >
              <Typography sx={{ color: "#666", fontFamily: "Consolas, monospace", fontSize: 10, fontWeight: 700, letterSpacing: ".16em" }}>
                DEPENDENCY STATUS
              </Typography>
              <Typography sx={{ color: "#666", fontFamily: "Consolas, monospace", fontSize: 10 }}>
                AUTO CHECK · 03 SEC
              </Typography>
            </Stack>
            <Stack
              aria-live="polite"
              component="ul"
              spacing={0}
              sx={{
                borderBottom: "1px solid #e3e3e3",
                borderTop: "1px solid #e3e3e3",
                listStyle: "none",
                m: 0,
                p: 0,
              }}
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
                      borderBottom: "1px solid #ececec",
                      display: "flex",
                      gap: 1.75,
                      minHeight: 68,
                      "&:last-child": { borderBottom: 0 },
                    }}
                  >
                    <Box sx={{ bgcolor: color, borderRadius: "50%", flex: "0 0 8px", height: 8 }} />
                    <Box sx={{ flex: 1 }}>
                      <Typography sx={{ color: "#0b0b0b", fontSize: 14, fontWeight: 750 }}>{label}</Typography>
                      <Typography sx={{ color: "#666", fontSize: 12 }}>{detail}</Typography>
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
              <CircularProgress aria-label="正在檢查服務" size={18} sx={{ color: "#0b0b0b" }} />
              <Typography sx={{ color: "#666", fontSize: 12 }}>
                每 3 秒自動重新檢查
              </Typography>
            </Stack>
            <Button
              onClick={() => setAttempt((current) => current + 1)}
              sx={{
                background: "#0b0b0b",
                border: "1px solid #0b0b0b",
                borderRadius: 0,
                color: "#fff",
                fontSize: 12,
                fontWeight: 700,
                letterSpacing: ".04em",
                px: 2.5,
                py: 1,
                "&:hover": { background: "#333", borderColor: "#333" },
              }}
              variant="contained"
            >
              立即重試
            </Button>
          </Stack>
          </Stack>
        </Paper>
      </Box>
      <Box
        component="footer"
        sx={{
          borderTop: "1px solid #e3e3e3",
          color: "#666",
          fontSize: 10,
          letterSpacing: ".06em",
          mt: "auto",
          px: 3,
          py: 2,
          textAlign: "center",
        }}
      >
        教學模擬平台，使用虛構資料；非真實交易系統，亦不構成投資建議。
      </Box>
    </Box>
  );
}
