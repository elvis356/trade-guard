"use client";

import { AppRouterCacheProvider } from "@mui/material-nextjs/v15-appRouter";
import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider as MuiThemeProvider, createTheme } from "@mui/material/styles";
import type { ReactNode } from "react";

const theme = createTheme({
  palette: {
    mode: "light",
    primary: { main: "#176b4b", dark: "#104c36", light: "#dcefe4" },
    secondary: { main: "#c27a43" },
    error: { main: "#a83d45" },
    warning: { main: "#947126" },
    success: { main: "#28764f" },
    background: { default: "#f4f6f1", paper: "#fffefa" },
    text: { primary: "#17241f", secondary: "#65756e" },
    divider: "#dce5df",
  },
  shape: { borderRadius: 12 },
  typography: {
    fontFamily: '"Aptos", "Segoe UI", "Noto Sans TC", "Microsoft JhengHei", sans-serif',
    h1: {
      fontFamily: 'Georgia, "Noto Serif TC", "PMingLiU", serif',
      fontSize: "clamp(2rem, 5vw, 3.1rem)",
      fontWeight: 650,
      letterSpacing: "-.045em",
      marginBottom: 12,
    },
    h2: { fontFamily: 'Georgia, "Noto Serif TC", "PMingLiU", serif', fontWeight: 650 },
    button: { fontWeight: 700, textTransform: "none" },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 9,
          paddingInline: 16,
          "&.MuiButton-containedPrimary": { boxShadow: "0 5px 14px rgba(23, 107, 75, .16)" },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { backgroundImage: "none" },
        rounded: { border: "1px solid #dce5df" },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: { backgroundImage: "none" },
      },
    },
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
