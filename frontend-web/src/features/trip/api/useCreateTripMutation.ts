import { useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import { mapTrip, tripPayload, type TripRead } from "@/lib/backend";
import { QUERY_KEYS } from "@/lib/constants";
import type { CreateTripRequest } from "../types";
export function useCreateTripMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (
      input: CreateTripRequest & { clientRequestId: string },
    ) => {
      const { data } = await apiClient.post<TripRead>("/trips", {
        ...tripPayload(input),
        clientRequestId: input.clientRequestId,
        timeZone: "Asia/Ho_Chi_Minh",
      });
      return mapTrip(data.trip);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.trips() });
      queryClient.invalidateQueries({ queryKey: ["journal"] });
      queryClient.invalidateQueries({ queryKey: ["home"] });
    },
  });
}
