export type MarketQuote = {
  symbol: string;
  name: string;
  referencePrice: number;
  lastPrice: number | null;
  lastQuantity: number;
  updatedAt: string | null;
};

export type Instrument = {
  id: number;
  symbol: string;
  name: string;
  active: boolean;
  referencePrice: number;
  createdAt: string;
  updatedAt: string;
};

export type BookLevel = {
  price: number;
  quantity: number;
};

export type BookSnapshot = {
  symbol: string;
  bids: BookLevel[];
  asks: BookLevel[];
  updatedAt: string;
};

export type Trade = {
  tradeId: number;
  symbol: string;
  price: number;
  qty: number;
  createdAt: string;
};

export type TradePage = {
  content: Trade[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export class ApiError extends Error {
  constructor(readonly status: number) {
    super(`TradeGuard API returned HTTP ${status}`);
    this.name = "ApiError";
  }
}

function backendUrl(path: string): string {
  const baseUrl = process.env.BACKEND_URL ?? "http://localhost:8080";
  return new URL(path, baseUrl).toString();
}

async function getJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(backendUrl(path), init);
  if (!response.ok) {
    throw new ApiError(response.status);
  }
  return response.json() as Promise<T>;
}

export function getQuotes(): Promise<MarketQuote[]> {
  return getJson<MarketQuote[]>("/api/market/quotes", { cache: "no-store" });
}

export async function getInstrument(symbol: string): Promise<Instrument | null> {
  const response = await fetch(
    backendUrl(`/api/market/${encodeURIComponent(symbol)}/info`),
    { next: { revalidate: 86400, tags: [`instrument-${symbol}`] } },
  );
  if (response.status === 404) return null;
  if (!response.ok) {
    throw new ApiError(response.status);
  }
  return response.json() as Promise<Instrument>;
}

export function getBook(symbol: string): Promise<BookSnapshot> {
  return getJson<BookSnapshot>(
    `/api/market/${encodeURIComponent(symbol)}/book`,
    { cache: "no-store" },
  );
}

export function getRecentTrades(symbol: string): Promise<TradePage> {
  return getJson<TradePage>(
    `/api/market/${encodeURIComponent(symbol)}/trades?page=0&size=10&sort=id,desc`,
    { cache: "no-store" },
  );
}
