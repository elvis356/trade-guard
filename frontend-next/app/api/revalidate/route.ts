import { timingSafeEqual } from "node:crypto";
import { revalidateTag } from "next/cache";

export const dynamic = "force-dynamic";

function matchesSecret(received: string | null, expected: string | undefined): boolean {
  if (!received || !expected) return false;
  const left = Buffer.from(received);
  const right = Buffer.from(expected);
  return left.length === right.length && timingSafeEqual(left, right);
}

export async function POST(request: Request) {
  const expectedSecret = process.env.REVALIDATE_SECRET;
  if (!matchesSecret(request.headers.get("x-revalidate-secret"), expectedSecret)) {
    return Response.json({ error: "Unauthorized" }, { status: 401 });
  }

  revalidateTag("daily-report");
  return Response.json({ revalidated: true, tag: "daily-report" });
}
