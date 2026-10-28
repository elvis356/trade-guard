import type { Metadata } from "next";
import { SiteHeader } from "@/components/site-header";
import "./globals.css";

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
      <body>
        <SiteHeader />
        <main className="page-shell">{children}</main>
        <footer className="site-footer">
          教學模擬平台，使用虛構資料；非真實交易系統，亦不構成投資建議。
        </footer>
      </body>
    </html>
  );
}
