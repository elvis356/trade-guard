export type Role = "TRADER" | "SURVEILLANCE" | "AUDITOR" | "ADMIN";

export type CurrentUser = {
  username: string;
  role: Role;
};

export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
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

export type Order = {
  order_id: number;
  client_order_id: string;
  seq_no: number;
  symbol: string;
  side: "B" | "S";
  price: number;
  qty: number;
  filled_qty: number;
  remaining_qty: number;
  status: "NEW" | "PARTIALLY_FILLED" | "FILLED" | "CANCELED";
  createdAt: string;
};

export type Alert = {
  id: number;
  ruleCode: string;
  symbol: string;
  severity: string;
  tradeId: number;
  description: string;
  occurredAt: string;
};

export type SurveillanceCase = {
  id: number;
  alertId: number;
  proposedBy: string;
  status: "PROPOSED" | "APPROVED" | "REJECTED";
  reviewedBy: string | null;
  reviewNote: string | null;
  createdAt: string;
  reviewedAt: string | null;
};

export type AuditVerification = {
  valid: boolean;
  entriesChecked: number;
  brokenEntryId: number | null;
  reason: string | null;
  verifiedAt: string;
};

export type ReconciliationIssue = {
  code: string;
  symbol: string;
  message: string;
};

export type ReconciliationReport = {
  balanced: boolean;
  ordersChecked: number;
  tradesChecked: number;
  eventsChecked: number;
  issues: ReconciliationIssue[];
  reconciledAt: string;
};

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function api<T>(
  path: string,
  init: RequestInit = {},
  allowConflict = false,
): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(path, {
    ...init,
    headers,
    cache: "no-store",
    credentials: "same-origin",
  });

  if (!response.ok && !(allowConflict && response.status === 409)) {
    let message = `請求失敗 (HTTP ${response.status})`;
    try {
      const body = await response.json() as { message?: string; title?: string };
      message = body.message || body.title || message;
    } catch {
      // Keep the HTTP status message when the server does not return JSON.
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "操作失敗，請稍後再試。";
}

export function roleLabel(role: Role): string {
  return {
    TRADER: "交易員",
    SURVEILLANCE: "監控人員",
    AUDITOR: "稽核員",
    ADMIN: "管理員",
  }[role];
}
