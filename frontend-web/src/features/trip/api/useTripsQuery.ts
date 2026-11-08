import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { TripSummary } from '../types';

export function useTripsQuery() {
  return useQuery({
    queryKey: QUERY_KEYS.trips(),
    queryFn: async (): Promise<TripSummary[]> => {
      const { data } = await apiClient.get<TripSummary[]>('/trips');
      return data;
    },
  });
}
