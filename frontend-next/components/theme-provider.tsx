"use client";

import { AppRouterCacheProvider } from "@mui/material-nextjs/v15-appRouter";
import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider as MuiThemeProvider, createTheme } from "@mui/material/styles";
import type { ReactNode } from "react";

// 設計語彙參考 Humism：純黑白、黑色頂部導覽、粗體無襯線標題、直角元素。
// 綠／紅／琥珀僅作為買賣與狀態的語意色。
const ink = "#0b0b0b";
const line = "#e3e3e3";
const sans = 'var(--font-lato), "Noto Sans TC", "Microsoft JhengHei", sans-serif';

const theme = createTheme({
  palette: {
    mode: "light",
    primary: { main: ink, dark: "#000000", light: "#f0f0f0", contrastText: "#ffffff" },
    secondary: { main: "#6b6b6b" },
    error: { main: "#b3261e" },
    warning: { main: "#8a6100" },
    success: { main: "#1f7a4d" },
    info: { main: "#2c5d8f" },
    background: { default: "#ffffff", paper: "#ffffff" },
    text: { primary: ink, secondary: "#666666" },
    divider: line,
  },
  shape: { borderRadius: 2 },
  typography: {
    fontFamily: sans,
    h1: { fontFamily: sans, fontSize: "clamp(2rem, 5vw, 3.4rem)", fontWeight: 900, letterSpacing: ".04em", marginBottom: 12 },
    h2: { fontFamily: sans, fontWeight: 800 },
    h5: { fontWeight: 800, letterSpacing: ".02em" },
    h6: { fontWeight: 800, letterSpacing: ".03em" },
    overline: { letterSpacing: ".2em" },
    button: { fontWeight: 700, textTransform: "none", letterSpacing: ".06em" },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 2, paddingInline: 20, transition: "background-color .18s, color .18s, border-color .18s" },
        contained: { "&:hover": { backgroundColor: "#333333" } },
        outlined: { borderColor: "#bdbdbd", color: ink, "&:hover": { borderColor: ink, backgroundColor: "#f5f5f5" } },
        text: { color: "#444444", "&:hover": { backgroundColor: "#f0f0f0", color: ink } },
      },
    },
    MuiPaper: { styleOverrides: { root: { backgroundImage: "none" }, rounded: { border: `1px solid ${line}` } } },
    MuiAppBar: { styleOverrides: { root: { backgroundImage: "none" } } },
    MuiChip: { styleOverrides: { root: { borderRadius: 2, letterSpacing: ".06em" } } },
    MuiAlert: { styleOverrides: { root: { borderRadius: 2 } } },
  },
});

export function ThemeProvider({ children }: { children: ReactNode }) {
  return (
    <AppRouterCacheProvider>
      <MuiThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </MuiThemeProvider>
    </AppRouterCacheProvider>
  );
}
