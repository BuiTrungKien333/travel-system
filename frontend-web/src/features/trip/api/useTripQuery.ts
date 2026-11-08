import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { TripOverview } from '../types';
import { tripOverview, type TripRecord } from './readContract';
import { useAuthStore } from '@/features/auth/store/useAuthStore';

export function useTripQuery(tripId: string) {
  return useQuery({
    queryKey: QUERY_KEYS.trip(tripId),
    queryFn: async (): Promise<TripOverview> => {
      const { data } = await apiClient.get<TripOverview | { trip: TripRecord }>(`/trips/${tripId}`);
      if (!('trip' in data)) return data;
      const places = await apiClient.get<Array<{ visited: boolean }>>(`/trips/${tripId}/places`);
      return tripOverview(data.trip, useAuthStore.getState().user?.id ?? '', places.data.filter(p => p.visited).length);
    },
    enabled: !!tripId,
  });
}
