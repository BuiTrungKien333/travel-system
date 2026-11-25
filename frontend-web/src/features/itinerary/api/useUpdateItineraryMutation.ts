import { useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import {
  mapItinerary,
  itineraryPayload,
  type BackendItinerary,
} from "@/lib/backend";
import { QUERY_KEYS } from "@/lib/constants";
import { useItineraryUIStore } from "../store/useItineraryUIStore";
import type { Itinerary, AIItineraryRecommendation } from "../types";
export function useUpdateItineraryMutation(tripId: string) {
  const queryClient = useQueryClient();
  const setPendingRecommendation = useItineraryUIStore(
    (s) => s.setPendingRecommendation,
  );
  return useMutation({
    mutationFn: async (
      input: Partial<Itinerary> | AIItineraryRecommendation,
    ) => {
      const optimization =
        "optimization" in input ? input.optimization : undefined;
      const { data } = optimization
        ? await apiClient.post<BackendItinerary>(
            `/trips/${tripId}/itinerary/apply-optimization`,
            {
              expectedUpdatedAt: optimization.sourceUpdatedAt,
              proposal: optimization.proposal,
            },
          )
        : await apiClient.put<BackendItinerary>(
            `/trips/${tripId}/itinerary`,
            itineraryPayload(input as Partial<Itinerary>),
          );
      return mapItinerary(data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.trip(tripId) });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.trips() });
      setPendingRecommendation(null);
    },
    onError: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.trip(tripId) });
    },
  });
}
