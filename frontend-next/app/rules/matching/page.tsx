import type { Metadata } from "next";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { PageHeading } from "@/components/page-heading";

export const dynamic = "force-static";

export const metadata: Metadata = {
  title: "撮合與交易規則",
};

export default function MatchingRulesPage() {
  return (
    <>
      <PageHeading
        description="TradeGuard 以虛構條件示範限價委託和市場監視流程。"
        eyebrow="STATIC GUIDE"
        title="撮合與交易規則"
      />
      <Paper className="panel rule-list" component="section" elevation={0} variant="outlined">
        <Stack spacing={3}>
          <Box component="section">
            <Typography component="h2" variant="h5" gutterBottom>
              委託與撮合
            </Typography>
            <Stack component="ul" spacing={1} sx={{ pl: 3 }}>
              <Typography component="li" variant="body1">目前支援限價買進與賣出委託，並驗證交易時段、價格區間、升降單位及交易單位。</Typography>
              <Typography component="li" variant="body1">撮合採價格優先，再依商品序號決定時間優先；支援部分成交。</Typography>
              <Typography component="li" variant="body1">成交價格依可成交對手委託價格決定，未成交數量保留於委託簿。</Typography>
            </Stack>
          </Box>
          <Box component="section">
            <Typography component="h2" variant="h5" gutterBottom>市場監視與案件</Typography>
            <Stack component="ul" spacing={1} sx={{ pl: 3 }}>
              <Typography component="li" variant="body1">規則式警示僅提供人工檢視線索，不代表違規認定。</Typography>
              <Typography component="li" variant="body1">案件提案與覆核須由不同監視使用者操作，以符合四眼原則。</Typography>
            </Stack>
          </Box>
          <Alert className="disclaimer" severity="info" variant="outlined">
            所列參數僅為教學示意，不等同任何交易所或監管機關的正式規則。
          </Alert>
        </Stack>
      </Paper>
    </>
  );
}
