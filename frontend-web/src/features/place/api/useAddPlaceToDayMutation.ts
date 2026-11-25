import { useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import { QUERY_KEYS } from "@/lib/constants";

export function useAddPlaceToDayMutation(tripId: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async ({
      placeId,
      date,
    }: {
      placeId: string;
      date: string;
    }) => {
      if (!date) throw new Error("Vui lòng chọn ngày cần thêm địa điểm.");
      // Membership and the selected day's schedule are committed together by the backend.
      await apiClient.post(`/trips/${tripId}/places/batch`, {
        placeIds: [placeId],
        date,
      });
    },
    onSettled: () => {
      client.invalidateQueries({ queryKey: QUERY_KEYS.trip(tripId) });
      client.invalidateQueries({ queryKey: QUERY_KEYS.trips() });
      client.invalidateQueries({ queryKey: ["places", "suggestions"] });
      client.invalidateQueries({ queryKey: ["home"] });
    },
  });
}
