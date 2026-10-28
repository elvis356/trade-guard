"use client";

import useSWR from "swr";
import type { BookSnapshot, TradePage } from "@/lib/api";

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path, { cache: "no-store" });
  if (!response.ok) throw new Error(`資料更新失敗 (HTTP ${response.status})`);
  return response.json() as Promise<T>;
}

export function OrderBookPanel({
  symbol,
  initialBook,
}: {
  symbol: string;
  initialBook: BookSnapshot;
}) {
  const { data, error } = useSWR(
    `/api/market/${encodeURIComponent(symbol)}/book`,
    getJson<BookSnapshot>,
    { fallbackData: initialBook, refreshInterval: 2000 },
  );
  const book = data ?? initialBook;

  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>委託簿</h2>
        {error && <span className="inline-error">更新失敗，顯示先前資料</span>}
      </div>
      <div className="book-columns">
        <div>
          <h3 className="buy-label">買進</h3>
          {book.bids.length === 0 ? <p className="empty-row">目前無買單</p> : book.bids.map((level, index) => (
            <div className="book-row bid-row" key={`${level.price}-${index}`}>
              <span>{level.price.toLocaleString("zh-TW")}</span>
              <span>{level.quantity.toLocaleString("zh-TW")}</span>
            </div>
          ))}
        </div>
        <div>
          <h3 className="sell-label">賣出</h3>
          {book.asks.length === 0 ? <p className="empty-row">目前無賣單</p> : book.asks.map((level, index) => (
            <div className="book-row ask-row" key={`${level.price}-${index}`}>
              <span>{level.price.toLocaleString("zh-TW")}</span>
              <span>{level.quantity.toLocaleString("zh-TW")}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export function RecentTradesPanel({
  symbol,
  initialTrades,
}: {
  symbol: string;
  initialTrades: TradePage;
}) {
  const path = `/api/market/${encodeURIComponent(symbol)}/trades?page=0&size=10&sort=id,desc`;
  const { data, error } = useSWR(path, getJson<TradePage>, {
    fallbackData: initialTrades,
    refreshInterval: 5000,
  });
  const trades = data ?? initialTrades;

  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>最近成交</h2>
        {error && <span className="inline-error">更新失敗，顯示先前資料</span>}
      </div>
      {trades.content.length === 0 ? (
        <p className="empty-row">尚無成交紀錄</p>
      ) : (
        <div className="trade-list">
          {trades.content.map((trade) => (
            <div className="trade-row" key={trade.tradeId}>
              <time dateTime={trade.createdAt}>
                {new Date(trade.createdAt).toLocaleTimeString("zh-TW", { timeZone: "Asia/Taipei" })}
              </time>
              <strong>{trade.price.toLocaleString("zh-TW")}</strong>
              <span>{trade.qty.toLocaleString("zh-TW")} 股</span>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
