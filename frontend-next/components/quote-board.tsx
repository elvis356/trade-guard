"use client";

import useSWR from "swr";
import Link from "next/link";
import type { MarketQuote } from "@/lib/api";

async function fetchQuotes(path: string): Promise<MarketQuote[]> {
  const response = await fetch(path, { cache: "no-store" });
  if (!response.ok) throw new Error(`行情更新失敗 (HTTP ${response.status})`);
  return response.json() as Promise<MarketQuote[]>;
}

function price(value: number | null): string {
  return value == null
    ? "尚無成交"
    : new Intl.NumberFormat("zh-TW", { minimumFractionDigits: 2 }).format(value);
}

export function QuoteBoard({ initialQuotes }: { initialQuotes: MarketQuote[] }) {
  const { data, error, isLoading } = useSWR("/api/market/quotes", fetchQuotes, {
    fallbackData: initialQuotes,
    refreshInterval: 5000,
    revalidateOnFocus: true,
  });
  const quotes = data ?? initialQuotes;

  if (error && quotes.length === 0) {
    return <p className="notice error">目前無法載入行情，請稍後重試。</p>;
  }
  if (quotes.length === 0 && isLoading) {
    return <p className="notice">正在載入行情…</p>;
  }
  if (quotes.length === 0) {
    return (
      <div className="empty-market">
        <p className="notice">目前沒有可顯示的商品行情。請管理員登入後，建立並啟用至少一項商品。</p>
        <Link className="button-link" href="/admin">前往管理中心</Link>
      </div>
    );
  }

  return (
    <>
      {error && <p className="notice stale">行情更新暫時失敗；目前顯示最後一次成功的資料。</p>}
      <div className="quote-grid">
        {quotes.map((quote) => (
          <Link className="quote-card" href={`/market/${encodeURIComponent(quote.symbol)}`} key={quote.symbol}>
            <div className="quote-heading">
              <span className="symbol">{quote.symbol}</span>
              <span className="quote-change">模擬行情</span>
            </div>
            <h2>{quote.name}</h2>
            <p className="quote-price">{price(quote.lastPrice)}</p>
            <p className="quote-meta">
              參考價 {price(quote.referencePrice)}
              {quote.lastQuantity > 0 && ` · 最近成交 ${quote.lastQuantity.toLocaleString("zh-TW")} 股`}
            </p>
          </Link>
        ))}
      </div>
    </>
  );
}
