import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import type { Memory } from '@/features/memory/types';

export interface MemoryWithTrip extends Memory {
  tripName?: string;
}

/**
 * Fetches ALL memories across ALL trips for the Memory Hub page.
 * This is an aggregate view — not tied to a single trip.
 */
export function useAllMemoriesQuery() {
  return useQuery({
    queryKey: ['me', 'memories'] as const,
    queryFn: async (): Promise<MemoryWithTrip[]> => {
      const { data } = await apiClient.get<MemoryWithTrip[]>('/me/memories');
      return data;
    },
    staleTime: 30_000,
  });
}
