"use client";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { apiRequest } from "@/lib/client";
import { envelope } from "@/lib/contracts";
import { ApiError } from "@/lib/errors";
import { overviewSchema, knowledgeSchema } from "./data";
export function useOverview(
  owner: string | undefined,
  days: number,
  enabled: boolean,
) {
  return useQuery({
    queryKey: ["overview", owner, days],
    enabled,
    staleTime: 60000,
    queryFn: async ({ signal }) => {
      const p = envelope(overviewSchema).safeParse(
        await apiRequest(`/overview?days=${days}`, { signal }),
      );
      if (!p.success) throw new ApiError("INVALID_RESPONSE", 502);
      return p.data.data;
    },
  });
}
export function useKnowledge(
  owner: string | undefined,
  page: number,
  enabled: boolean,
) {
  return useQuery({
    queryKey: ["knowledge", owner, page],
    enabled,
    queryFn: async ({ signal }) => {
      const p = envelope(knowledgeSchema).safeParse(
        await apiRequest(`/knowledge?page=${page}`, { signal }),
      );
      if (!p.success) throw new ApiError("INVALID_RESPONSE", 502);
      return p.data.data;
    },
  });
}
export function useCatalogAction() {
  const cache = useQueryClient();
  return useMutation({
    retry: false,
    mutationFn: async (
      action: { id: string; approved: boolean; version: number } | "index",
    ) => {
      if (action === "index") {
        const result = envelope(
          z.object({
            updated: z.number(),
            skipped: z.number(),
            failed: z.number(),
          }),
        ).safeParse(await apiRequest("/knowledge/index", { method: "POST" }));
        if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);
        return result.data.data;
      }
      await apiRequest(`/catalog/${action.id}`, {
        method: "PATCH",
        body: { approved: action.approved, version: action.version },
      });
      return null;
    },
    onSuccess: async () => {
      await Promise.all([
        cache.invalidateQueries({ queryKey: ["knowledge"] }),
        cache.invalidateQueries({ queryKey: ["overview"] }),
      ]);
    },
  });
}
