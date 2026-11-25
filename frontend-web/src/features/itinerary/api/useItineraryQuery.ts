import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import { mapItinerary, fetchTrip, type BackendItinerary } from "@/lib/backend";
import { QUERY_KEYS } from "@/lib/constants";
import type { TripPlace } from "@/features/place/types";
export function useItineraryQuery(tripId: string) {
  return useQuery({
    queryKey: QUERY_KEYS.itinerary(tripId),
    enabled: !!tripId,
    queryFn: async () => {
      const [itinerary, places, trip] = await Promise.all([
        apiClient.get<BackendItinerary>(`/trips/${tripId}/itinerary`),
        apiClient.get<TripPlace[]>(`/trips/${tripId}/places`),
        fetchTrip(tripId),
      ]);
      const dates = trip.days?.map((day) => day.date) ?? [];
      const result = mapItinerary({
        ...itinerary.data,
        days: dates.length
          ? dates.map(
              (date) =>
                itinerary.data.days.find((day) => day.date === date) ?? {
                  date,
                  stops: [],
                },
            )
          : itinerary.data.days,
      });
      const visited = new Set(
        places.data.filter((p) => p.visited).map((p) => p.placeId),
      );
      result.tripVersion = trip.version;
      result.readOnly = trip.mode === "RECAP" || trip.mode === "CANCELLED";
      result.editableFrom =
        trip.mode === "LIVE"
          ? new Intl.DateTimeFormat("en-CA", {
              timeZone: "Asia/Ho_Chi_Minh",
              year: "numeric",
              month: "2-digit",
              day: "2-digit",
            }).format(new Date())
          : undefined;
      result.days.forEach((day) =>
        day.items.forEach((item) => {
          item.visited = visited.has(item.placeId);
        }),
      );
      return result;
    },
  });
}
