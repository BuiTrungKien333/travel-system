import { NextRequest } from "next/server";
import { z } from "zod";
import { backendRequest } from "@/server/backend";
import {
  accessToken,
  assertSameOrigin,
  failure,
  json,
  readBody,
} from "@/server/http";
import { ApiError } from "@/lib/errors";
import { placeRequestSchema } from "@/features/overview/data";

const filters = z.object({
  page: z.coerce.number().int().min(0).max(1000000).default(0),
  provinceCode: z.string().regex(/^\d{2}$/).optional(),
  status: z.enum(["ALL", "PENDING", "APPROVED", "ARCHIVED"]).default("ALL"),
  tag: z.string().regex(/^[A-Za-z0-9_]{2,30}$/).optional(),
  q: z.string().max(200).optional(),
});

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  try {
    const parsed = filters.safeParse(
      Object.fromEntries(request.nextUrl.searchParams.entries()),
    );
    if (!parsed.success) throw new ApiError("COMMON_003", 400);
    const params = new URLSearchParams({
      page: String(parsed.data.page),
      size: "20",
      status: parsed.data.status,
    });
    if (parsed.data.provinceCode) params.set("provinceCode", parsed.data.provinceCode);
    if (parsed.data.tag) params.set("tag", parsed.data.tag);
    if (parsed.data.q?.trim()) params.set("q", parsed.data.q.trim());
    return json(
      await backendRequest(`/api/v1/admin/catalog?${params}`, {
        token: accessToken(request),
      }),
    );
  } catch (error) {
    return failure(error);
  }
}

export async function POST(request: NextRequest) {
  try {
    assertSameOrigin(request);
    const body = placeRequestSchema.safeParse(await readBody(request));
    if (!body.success) throw new ApiError("COMMON_003", 400);
    return json(
      await backendRequest("/api/v1/admin/catalog", {
        method: "POST",
        token: accessToken(request),
        body: body.data,
      }),
    );
  } catch (error) {
    return failure(error);
  }
}
