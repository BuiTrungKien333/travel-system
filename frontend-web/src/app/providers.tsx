"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { useState } from "react";
import { Toaster } from 'sonner';

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // Do not refetch on window focus in development to reduce noise
            refetchOnWindowFocus: process.env.NODE_ENV === "production",
            // Stale time: 30 seconds by default
            staleTime: 30_000,
            // Retry once on failure (external APIs may blip)
            retry: 1,
          },
          mutations: {
            // No retries for mutations to avoid double-submit
            retry: 0,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <Toaster richColors position="top-right" />
      {process.env.NODE_ENV === "development" && (
        <ReactQueryDevtools initialIsOpen={false} />
      )}
    </QueryClientProvider>
  );
}
