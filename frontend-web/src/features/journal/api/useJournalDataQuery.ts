import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import {
  MOCK_JOURNEYS,
  MOCK_JOURNEY_STATS,
  ALL_MOCK_JOURNAL_MEMORIES,
} from '@/constants/mock-journal';
import type { Journey, TravelStatsData, Memory } from '../types';

export interface JournalDataResponse {
  journeys: Journey[];
  stats: TravelStatsData;
  recentMemories: Memory[];
}

interface ApiTripRecord {
  id: string;
  name?: string;
  title?: string;
  coverImage?: string;
  startDate?: string;
  endDate?: string;
  destination?: string;
  description?: string;
  status?: 'PLANNING' | 'READY' | 'ACTIVE' | 'COMPLETED';
}

export function useJournalDataQuery() {
  const isMockEnabled =
    typeof process !== 'undefined' &&
    (process.env.NEXT_PUBLIC_ENABLE_MOCK === 'true' ||
      !process.env.NEXT_PUBLIC_API_BASE_URL);

  return useQuery<JournalDataResponse>({
    queryKey: ['journal', 'data'],
    queryFn: async () => {
      if (isMockEnabled) {
        return {
          journeys: MOCK_JOURNEYS,
          stats: MOCK_JOURNEY_STATS,
          recentMemories: ALL_MOCK_JOURNAL_MEMORIES,
        };
      }

      try {
        // Attempt to fetch from backend endpoints
        const [statsRes, tripsRes] = await Promise.allSettled([
          apiClient.get<TravelStatsData>('/me/journey'),
          apiClient.get<ApiTripRecord[]>('/trips'),
        ]);

        const stats: TravelStatsData =
          statsRes.status === 'fulfilled' && statsRes.value.data
            ? statsRes.value.data
            : MOCK_JOURNEY_STATS;

        // If backend returned trips, map them into Journey shape
        let journeys: Journey[] = MOCK_JOURNEYS;
        if (tripsRes.status === 'fulfilled' && Array.isArray(tripsRes.value.data)) {
          const apiTrips = tripsRes.value.data;
          if (apiTrips.length > 0) {
            journeys = apiTrips.map((trip) => ({
              id: trip.id,
              title: trip.name || trip.title || 'Hành trình khám phá',
              coverImage:
                trip.coverImage ||
                'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=800',
              startDate: trip.startDate || '2026-06-01',
              endDate: trip.endDate || '2026-06-05',
              provinces: trip.destination ? [trip.destination] : ['Việt Nam'],
              description: trip.description || 'Chuyến đi lưu giữ những kỷ niệm đáng nhớ.',
              status: trip.status || 'COMPLETED',
              locations: [],
              memories: [],
            }));
          }
        }

        return {
          journeys,
          stats,
          recentMemories: ALL_MOCK_JOURNAL_MEMORIES,
        };
      } catch {
        // Fallback gracefully on any error
        return {
          journeys: MOCK_JOURNEYS,
          stats: MOCK_JOURNEY_STATS,
          recentMemories: ALL_MOCK_JOURNAL_MEMORIES,
        };
      }
    },
    staleTime: 5 * 60 * 1000,
  });
}
