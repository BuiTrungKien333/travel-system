import { z } from "zod";
import { NextRequest } from "next/server";
import { ApiError } from "@/lib/errors";
import { backendRequest } from "@/server/backend";
import {
  accessToken,
  assertSameOrigin,
  failure,
  json,
  readBody,
} from "@/server/http";

const requestSchema = z.object({
  positiveQuery: z.string().trim().min(1).max(1000),
  provinceCode: z.string().regex(/^\d{2}$/).optional(),
  excludedTags: z
    .array(z.string().regex(/^[A-Z_]{2,30}$/))
    .max(10)
    .default([]),
  limit: z.number().int().min(1).max(8).default(8),
});

export async function POST(request: NextRequest) {
  try {
    assertSameOrigin(request);
    const body = requestSchema.safeParse(await readBody(request));
    if (!body.success) throw new ApiError("COMMON_003", 400);
    return json(
      await backendRequest("/api/v1/admin/planning/retrieval/search", {
        method: "POST",
        token: accessToken(request),
        body: body.data,
      }),
    );
  } catch (error) {
    return failure(error);
  }
}
