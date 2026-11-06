import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { TripOverview } from '../types';

export function useTripQuery(tripId: string) {
  return useQuery({
    queryKey: QUERY_KEYS.trip(tripId),
    queryFn: async (): Promise<TripOverview> => {
      const { data } = await apiClient.get<TripOverview>(`/trips/${tripId}`);
      return data;
    },
    enabled: !!tripId,
  });
}
