import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { useAuthStore } from '../store/useAuthStore';
import { QUERY_KEYS } from '@/lib/constants';
import type { LoginInput } from '../schema';
import type { LoginResponse } from '../types';

export function useLoginMutation() {
  const queryClient = useQueryClient();
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: async (input: LoginInput): Promise<LoginResponse> => {
      const { data } = await apiClient.post<LoginResponse>('/auth/login', input);
      return data;
    },
    onSuccess: (data) => {
      setAuth(data.user, data.accessToken, data.refreshToken);
      queryClient.setQueryData(QUERY_KEYS.me(), data.user);
    },
  });
}
