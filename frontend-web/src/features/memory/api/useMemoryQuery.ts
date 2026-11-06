import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { Memory } from '../types';

export function useMemoryQuery(tripId: string) {
  return useQuery({
    queryKey: QUERY_KEYS.memories(tripId),
    queryFn: async (): Promise<Memory[]> => {
      const { data } = await apiClient.get<Memory[]>(`/trips/${tripId}/memories`);
      return data;
    },
    enabled: !!tripId,
  });
}
