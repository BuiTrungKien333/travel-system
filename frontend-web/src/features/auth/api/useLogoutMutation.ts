import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { useAuthStore } from '../store/useAuthStore';

export function useLogoutMutation() {
  const queryClient = useQueryClient();
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const refreshToken = useAuthStore((s) => s.refreshToken);

  return useMutation({
    mutationFn: async () => {
      if (refreshToken) await apiClient.post('/auth/logout', { refreshToken });
    },
    onSettled: () => {
      clearAuth();
      queryClient.clear();
    },
  });
}
