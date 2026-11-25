"use client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/lib/client";
import { ApiError } from "@/lib/errors";
import {
  accountPageSchema,
  envelope,
  type AccountChange,
} from "@/lib/contracts";

export function useAccounts(
  owner: string | undefined,
  q: string,
  page: number,
  enabled: boolean,
) {
  return useQuery({
    queryKey: ["admin-users", owner, q, page],
    queryFn: async ({ signal }) => {
      const params = new URLSearchParams({ q, page: String(page), size: "20" });
      const parsed = envelope(accountPageSchema).safeParse(
        await apiRequest(`/users?${params}`, { signal }),
      );
      if (!parsed.success) throw new ApiError("INVALID_RESPONSE", 502);

      return parsed.data.data;
    },
    enabled,
    staleTime: 10000,
  });
}

export function useChangeAccount(owner: string) {
  const cache = useQueryClient();

  return useMutation({
    mutationFn: ({ id, change }: { id: string; change: AccountChange }) =>
      apiRequest(`/users/${id}`, { method: "PATCH", body: change }),
    onSuccess: async () => {
      await cache.invalidateQueries({ queryKey: ["admin-users", owner] });
    },
  });
}
