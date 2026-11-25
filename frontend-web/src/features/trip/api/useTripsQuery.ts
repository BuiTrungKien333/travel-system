import { useQuery } from "@tanstack/react-query";
import { fetchTrips } from "@/lib/backend";
import { QUERY_KEYS } from "@/lib/constants";
export function useTripsQuery() {
  return useQuery({ queryKey: QUERY_KEYS.trips(), queryFn: fetchTrips });
}
