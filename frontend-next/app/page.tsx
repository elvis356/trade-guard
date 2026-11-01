import Link from "next/link";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import { PageHeading } from "@/components/page-heading";
import { QuoteBoard } from "@/components/quote-board";
import { getQuotes } from "@/lib/api";

export const dynamic = "force-dynamic";

export default async function HomePage() {
  let quotes = null;
  let unavailable = false;
  try {
    quotes = await getQuotes();
  } catch {
    unavailable = true;
  }

  return (
    <>
      <PageHeading
        description="檢視模擬市場商品行情，行情資料每五秒自動更新。"
        eyebrow="MARKET OVERVIEW · TAIPEI"
        title="市場概覽"
      />
      {unavailable ? (
        <Alert className="notice" severity="error">
          行情服務目前無法連線。請確認後端已啟動，且前端 `BACKEND_URL` 設定正確。
        </Alert>
      ) : (
        <QuoteBoard initialQuotes={quotes ?? []} />
      )}
      <Paper className="callout" component="section" elevation={0} variant="outlined">
        <Box>
          <Typography className="eyebrow" component="p" variant="overline">SIMULATED EXCHANGE</Typography>
          <Typography component="h2" variant="h5">僅供教學與作品集展示</Typography>
          <Typography color="text.secondary" component="p" sx={{ mt: 1 }} variant="body2">報價、委託與成交皆屬模擬資料，不可用於真實交易決策。</Typography>
        </Box>
        <Button component={Link} href="/rules/matching" variant="contained">了解撮合規則</Button>
      </Paper>
    </>
  );
}
