"use client";

import useSWR from "swr";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { PanelHeading } from "@/components/page-heading";
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
    <Paper className="panel" component="section" elevation={0} variant="outlined">
      <PanelHeading title="委託簿" trailing={error && <Alert severity="warning" sx={{ py: 0 }}>更新失敗，顯示先前資料</Alert>} />
      <Box className="book-columns">
        <Box>
          <Typography className="buy-label" component="h3" variant="subtitle2">買進</Typography>
          {book.bids.length === 0 ? <Typography className="empty-row" color="text.secondary" component="p">目前無買單</Typography> : book.bids.map((level, index) => (
            <Stack className="book-row bid-row" direction="row" key={`${level.price}-${index}`} sx={{ justifyContent: "space-between" }}>
              <Typography component="span" variant="body2">{level.price.toLocaleString("zh-TW")}</Typography>
              <Typography component="span" variant="body2">{level.quantity.toLocaleString("zh-TW")}</Typography>
            </Stack>
          ))}
        </Box>
        <Box>
          <Typography className="sell-label" component="h3" variant="subtitle2">賣出</Typography>
          {book.asks.length === 0 ? <Typography className="empty-row" color="text.secondary" component="p">目前無賣單</Typography> : book.asks.map((level, index) => (
            <Stack className="book-row ask-row" direction="row" key={`${level.price}-${index}`} sx={{ justifyContent: "space-between" }}>
              <Typography component="span" variant="body2">{level.price.toLocaleString("zh-TW")}</Typography>
              <Typography component="span" variant="body2">{level.quantity.toLocaleString("zh-TW")}</Typography>
            </Stack>
          ))}
        </Box>
      </Box>
    </Paper>
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
    <Paper className="panel" component="section" elevation={0} variant="outlined">
      <PanelHeading title="最近成交" trailing={error && <Alert severity="warning" sx={{ py: 0 }}>更新失敗，顯示先前資料</Alert>} />
      {trades.content.length === 0 ? (
        <Typography className="empty-row" color="text.secondary" component="p">尚無成交紀錄</Typography>
      ) : (
        <Box className="trade-list">
          {trades.content.map((trade) => (
            <Stack className="trade-row" direction="row" key={trade.tradeId} sx={{ justifyContent: "space-between" }}>
              <Typography color="text.secondary" component="time" dateTime={trade.createdAt} variant="body2">
                {new Date(trade.createdAt).toLocaleTimeString("zh-TW", { timeZone: "Asia/Taipei" })}
              </Typography>
              <Typography component="strong" variant="subtitle2">{trade.price.toLocaleString("zh-TW")}</Typography>
              <Typography color="text.secondary" component="span" variant="body2">{trade.qty.toLocaleString("zh-TW")} 股</Typography>
            </Stack>
          ))}
        </Box>
      )}
    </Paper>
  );
}
