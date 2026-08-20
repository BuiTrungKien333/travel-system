import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { UpdateTripRequest, Trip } from '../types';

export function useUpdateTripMutation(tripId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: UpdateTripRequest): Promise<Trip> => {
      const { data } = await apiClient.put<Trip>(`/trips/${tripId}`, input);
      return data;
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(QUERY_KEYS.trip(tripId), updated);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.trips() });
    },
  });
}
