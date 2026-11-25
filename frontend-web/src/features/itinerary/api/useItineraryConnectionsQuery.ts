import { useEffect, useRef } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import { QUERY_KEYS } from "@/lib/constants";
import type { BackendItinerary } from "@/lib/backend";
import type { NavigationRoute } from "@/features/map/navigation";
import type { Itinerary } from "../types";

export interface ItineraryConnections {
  itinerary: BackendItinerary;
  days: {
    date: string;
    optimization: string;
    route: NavigationRoute | null;
    routeErrorCode: string | null;
  }[];
}

export function useItineraryConnectionsQuery(
  tripId: string,
  itinerary?: Itinerary,
) {
  const client = useQueryClient();
  const optimizedMembership = useRef("");
  const membershipOf = (plan?: Itinerary) =>
    JSON.stringify([
      tripId,
      plan?.days.map((day) => [
        day.date,
        day.items.map((item) => item.placeId).sort(),
      ]),
    ]);
  const membership = membershipOf(itinerary);
  const query = useQuery({
    queryKey: [
      ...QUERY_KEYS.itineraryConnections(tripId),
      itinerary?.tripVersion,
      itinerary?.updatedAt,
    ],
    enabled: !!itinerary && itinerary.days.some((day) => day.items.length > 1),
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnMount: "always",
    queryFn: async ({ signal }) => {
      const { data } = await apiClient.post<ItineraryConnections>(
        `/trips/${tripId}/itinerary/connect`,
        {
          expectedTripVersion: itinerary!.tripVersion,
          expectedUpdatedAt: itinerary!.updatedAt || null,
          optimize: optimizedMembership.current !== membership,
          vehicle: "DRIVE",
        },
        { signal, timeout: 120_000 },
      );
      optimizedMembership.current = membership;
      return data;
    },
  });
  useEffect(() => {
    if (
      (query.data && query.data.itinerary.updatedAt !== itinerary?.updatedAt) ||
      query.error
    ) {
      client.invalidateQueries({
        queryKey: QUERY_KEYS.itinerary(tripId),
        exact: true,
      });
    }
  }, [query.data, query.error, itinerary?.updatedAt, client, tripId]);
  return {
    ...query,
    preserveManualOrder: (next: Itinerary) => {
      optimizedMembership.current = membershipOf(next);
    },
  };
}
