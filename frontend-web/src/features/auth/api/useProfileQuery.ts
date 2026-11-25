import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import { QUERY_KEYS } from "@/lib/constants";
import { useAuthStore } from "../store/useAuthStore";
import type { User } from "../types";
export function useProfileQuery() {
  const enabled = useAuthStore((s) => s.isAuthenticated);
  return useQuery({
    queryKey: QUERY_KEYS.me(),
    enabled,
    queryFn: async () => {
      const { data } = await apiClient.get<User>("/users/me");
      return data;
    },
  });
}
export function useUpdateProfileMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: { fullName: string; avatarUrl?: string }) => {
      const { data } = await apiClient.put<User>("/users/me", input);
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(QUERY_KEYS.me(), data);
      useAuthStore.getState().updateUser(data);
    },
  });
}
