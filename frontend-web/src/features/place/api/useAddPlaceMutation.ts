import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { AddPlaceRequest, TripPlace } from '../types';

export function useAddPlaceMutation(tripId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: AddPlaceRequest): Promise<TripPlace> => {
      const { data } = await apiClient.post<TripPlace>(`/trips/${tripId}/places`, input);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.tripPlaces(tripId) });
    },
  });
}
