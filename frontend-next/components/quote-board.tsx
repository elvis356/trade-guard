"use client";

import useSWR from "swr";
import Link from "next/link";
import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardActionArea from "@mui/material/CardActionArea";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
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
    return <Alert severity="error">目前無法載入行情，請稍後重試。</Alert>;
  }
  if (quotes.length === 0 && isLoading) {
    return <Alert severity="info">正在載入行情…</Alert>;
  }
  if (quotes.length === 0) {
    return (
      <Box className="empty-market">
        <Alert severity="info">目前沒有可顯示的商品行情。請管理員登入後，建立並啟用至少一項商品。</Alert>
        <Button component={Link} href="/admin" variant="contained">前往管理中心</Button>
      </Box>
    );
  }

  return (
    <>
      {error && <Alert severity="warning" sx={{ mb: 2 }}>行情更新暫時失敗；目前顯示最後一次成功的資料。</Alert>}
      <Box className="quote-grid">
        {quotes.map((quote) => (
          <Card className="quote-card" key={quote.symbol} variant="outlined">
            <CardActionArea component={Link} href={`/market/${encodeURIComponent(quote.symbol)}`}>
              <CardContent>
                <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between" }}>
                  <Typography className="symbol" component="span" variant="overline">{quote.symbol}</Typography>
                  <Chip color="success" label="模擬行情" size="small" variant="outlined" />
                </Stack>
                <Typography component="h2" sx={{ mt: 2 }} variant="h6">{quote.name}</Typography>
                <Typography className="quote-price" component="p" variant="h4">{price(quote.lastPrice)}</Typography>
                <Typography color="text.secondary" component="p" variant="body2">
                  參考價 {price(quote.referencePrice)}
                  {quote.lastQuantity > 0 && ` · 最近成交 ${quote.lastQuantity.toLocaleString("zh-TW")} 股`}
                </Typography>
              </CardContent>
            </CardActionArea>
          </Card>
        ))}
      </Box>
    </>
  );
}
