import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { Memory } from '../types';

/**
 * Creates a Memory with optional photos.
 * Flow: Frontend → POST /trips/{tripId}/memories (multipart) → Backend → Media Storage → CDN URL
 */
export function useCreateMemoryMutation(tripId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (formData: FormData): Promise<Memory> => {
      const { data } = await apiClient.post<Memory>(
        `/trips/${tripId}/memories`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.memories(tripId) });
      // Journey stats may change after new Memory (Province explored rule)
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.journey() });
    },
  });
}
