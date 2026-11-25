import { useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import { QUERY_KEYS } from "@/lib/constants";
export function useVisitedMutation(tripId?: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (input: { placeId: string; visited: boolean }) => {
      if (!tripId) throw new Error("Thiếu chuyến đi.");
      await apiClient.put(`/trips/${tripId}/places/${input.placeId}`, {
        visited: input.visited,
      });
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: QUERY_KEYS.trip(tripId!) });
      client.invalidateQueries({ queryKey: ["home"] });
    },
  });
}
