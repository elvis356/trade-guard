import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { OrderBookPanel, RecentTradesPanel } from "@/components/market-panels";
import { getBook, getInstrument, getRecentTrades } from "@/lib/api";

export const dynamic = "force-dynamic";

type Props = { params: Promise<{ symbol: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { symbol } = await params;
  return { title: `${symbol} 商品行情` };
}

function PanelSkeleton({ title }: { title: string }) {
  return (
    <section aria-label={`載入${title}`} className="panel skeleton">
      <h2>{title}</h2>
      <p>正在取得最新資料…</p>
    </section>
  );
}

export default async function MarketPage({ params }: Props) {
  const { symbol } = await params;
  let instrument;
  try {
    instrument = await getInstrument(symbol);
  } catch {
    return <p className="notice error">商品資料目前無法載入，請稍後重試。</p>;
  }
  if (!instrument || !instrument.active) notFound();

  return (
    <>
      <section className="hero">
        <p className="eyebrow">INSTRUMENT · {instrument.symbol}</p>
        <h1>{instrument.name}</h1>
        <p>參考價 {instrument.referencePrice.toLocaleString("zh-TW")} · 商品基本資料每日快取。</p>
      </section>
      <div className="panel-grid">
        <Suspense fallback={<PanelSkeleton title="委託簿" />}>
          <OrderBook symbol={symbol} />
        </Suspense>
        <Suspense fallback={<PanelSkeleton title="最近成交" />}>
          <RecentTrades symbol={symbol} />
        </Suspense>
      </div>
    </>
  );
}

async function OrderBook({ symbol }: { symbol: string }) {
  try {
    const book = await getBook(symbol);
    return <OrderBookPanel symbol={symbol} initialBook={book} />;
  } catch {
    return <p className="notice error">目前無法載入委託簿。</p>;
  }
}

async function RecentTrades({ symbol }: { symbol: string }) {
  try {
    const trades = await getRecentTrades(symbol);
    return <RecentTradesPanel symbol={symbol} initialTrades={trades} />;
  } catch {
    return <p className="notice error">目前無法載入成交紀錄。</p>;
  }
}
