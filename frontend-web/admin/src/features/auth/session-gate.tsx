"use client";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { ApiError, errorMessage } from "@/lib/errors";
import { invalidateClientSession } from "@/lib/client";
import { Banner, Button } from "@/components/ui";
import { useSession } from "./api";
import { AdminShell } from "@/components/admin-shell";

export function SessionGate({ children }: { children: React.ReactNode }) {
  const query = useSession();
  const router = useRouter();
  const cache = useQueryClient();
  const invalid =
    query.error instanceof ApiError && [401, 403].includes(query.error.status);
  useEffect(() => {
    if (invalid) {
      invalidateClientSession();
      void cache.cancelQueries();
      cache.clear();
      router.replace("/login?notice=expired");
      router.refresh();
    }
  }, [invalid, router, cache]);

  if (query.isPending || invalid)
    return (
      <main className="grid min-h-dvh place-items-center bg-canvas">
        <div
          role="status"
          className="rounded-2xl border border-line bg-white px-8 py-6 text-sm text-muted"
        >
          Đang xác thực quyền truy cập…
        </div>
      </main>
    );

  if (query.isError)
    return (
      <main className="mx-auto max-w-lg space-y-4 px-6 py-24">
        <Banner error>{errorMessage(query.error)}</Banner>
        <Button onClick={() => void query.refetch()}>Thử kết nối lại</Button>
      </main>
    );

  return <AdminShell session={query.data}>{children}</AdminShell>;
}
