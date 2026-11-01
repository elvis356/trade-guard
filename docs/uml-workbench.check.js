#!/usr/bin/env node
/*
 * uml-workbench.html 內嵌資料（<script id="uml-data">）一致性檢查（無外部相依，Node 18+）。
 *
 *   node docs/uml-workbench.check.js            錯誤 → exit 1，警告只列出
 *   node docs/uml-workbench.check.js --strict   警告也視為失敗
 *
 * 錯誤：資料自相矛盾，工作台一定會顯示錯誤或斷鏈。
 * 警告：版面或追溯覆蓋度的提醒，需要人判斷。
 */
"use strict";

const fs = require("fs");
const path = require("path");
const vm = require("vm");

// 資料內嵌在 uml-workbench.html 的 <script id="uml-data">；在隔離的 vm 內執行取出 UML_DATA。
const htmlFile = path.join(__dirname, "uml-workbench.html");
const block = fs.readFileSync(htmlFile, "utf8").match(/<script id="uml-data">([\s\S]*?)<\/script>/);
if (!block) {
  console.error('在 uml-workbench.html 找不到 <script id="uml-data"> 區塊');
  process.exit(1);
}
const sandbox = {};
vm.createContext(sandbox);
vm.runInContext(block[1], sandbox, { filename: "uml-workbench.html#uml-data" });
const { TYPES, ENTITIES, TRACE, REL, DIAGRAMS } = sandbox.UML_DATA;

const strict = process.argv.includes("--strict");
const errors = [];
const warnings = [];
const err = (diagram, msg) => errors.push(`${diagram ? `[${diagram}] ` : ""}${msg}`);
const warn = (diagram, msg) => warnings.push(`${diagram ? `[${diagram}] ` : ""}${msg}`);

const ROW = 18, HEADER_PAD = 41; // 與 uml-workbench.html 的 drawClass / drawTable / fitView 一致

// ---------------------------------------------------------------- 實體
const names = new Map();
for (const [id, e] of Object.entries(ENTITIES)) {
  if (!e.name || !e.desc) err(null, `實體 ${id} 缺少 name 或 desc`);
  if (!TYPES[e.type]) err(null, `實體 ${id} 的 type「${e.type}」不在 TYPES`);
  if (!names.has(e.name)) names.set(e.name, []);
  names.get(e.name).push(id);
}
for (const [name, ids] of names) {
  if (ids.length > 1) err(null, `顯示名稱重複「${name}」：${ids.join("、")}（同一元件請共用 ID，避免跨圖追溯斷鏈）`);
}

// ---------------------------------------------------------------- 圖表
const used = new Set();
const rows = (n) => {
  const e = ENTITIES[n.id];
  return n.shape === "table" ? (e.columns || e.fields || []).length : (e.fields || []).length;
};
const heightOf = (n) => (["class", "table"].includes(n.shape) ? Math.max(n.h, HEADER_PAD + rows(n) * ROW) : n.h);

function anchorToward(n, x, y) {
  const h = heightOf(n), cx = n.x + n.w / 2, cy = n.y + h / 2, dx = x - cx, dy = y - cy;
  const k = Math.min(n.w / 2 / Math.max(Math.abs(dx), 0.001), h / 2 / Math.max(Math.abs(dy), 0.001));
  return [cx + dx * k, cy + dy * k];
}
function edgeAnchor(a, b) {
  const ah = heightOf(a), bh = heightOf(b);
  const acx = a.x + a.w / 2, acy = a.y + ah / 2, bcx = b.x + b.w / 2, bcy = b.y + bh / 2;
  const dx = bcx - acx, dy = bcy - acy;
  const ka = Math.min(a.w / 2 / Math.max(Math.abs(dx), 0.001), ah / 2 / Math.max(Math.abs(dy), 0.001));
  const kb = Math.min(b.w / 2 / Math.max(Math.abs(dx), 0.001), bh / 2 / Math.max(Math.abs(dy), 0.001));
  return [[acx + dx * ka, acy + dy * ka], [bcx - dx * kb, bcy - dy * kb]];
}
const inside = (n, [x, y], pad) => x > n.x + pad && x < n.x + n.w - pad && y > n.y + pad && y < n.y + heightOf(n) - pad;

for (const d of DIAGRAMS) {
  const ids = d.nodes.map((n) => n.id);
  const seen = new Set();
  for (const n of d.nodes) {
    used.add(n.id);
    if (!ENTITIES[n.id]) err(d.id, `節點 ${n.id} 沒有對應的實體`);
    if (seen.has(n.id)) err(d.id, `節點 ${n.id} 在同一張圖重複`);
    seen.add(n.id);
    if (n.shape === "table" && !(ENTITIES[n.id]?.columns || []).length) err(d.id, `資料表節點 ${n.id} 沒有 columns`);
  }

  if (d.sequence) {
    const inMessage = new Set();
    d.messages.forEach(([from, to], i) => {
      for (const p of [from, to]) {
        if (!d.participants.includes(p)) err(d.id, `訊息 #${i + 1} 引用不在 participants 的 ${p}`);
        inMessage.add(p);
      }
    });
    for (const p of d.participants) {
      if (!ids.includes(p)) err(d.id, `參與者 ${p} 沒有對應節點`);
      if (!inMessage.has(p)) err(d.id, `參與者 ${p} 沒有任何訊息（空生命線）`);
    }
    for (const [who, from, to] of d.activations || []) {
      if (!d.participants.includes(who)) err(d.id, `活動條引用未知參與者 ${who}`);
      if (!(from >= 0 && to >= from && to < d.messages.length)) err(d.id, `活動條 ${who} 的訊息範圍 ${from}–${to} 超出 0–${d.messages.length - 1}`);
    }
    continue;
  }

  for (const e of d.edges || []) {
    if (!ids.includes(e.a) || !ids.includes(e.b)) err(d.id, `連線 ${e.a} → ${e.b} 指向不在本圖的節點`);
  }

  // 節點重疊（容器節點本來就會包住其他節點，略過）
  const boxes = d.nodes.filter((n) => n.shape !== "container");
  for (let i = 0; i < boxes.length; i++) {
    for (let j = i + 1; j < boxes.length; j++) {
      const a = boxes[i], b = boxes[j], ah = heightOf(a), bh = heightOf(b);
      if (a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + bh && b.y < a.y + ah) err(d.id, `節點 ${a.id} 與 ${b.id} 重疊`);
    }
  }

  // 連線穿過其他節點（警示；端點節點與容器除外）
  for (const e of d.edges || []) {
    const a = d.nodes.find((n) => n.id === e.a), b = d.nodes.find((n) => n.id === e.b);
    if (!a || !b) continue;
    const pts = e.via && e.via.length
      ? [anchorToward(a, ...e.via[0]), ...e.via, anchorToward(b, ...e.via[e.via.length - 1])]
      : edgeAnchor(a, b);
    const hit = new Set();
    for (let i = 1; i < pts.length; i++) {
      const [x1, y1] = pts[i - 1], [x2, y2] = pts[i];
      const steps = Math.max(2, Math.ceil(Math.hypot(x2 - x1, y2 - y1) / 4));
      for (let k = 0; k <= steps; k++) {
        const p = [x1 + ((x2 - x1) * k) / steps, y1 + ((y2 - y1) * k) / steps];
        for (const n of boxes) if (n !== a && n !== b && inside(n, p, 3)) hit.add(n.id);
      }
    }
    if (hit.size) warn(d.id, `連線 ${e.a} → ${e.b} 穿過節點：${[...hit].join("、")}`);
  }
}

for (const id of Object.keys(ENTITIES)) if (!used.has(id)) err(null, `實體 ${id} 沒有出現在任何圖表`);

// ---------------------------------------------------------------- 追溯
const linked = new Set();
for (const [from, rel, to] of TRACE) {
  if (!ENTITIES[from]) err(null, `TRACE 引用未知實體 ${from}`);
  if (!ENTITIES[to]) err(null, `TRACE 引用未知實體 ${to}`);
  if (!REL[rel]) err(null, `TRACE 關係類型「${rel}」不在 REL（${from} → ${to}）`);
  linked.add(from);
  linked.add(to);
}
for (const [id, e] of Object.entries(ENTITIES)) {
  if (["usecase", "domain", "actor"].includes(e.type) && !linked.has(id)) warn(null, `${e.type} 實體 ${id}（${e.name}）沒有任何 TRACE 連線`);
}

// ---------------------------------------------------------------- 輸出
const entityCount = Object.keys(ENTITIES).length;
console.log(`UML 工作台資料：${DIAGRAMS.length} 張圖、${entityCount} 個實體、${TRACE.length} 條追溯關係`);
if (warnings.length) console.log(`\n警告 (${warnings.length})\n` + warnings.map((m) => `  - ${m}`).join("\n"));
if (errors.length) console.log(`\n錯誤 (${errors.length})\n` + errors.map((m) => `  - ${m}`).join("\n"));
if (!errors.length && !warnings.length) console.log("全部通過。");
process.exit(errors.length || (strict && warnings.length) ? 1 : 0);
