import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { SessionGate } from "@/features/auth/session-gate";
import { cookieNames } from "@/server/http";
export const dynamic = "force-dynamic";

export default async function ConsoleLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jar = await cookies();
  if (!jar.has(cookieNames.access) && !jar.has(cookieNames.refresh))
    redirect("/login");

  // Cookies are only a session hint. Every data endpoint verifies current rights at the backend.
  return <SessionGate>{children}</SessionGate>;
}
