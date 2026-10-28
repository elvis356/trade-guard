import Link from "next/link";
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
      <section className="hero">
        <p className="eyebrow">MARKET OVERVIEW · TAIPEI</p>
        <h1>市場概覽</h1>
        <p>檢視模擬市場商品行情，行情資料每五秒自動更新。</p>
      </section>
      {unavailable ? (
        <p className="notice error">
          行情服務目前無法連線。請確認後端已啟動，且前端 `BACKEND_URL` 設定正確。
        </p>
      ) : (
        <QuoteBoard initialQuotes={quotes ?? []} />
      )}
      <section className="callout">
        <div>
          <p className="eyebrow">SIMULATED EXCHANGE</p>
          <h2>僅供教學與作品集展示</h2>
          <p>報價、委託與成交皆屬模擬資料，不可用於真實交易決策。</p>
        </div>
        <Link className="button-link" href="/rules/matching">了解撮合規則</Link>
      </section>
    </>
  );
}
