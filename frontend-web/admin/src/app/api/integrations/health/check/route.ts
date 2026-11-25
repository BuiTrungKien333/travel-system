import { NextRequest } from "next/server";
import { backendRequest } from "@/server/backend";
import { accessToken, assertSameOrigin, failure, json } from "@/server/http";

export async function POST(request: NextRequest) {
  try {
    assertSameOrigin(request);

    return json(
      await backendRequest("/api/v1/admin/integrations/health/check", {
        method: "POST",
        token: accessToken(request),
      }),
    );
  } catch (error) {
    return failure(error);
  }
}
