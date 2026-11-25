import { useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import { QUERY_KEYS } from "@/lib/constants";

export function useRemovePlaceMutation(tripId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (placeId: string): Promise<void> => {
      await apiClient.delete(`/trips/${tripId}/places/${placeId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.trip(tripId) });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.trips() });
    },
  });
}
