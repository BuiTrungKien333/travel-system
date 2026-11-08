import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import type { TripOverview } from '@/features/trip/types';

/**
 * Fetches trips that have at least 1 memory for the Memory Hub page.
 */
export function useTripsWithMemoriesQuery() {
  return useQuery({
    queryKey: ['me', 'trips-with-memories'] as const,
    queryFn: async (): Promise<TripOverview[]> => {
      const { data } = await apiClient.get<TripOverview[]>('/me/trips-with-memories');
      return data;
    },
    staleTime: 30_000,
  });
}
