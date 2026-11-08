import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';

export function useDeleteMemoryMutation(tripId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (memoryId: string): Promise<void> => {
      await apiClient.delete(`/memories/${memoryId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.memories(tripId) });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.journey() });
    },
  });
}
