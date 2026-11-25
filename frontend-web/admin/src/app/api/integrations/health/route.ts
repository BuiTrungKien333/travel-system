import { NextRequest } from "next/server";
import { backendRequest } from "@/server/backend";
import { accessToken, failure, json } from "@/server/http";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  try {
    return json(
      await backendRequest("/api/v1/admin/integrations/health", {
        token: accessToken(request),
      }),
    );
  } catch (error) {
    return failure(error);
  }
}
