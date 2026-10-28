import Link from "next/link";

const links = [
  { href: "/", label: "市場總覽" },
  { href: "/rules/matching", label: "撮合規則" },
  { href: "/reports/daily", label: "日終報表" },
];

export function SiteHeader() {
  return (
    <header className="site-header">
      <Link aria-label="TradeGuard 首頁" className="brand" href="/">
        <span className="brand-mark">TG</span>
        <span>TradeGuard</span>
      </Link>
      <nav aria-label="主要導覽">
        {links.map((link) => (
          <Link href={link.href} key={link.href}>
            {link.label}
          </Link>
        ))}
      </nav>
      <span className="market-badge"><span /> 模擬市場</span>
    </header>
  );
}
