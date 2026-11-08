import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { invalidateMemories } from './invalidateMemories';

export function useDeleteMemoryMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => { await apiClient.delete(`/journal/memories/${id}`); },
    onSuccess: () => invalidateMemories(client),
  });
}
