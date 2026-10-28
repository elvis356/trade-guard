import type { Metadata } from "next";

export const dynamic = "force-static";

export const metadata: Metadata = {
  title: "撮合與交易規則",
};

export default function MatchingRulesPage() {
  return (
    <>
      <section className="hero">
        <p className="eyebrow">STATIC GUIDE</p>
        <h1>撮合與交易規則</h1>
        <p>TradeGuard 以虛構條件示範限價委託和市場監視流程。</p>
      </section>
      <section className="panel rule-list">
        <h2>委託與撮合</h2>
        <ul>
          <li>目前支援限價買進與賣出委託，並驗證交易時段、價格區間、升降單位及交易單位。</li>
          <li>撮合採價格優先，再依商品序號決定時間優先；支援部分成交。</li>
          <li>成交價格依可成交對手委託價格決定，未成交數量保留於委託簿。</li>
        </ul>
        <h2>市場監視與案件</h2>
        <ul>
          <li>規則式警示僅提供人工檢視線索，不代表違規認定。</li>
          <li>案件提案與覆核須由不同監視使用者操作，以符合四眼原則。</li>
        </ul>
        <p className="disclaimer">
          所列參數僅為教學示意，不等同任何交易所或監管機關的正式規則。
        </p>
      </section>
    </>
  );
}
