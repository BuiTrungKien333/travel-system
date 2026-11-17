import { NextRequest } from "next/server";
import { backendRequest } from "@/server/backend";
import { accessToken, failure, json } from "@/server/http";
import { ApiError } from "@/lib/errors";
export const dynamic = "force-dynamic";
export async function GET(request: NextRequest) {
  try {
    const page = Number(request.nextUrl.searchParams.get("page") ?? 0);
    if (!Number.isSafeInteger(page) || page < 0 || page > 1000000)
      throw new ApiError("COMMON_003", 400);
    return json(
      await backendRequest(`/api/v1/admin/knowledge?page=${page}&size=20`, {
        token: accessToken(request),
      }),
    );
  } catch (e) {
    return failure(e);
  }
}
