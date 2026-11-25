import { useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import { fetchTrip, mapTrip, tripPayload, type TripRead } from "@/lib/backend";
import { QUERY_KEYS } from "@/lib/constants";
import type { TripOverview, UpdateTripRequest } from "../types";
export function useUpdateTripMutation(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: UpdateTripRequest) => {
      const trip =
        queryClient.getQueryData<TripOverview>(QUERY_KEYS.trip(tripId)) ??
        (await fetchTrip(tripId));
      const { data } =
        input.status === "COMPLETED"
          ? await apiClient.post<TripRead>(`/trips/${tripId}/complete`, {
              version: input.version ?? trip.version,
            })
          : await apiClient.patch<TripRead>(`/trips/${tripId}`, {
              ...tripPayload(input),
              version: input.version ?? trip.version,
            });
      return mapTrip(data.trip);
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(QUERY_KEYS.trip(tripId), updated);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.trips() });
      queryClient.invalidateQueries({ queryKey: ["home"] });
      queryClient.invalidateQueries({ queryKey: ["journal"] });
    },
  });
}
