"use client";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { apiRequest } from "@/lib/client";
import { envelope } from "@/lib/contracts";
import { ApiError } from "@/lib/errors";
import {
  catalogPageSchema,
  integrationHealthSchema,
  integrationCheckSchema,
  overviewSchema,
  knowledgeSchema,
  placeRequestSchema,
  provinceOptionsSchema,
  ragSearchSchema,
  uploadedImageSchema,
  type PlaceRequest,
} from "./data";

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

export function useIntegrationHealth(
  owner: string | undefined,
  enabled: boolean,
) {
  return useQuery<z.infer<typeof integrationHealthSchema>>({
    queryKey: ["integration-health", owner],
    enabled,
    staleTime: 15000,
    refetchInterval: (query) => (query.state.data?.checking ? 2000 : 60000),
    queryFn: async ({ signal }) => {
      const result = envelope(integrationHealthSchema).safeParse(
        await apiRequest("/integrations/health", { signal }),
      );
      if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);

      return result.data.data;
    },
  });
}

export function useTriggerIntegrationHealthCheck(owner: string | undefined) {
  const cache = useQueryClient();
  const queryKey = ["integration-health", owner];

  return useMutation({
    retry: false,
    mutationFn: async () => {
      const result = envelope(integrationCheckSchema).safeParse(
        await apiRequest("/integrations/health/check", { method: "POST" }),
      );
      if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);

      return result.data.data;
    },
    onSuccess: (result) => {
      cache.setQueryData<z.infer<typeof integrationHealthSchema>>(
        queryKey,
        (current) =>
          current ? { ...current, checking: result.checking } : current,
      );
      void cache.invalidateQueries({ queryKey });
    },
  });
}

export function useKnowledge(
  owner: string | undefined,
  page: number,
  enabled: boolean,
  filters: { provinceCode: string; indexStatus: string } = {
    provinceCode: "",
    indexStatus: "",
  },
) {
  return useQuery({
    queryKey: ["knowledge", owner, page, filters],
    enabled,
    queryFn: async ({ signal }) => {
      const params = new URLSearchParams({ page: String(page) });
      if (filters.provinceCode)
        params.set("provinceCode", filters.provinceCode);
      if (filters.indexStatus) params.set("indexStatus", filters.indexStatus);
      const p = envelope(knowledgeSchema).safeParse(
        await apiRequest(`/knowledge?${params}`, { signal }),
      );
      if (!p.success) throw new ApiError("INVALID_RESPONSE", 502);

      return p.data.data;
    },
  });
}

export type CatalogFilters = {
  provinceCode: string;
  status: "ALL" | "PENDING" | "APPROVED" | "ARCHIVED";
  tag: string;
  q: string;
};

export function useAdminCatalog(
  owner: string | undefined,
  page: number,
  filters: CatalogFilters,
  enabled: boolean,
) {
  return useQuery({
    queryKey: ["admin-catalog", owner, page, filters],
    enabled,
    queryFn: async ({ signal }) => {
      const params = new URLSearchParams({
        page: String(page),
        status: filters.status,
      });
      if (filters.provinceCode)
        params.set("provinceCode", filters.provinceCode);
      if (filters.tag.trim()) params.set("tag", filters.tag.trim());
      if (filters.q.trim()) params.set("q", filters.q.trim());
      const result = envelope(catalogPageSchema).safeParse(
        await apiRequest(`/catalog?${params}`, { signal }),
      );
      if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);

      return result.data.data;
    },
  });
}

export function useCatalogProvinces(
  owner: string | undefined,
  enabled: boolean,
) {
  return useQuery({
    queryKey: ["catalog-provinces", owner],
    enabled,
    staleTime: 300000,
    queryFn: async ({ signal }) => {
      const result = envelope(provinceOptionsSchema).safeParse(
        await apiRequest("/catalog/provinces", { signal }),
      );
      if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);

      return result.data.data;
    },
  });
}

export async function uploadCatalogImage(file: File) {
  const form = new FormData();
  form.set("file", file);
  const result = envelope(uploadedImageSchema).safeParse(
    await apiRequest("/catalog/upload", {
      method: "POST",
      body: form,
      signal: AbortSignal.timeout(70000),
    }),
  );
  if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);

  return result.data.data;
}

type CatalogChange =
  | { type: "create"; input: PlaceRequest }
  | { type: "update"; id: string; version: number; input: PlaceRequest }
  | { type: "review"; id: string; version: number; approved: boolean }
  | { type: "archive" | "restore"; id: string; version: number };

export function useCatalogManagementAction() {
  const cache = useQueryClient();

  return useMutation({
    retry: false,
    mutationFn: async (change: CatalogChange) => {
      if (change.type === "create" || change.type === "update") {
        const body = placeRequestSchema.safeParse(change.input);
        if (!body.success) throw new ApiError("COMMON_003", 400);
        const path =
          change.type === "create"
            ? "/catalog"
            : `/catalog/${change.id}?version=${change.version}`;

        const result = envelope(
          catalogPageSchema.shape.content.element,
        ).safeParse(
          await apiRequest(path, {
            method: change.type === "create" ? "POST" : "PUT",
            body: body.data,
          }),
        );
        if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);

        return result.data.data;
      }

      if (change.type === "review") {
        await apiRequest(`/catalog/${change.id}`, {
          method: "PATCH",
          body: { approved: change.approved, version: change.version },
        });

        return null;
      }

      await apiRequest(
        change.type === "archive"
          ? `/catalog/${change.id}?version=${change.version}`
          : `/catalog/${change.id}/restore?version=${change.version}`,
        {
          method: change.type === "archive" ? "DELETE" : "POST",
        },
      );

      return null;
    },
    onSuccess: async () => {
      await Promise.all([
        cache.invalidateQueries({ queryKey: ["admin-catalog"] }),
        cache.invalidateQueries({ queryKey: ["knowledge"] }),
        cache.invalidateQueries({ queryKey: ["overview"] }),
      ]);
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
            model: z.string(),
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

export function useRagSearch() {
  return useMutation({
    retry: false,
    mutationFn: async (request: {
      positiveQuery: string;
      provinceCode?: string;
      excludedTags: string[];
      limit: number;
    }) => {
      const result = envelope(ragSearchSchema).safeParse(
        await apiRequest("/knowledge/search", {
          method: "POST",
          body: request,
        }),
      );
      if (!result.success) throw new ApiError("INVALID_RESPONSE", 502);

      return result.data.data;
    },
  });
}
