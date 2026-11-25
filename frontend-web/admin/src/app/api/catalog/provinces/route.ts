import { NextRequest } from "next/server";
import { z } from "zod";
import { backendRequest } from "@/server/backend";
import { accessToken, failure, json } from "@/server/http";
import { ApiError } from "@/lib/errors";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  try {
    const result = z
      .object({
        success: z.literal(true),
        data: z.array(z.object({ code: z.string(), name: z.string() })),
      })
      .safeParse(
        await backendRequest("/api/v1/admin/catalog/provinces", {
          token: accessToken(request),
        }),
      );
    if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);
    return json(result.data);
  } catch (error) {
    return failure(error);
  }
}
