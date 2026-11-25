import type { Itinerary } from "./types";
import type { ItineraryConnections } from "./api/useItineraryConnectionsQuery";
import type { RoutePosition } from "@/features/map/navigation";

const COLORS = ["#2563eb", "#ea580c", "#9333ea", "#0d9488", "#db2777"];
export function itineraryDayColor(dayIndex: number) {
  return COLORS[dayIndex % COLORS.length];
}

export function itineraryRouteGroups(
  itinerary: Itinerary | undefined,
  connections?: ItineraryConnections,
) {
  return (itinerary?.days ?? [])
    .filter((day) => day.items.length > 1)
    .map((day) => {
      const connected =
        connections && connections.itinerary.updatedAt === itinerary?.updatedAt
          ? connections.days.find((result) => result.date === day.date)
          : undefined;
      const estimated = !connected?.route;
      const points: RoutePosition[] = day.items
        .filter(
          (item) =>
            Number.isFinite(item.placeLat) && Number.isFinite(item.placeLng),
        )
        .map((item) => ({
          latitude: item.placeLat!,
          longitude: item.placeLng!,
        }));
      return {
        id: day.date,
        color: itineraryDayColor(day.dayIndex),
        estimated,
        geometry: estimated
          ? points.length === day.items.length
            ? [points]
            : []
          : connected.route!.legs.map((leg) => leg.geometry),
      };
    });
}
