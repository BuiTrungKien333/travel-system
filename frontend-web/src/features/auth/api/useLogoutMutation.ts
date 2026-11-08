import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { useAuthStore } from '../store/useAuthStore';
import { useMemoryUIStore } from '@/features/memory/store/useMemoryUIStore';

export function useLogoutMutation() {
  const queryClient = useQueryClient();
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const refreshToken = useAuthStore((s) => s.refreshToken);

  return useMutation({
    mutationFn: async () => {
      if (refreshToken) await apiClient.post('/auth/logout', { refreshToken });
    },
    onSettled: () => {
      const memories = useMemoryUIStore.getState();
      memories.closeLightbox(); memories.setIsUploadOpen(false); memories.setActiveTrip(null);
      clearAuth();
      queryClient.clear();
    },
  });
}
