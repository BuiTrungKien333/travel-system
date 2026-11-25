import { useTripsQuery } from "@/features/trip/api/useTripsQuery";
export function useTripsWithMemoriesQuery() {
  const query = useTripsQuery();
  return { ...query, data: query.data?.filter((trip) => trip.memoryCount > 0) };
}
