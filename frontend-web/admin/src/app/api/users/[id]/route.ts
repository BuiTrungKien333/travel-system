import { NextRequest } from "next/server";
import { changeAccount } from "@/server/handlers";

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  return changeAccount(request, (await params).id);
}
