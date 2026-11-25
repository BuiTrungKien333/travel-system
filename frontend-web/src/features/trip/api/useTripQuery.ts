import { useQuery } from "@tanstack/react-query";
import { fetchTrip } from "@/lib/backend";
import { QUERY_KEYS } from "@/lib/constants";
export function useTripQuery(tripId: string) {
  return useQuery({
    queryKey: QUERY_KEYS.trip(tripId),
    queryFn: () => fetchTrip(tripId),
    enabled: !!tripId,
  });
}
