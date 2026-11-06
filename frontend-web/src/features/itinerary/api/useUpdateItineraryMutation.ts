import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import { useItineraryUIStore } from '../store/useItineraryUIStore';
import type { Itinerary } from '../types';

export function useUpdateItineraryMutation(tripId: string) {
  const queryClient = useQueryClient();
  const setPendingRecommendation = useItineraryUIStore(
    (s) => s.setPendingRecommendation
  );

  return useMutation({
    mutationFn: async (updatedItinerary: Partial<Itinerary>): Promise<Itinerary> => {
      const { data } = await apiClient.put<Itinerary>(
        `/trips/${tripId}/itinerary`,
        updatedItinerary
      );
      return data;
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(QUERY_KEYS.itinerary(tripId), updated);
      setPendingRecommendation(null);
    },
  });
}
