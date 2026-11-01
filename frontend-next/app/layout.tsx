import type { Metadata } from "next";
import { Lato } from "next/font/google";
import { AuthProvider } from "@/components/auth-provider";
import { SiteHeader } from "@/components/site-header";
import { ThemeProvider } from "@/components/theme-provider";
import "./globals.css";

const lato = Lato({ display: "swap", subsets: ["latin"], variable: "--font-lato", weight: ["300", "400", "700", "900"] });

export const metadata: Metadata = {
  title: {
    default: "TradeGuard｜證券交易模擬與市場監視",
    template: "%s｜TradeGuard",
  },
  description: "教學用途的虛構證券交易模擬與市場監視平台，非真實交易系統或投資建議。",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-Hant">
      <body className={lato.variable}>
        <ThemeProvider>
          <AuthProvider>
            <SiteHeader />
            <main className="page-shell">{children}</main>
            <footer className="site-footer">
              教學模擬平台，使用虛構資料；非真實交易系統，亦不構成投資建議。
            </footer>
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
