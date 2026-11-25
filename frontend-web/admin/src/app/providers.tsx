"use client";
import { useEffect, useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ApiError } from "@/lib/errors";
import { invalidateClientSession, openClientSession } from "@/lib/client";

export function Providers({ children }: { children: React.ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: (count, error) =>
              count < 1 && error instanceof ApiError && error.status >= 500,
            refetchOnWindowFocus: true,
          },
          mutations: { retry: false },
        },
      }),
  );

  useEffect(() => {
    if (typeof BroadcastChannel === "undefined") return;
    const channel = new BroadcastChannel("wego-admin-session");
    channel.onmessage = (event: MessageEvent<unknown>) => {
      if (event.data !== "logout" && event.data !== "login") return;
      if (event.data === "logout") invalidateClientSession();
      else openClientSession();
      void client.cancelQueries();
      client.clear();
      window.location.replace(
        event.data === "logout" ? "/admin/login" : "/admin/dashboard",
      );
    };

    return () => channel.close();
  }, [client]);

  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
