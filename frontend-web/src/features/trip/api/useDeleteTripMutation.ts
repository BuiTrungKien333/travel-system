import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';

export function useDeleteTripMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (tripId: string): Promise<void> => {
      await apiClient.delete(`/trips/${tripId}`);
    },
    onSuccess: (_data, tripId) => {
      queryClient.removeQueries({ queryKey: QUERY_KEYS.trip(tripId) });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.trips() });
    },
  });
}
