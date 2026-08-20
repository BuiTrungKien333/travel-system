import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { TripPlace } from '../types';

export function useTripPlacesQuery(tripId: string) {
  return useQuery({
    queryKey: QUERY_KEYS.tripPlaces(tripId),
    queryFn: async (): Promise<TripPlace[]> => {
      const { data } = await apiClient.get<TripPlace[]>(`/trips/${tripId}/places`);
      return data;
    },
    enabled: !!tripId,
  });
}
