import { NextRequest } from "next/server";
import { backendRequest } from "@/server/backend";
import { accessToken, failure, json } from "@/server/http";
import { ApiError } from "@/lib/errors";
export const dynamic = "force-dynamic";
export async function GET(request: NextRequest) {
  try {
    const days = Number(request.nextUrl.searchParams.get("days") ?? 30);
    if (![7, 30, 90].includes(days)) throw new ApiError("COMMON_003", 400);
    return json(
      await backendRequest(`/api/v1/admin/overview?days=${days}`, {
        token: accessToken(request),
      }),
    );
  } catch (e) {
    return failure(e);
  }
}
