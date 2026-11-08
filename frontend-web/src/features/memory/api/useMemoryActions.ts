import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { invalidateMemories } from './invalidateMemories';
import { toMemory, type MemoryRecord } from './contract';

export function useUpdateMemoryMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...request }: { id: string; version: number; note?: string | null; experiencedAt?: string; mediaOrder?: string[] }) => {
      const { data } = await apiClient.patch<MemoryRecord>(`/journal/memories/${id}`, request);
      return toMemory(data);
    },
    onSuccess: () => invalidateMemories(client),
  });
}

export function useDeleteMediaMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => { await apiClient.delete('/media', { params: { id } }); },
    onSuccess: () => invalidateMemories(client),
  });
}

export async function getMemory(id: string) {
  const { data } = await apiClient.get<MemoryRecord>(`/journal/memories/${id}`);
  return toMemory(data);
}
