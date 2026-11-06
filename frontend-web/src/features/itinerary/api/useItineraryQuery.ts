import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { Itinerary } from '../types';

export function useItineraryQuery(tripId: string) {
  return useQuery({
    queryKey: QUERY_KEYS.itinerary(tripId),
    queryFn: async (): Promise<Itinerary> => {
      const { data } = await apiClient.get<Itinerary>(`/trips/${tripId}/itinerary`);
      return data;
    },
    enabled: !!tripId,
  });
}
