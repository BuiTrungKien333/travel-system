import { useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import { fetchTrip } from "@/lib/backend";
import type { TripOverview } from "../types";
import { QUERY_KEYS } from "@/lib/constants";

export function useDeleteTripMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (tripId: string): Promise<void> => {
      const trip =
        queryClient.getQueryData<TripOverview>(QUERY_KEYS.trip(tripId)) ??
        (await fetchTrip(tripId));
      await apiClient.delete(`/trips/${tripId}`, {
        params: { version: trip.version },
      });
    },
    onSuccess: (_data, tripId) => {
      queryClient.removeQueries({ queryKey: QUERY_KEYS.trip(tripId) });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.trips() });
      queryClient.invalidateQueries({ queryKey: ["journal"] });
      queryClient.invalidateQueries({ queryKey: ["home"] });
    },
  });
}
