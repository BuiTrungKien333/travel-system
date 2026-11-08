import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { TripSummary } from '../types';
import { tripSummary, type TripRecord } from './readContract';

export function useTripsQuery() {
  return useQuery({
    queryKey: QUERY_KEYS.trips(),
    queryFn: async (): Promise<TripSummary[]> => {
      const { data } = await apiClient.get<TripSummary[] | { content: TripRecord[] }>('/trips', { params: { size: 100 } });
      return Array.isArray(data) ? data : data.content.map(tripSummary);
    },
  });
}
