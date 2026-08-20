import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { JourneyStats } from '../types';

export function useJourneyQuery() {
  return useQuery({
    queryKey: QUERY_KEYS.journey(),
    queryFn: async (): Promise<JourneyStats> => {
      const { data } = await apiClient.get<JourneyStats>('/me/journey');
      return data;
    },
    // Province data rarely changes — use longer stale time
    staleTime: 60_000,
  });
}
