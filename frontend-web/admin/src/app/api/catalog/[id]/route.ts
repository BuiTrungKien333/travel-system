import { NextRequest } from "next/server";
import { z } from "zod";
import { idSchema } from "@/lib/contracts";
import { backendRequest } from "@/server/backend";
import {
  accessToken,
  assertSameOrigin,
  readBody,
  failure,
  json,
} from "@/server/http";
import { ApiError } from "@/lib/errors";
export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    const { id } = await params;
    if (!idSchema.safeParse(id).success) throw new ApiError("COMMON_003", 400);
    const body = z
      .object({ approved: z.boolean(), version: z.number().int().positive() })
      .strict()
      .safeParse(await readBody(request));
    if (!body.success) throw new ApiError("COMMON_003", 400);
    await backendRequest(`/api/v1/admin/catalog/${id}`, {
      method: "PATCH",
      token: accessToken(request),
      body: body.data,
    });
    return json({ success: true });
  } catch (e) {
    return failure(e);
  }
}
