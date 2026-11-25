import { NextRequest } from "next/server";
import { backendRequest } from "@/server/backend";
import { accessToken, failure, json } from "@/server/http";
import { ApiError } from "@/lib/errors";
import { z } from "zod";
export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  try {
    const query = z
      .object({
        page: z.coerce.number().int().min(0).max(1000000).default(0),
        provinceCode: z
          .string()
          .regex(/^\d{2}$/)
          .optional(),
        indexStatus: z
          .enum(["CURRENT", "STALE", "MISSING", "OUT_OF_SCOPE"])
          .optional(),
      })
      .safeParse(Object.fromEntries(request.nextUrl.searchParams.entries()));
    if (!query.success) throw new ApiError("COMMON_003", 400);
    const params = new URLSearchParams({
      page: String(query.data.page),
      size: "20",
    });
    if (query.data.provinceCode)
      params.set("provinceCode", query.data.provinceCode);
    if (query.data.indexStatus)
      params.set("indexStatus", query.data.indexStatus);

    return json(
      await backendRequest(`/api/v1/admin/knowledge?${params}`, {
        token: accessToken(request),
      }),
    );
  } catch (e) {
    return failure(e);
  }
}
