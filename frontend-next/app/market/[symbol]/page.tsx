import type { Metadata } from "next";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { OrderBookPanel, RecentTradesPanel } from "@/components/market-panels";
import { PageHeading } from "@/components/page-heading";
import { getBook, getInstrument, getRecentTrades } from "@/lib/api";

export const dynamic = "force-dynamic";

type Props = { params: Promise<{ symbol: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { symbol } = await params;
  return { title: `${symbol} 商品行情` };
}

function PanelSkeleton({ title }: { title: string }) {
  return (
    <Paper aria-label={`載入${title}`} className="panel skeleton" component="section" elevation={0} variant="outlined">
      <Typography component="h2" variant="h6">{title}</Typography>
      <Typography color="text.secondary" component="p" variant="body2">正在取得最新資料…</Typography>
    </Paper>
  );
}

export default async function MarketPage({ params }: Props) {
  const { symbol } = await params;
  let instrument;
  try {
    instrument = await getInstrument(symbol);
  } catch {
    return <Alert severity="error">商品資料目前無法載入，請稍後重試。</Alert>;
  }
  if (!instrument || !instrument.active) notFound();

  return (
    <>
      <PageHeading
        description={`參考價 ${instrument.referencePrice.toLocaleString("zh-TW")} · 商品基本資料每日快取。`}
        eyebrow={`INSTRUMENT · ${instrument.symbol}`}
        title={instrument.name}
      />
      <Box className="panel-grid">
        <Suspense fallback={<PanelSkeleton title="委託簿" />}>
          <OrderBook symbol={symbol} />
        </Suspense>
        <Suspense fallback={<PanelSkeleton title="最近成交" />}>
          <RecentTrades symbol={symbol} />
        </Suspense>
      </Box>
    </>
  );
}

async function OrderBook({ symbol }: { symbol: string }) {
  try {
    const book = await getBook(symbol);
    return <OrderBookPanel symbol={symbol} initialBook={book} />;
  } catch {
    return <Alert severity="error">目前無法載入委託簿。</Alert>;
  }
}

async function RecentTrades({ symbol }: { symbol: string }) {
  try {
    const trades = await getRecentTrades(symbol);
    return <RecentTradesPanel symbol={symbol} initialTrades={trades} />;
  } catch {
    return <Alert severity="error">目前無法載入成交紀錄。</Alert>;
  }
}
