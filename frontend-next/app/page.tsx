import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import { QuoteBoard } from "@/components/quote-board";
import { PageHeading } from "@/components/page-heading";
import { getQuotes, type MarketQuote } from "@/lib/api";

export default async function HomePage() {
  let quotes: MarketQuote[] = [];
  try {
    quotes = await getQuotes();
  } catch {
    // QuoteBoard retries from the browser and presents its API error state.
  }

  return (
    <>
      <PageHeading
        description="瀏覽公開市場報價，選擇商品查看委託簿與近期成交。登入後可依角色使用交易與管理工作區。"
        eyebrow="MARKET OVERVIEW · PUBLIC"
        title="市場總覽"
      />
      <Paper className="panel" component="section" elevation={0} variant="outlined">
        <Box sx={{ mb: 2 }}>
          <Typography className="eyebrow" component="p" variant="overline">MARKET WATCH</Typography>
          <Typography component="h2" variant="h6">公開行情</Typography>
          <Typography color="text.secondary" component="p" variant="body2">
            行情每 5 秒更新；選取報價卡可查看商品委託簿與近期成交。
          </Typography>
        </Box>
        <QuoteBoard initialQuotes={quotes} />
        <Alert className="disclaimer" severity="info" variant="outlined">
          所有報價皆為教學模擬資料，不涉及真實證券或投資建議。
        </Alert>
      </Paper>
    </>
  );
}
