import { NextRequest } from "next/server";
import { ApiError } from "@/lib/errors";
import { backendRequest } from "@/server/backend";
import { accessToken, assertSameOrigin, failure, json } from "@/server/http";

const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const IMAGE_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/gif",
  "image/webp",
]);

export async function POST(request: NextRequest) {
  try {
    assertSameOrigin(request);
    const incoming = await request.formData();
    const file = incoming.get("file");
    if (!(file instanceof File) || !file.size || !IMAGE_TYPES.has(file.type))
      throw new ApiError("UPLOAD_003", 400);
    if (file.size > MAX_IMAGE_BYTES) throw new ApiError("UPLOAD_002", 400);

    const form = new FormData();
    form.set("file", file, file.name);
    const result = await backendRequest("/api/v1/admin/catalog/images/upload", {
      method: "POST",
      token: accessToken(request),
      body: form,
      timeoutMillis: 60000,
    });

    return json(result, 201);
  } catch (error) {
    return failure(error);
  }
}
