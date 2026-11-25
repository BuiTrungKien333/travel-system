import { NextRequest } from "next/server";
import { idSchema } from "@/lib/contracts";
import { ApiError } from "@/lib/errors";
import { backendRequest } from "@/server/backend";
import { accessToken, assertSameOrigin, failure, json } from "@/server/http";

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  try {
    assertSameOrigin(request);
    const { id } = await params;
    if (!idSchema.safeParse(id).success) throw new ApiError("COMMON_003", 400);
    const version = Number(request.nextUrl.searchParams.get("version"));
    if (!Number.isSafeInteger(version) || version < 1)
      throw new ApiError("COMMON_003", 400);

    return json(
      await backendRequest(
        `/api/v1/admin/catalog/${id}/restore?version=${version}`,
        { method: "POST", token: accessToken(request) },
      ),
    );
  } catch (error) {
    return failure(error);
  }
}
