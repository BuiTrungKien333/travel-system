"use client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import {
  apiRequest,
  announceSession,
  invalidateClientSession,
  openClientSession,
  signIn,
  signOut,
} from "@/lib/client";
import { ApiError } from "@/lib/errors";
import { envelope, sessionSchema } from "@/lib/contracts";
export const sessionKey = ["admin-session"] as const;
export function useSession() {
  return useQuery({
    queryKey: sessionKey,
    queryFn: async ({ signal }) => {
      const parsed = envelope(sessionSchema).safeParse(
        await apiRequest("/auth/session", { signal }),
      );
      if (!parsed.success) throw new ApiError("INVALID_RESPONSE", 502);
      return parsed.data.data;
    },
    staleTime: 0,
    refetchInterval: 60000,
  });
}
export function useSignIn() {
  const cache = useQueryClient();
  const router = useRouter();
  return useMutation({
    mutationFn: async (values: { email: string; password: string }) => {
      const parsed = envelope(sessionSchema).safeParse(await signIn(values));
      if (!parsed.success) throw new ApiError("INVALID_RESPONSE", 502);
      return parsed.data.data;
    },
    onSuccess: async (user) => {
      await cache.cancelQueries();
      cache.clear();
      cache.setQueryData(sessionKey, user);
      announceSession("login");
      router.replace("/dashboard");
      router.refresh();
    },
  });
}
export function useSignOut() {
  const cache = useQueryClient();
  const router = useRouter();
  return useMutation({
    mutationFn: signOut,
    onMutate: () => cache.cancelQueries(),
    onError: () => openClientSession(),
    onSuccess: async ({ revoked }) => {
      invalidateClientSession();
      await cache.cancelQueries();
      cache.clear();
      router.replace(revoked ? "/login" : "/login?notice=logout-incomplete");
      router.refresh();
    },
  });
}
