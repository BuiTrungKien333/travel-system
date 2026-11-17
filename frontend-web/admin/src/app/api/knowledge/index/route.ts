import { NextRequest } from "next/server";
import { backendRequest } from "@/server/backend";
import { accessToken, assertSameOrigin, failure, json } from "@/server/http";
export async function POST(request: NextRequest) {
  try {
    assertSameOrigin(request);
    return json(
      await backendRequest("/api/v1/admin/planning/retrieval/index", {
        method: "POST",
        token: accessToken(request),
      }),
    );
  } catch (e) {
    return failure(e);
  }
}
