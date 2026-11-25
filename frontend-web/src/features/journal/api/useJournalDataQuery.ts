import { useQuery } from "@tanstack/react-query";
import apiClient, { ApiError } from "@/lib/api";
import type { HomeDashboard } from "@/features/trip/components/HomeDashboardSummary";
import type { Destination } from "@/features/place/api/useDestinationsQuery";
import { fetchTrips, fetchMemories, getAllPages } from "@/lib/backend";
import type {
  Journey,
  TravelLocation,
  TravelStatsData,
  Memory,
} from "../types";
export interface JournalDataResponse {
  journeys: Journey[];
  stats: TravelStatsData;
  recentMemories: Memory[];
}
interface PlaceDetail {
  id: string;
  name: string;
  formattedAddress: string;
  provinceId?: string;
  location: { latitude: number; longitude: number };
}
export async function fetchJournalData(): Promise<JournalDataResponse> {
  const [trips, memories, dashboard, destinations] = await Promise.all([
    fetchTrips(),
    fetchMemories(),
    apiClient.get<HomeDashboard>("/home/dashboard"),
    getAllPages<Destination>("/destinations"),
  ]);
  // Resolve memory-place identity directly: removing trip membership must not erase its history.
  // shortcut: aggregate existing endpoints until the backend provides a journal summary API.
  const placeIds = [
    ...new Set(memories.flatMap((m) => (m.placeId ? [m.placeId] : []))),
  ];
  const places = new Map(
    await Promise.all(
      placeIds.map(async (id) => {
        try {
          const { data } = await apiClient.get<PlaceDetail>(`/places/${id}`);
          return [id, data] as const;
        } catch (error) {
          // Archived/unavailable places must not hide memories that still belong to the user.
          if (error instanceof ApiError && error.status === 404)
            return [id, null] as const;
          throw error;
        }
      }),
    ),
  );
  const provinces = new Map<
    string,
    { id: string; code: string; name: string }
  >();
  const tripById = new Map(trips.map((trip) => [trip.id, trip]));
  const mappedMemories: Memory[] = memories.map((memory) => {
    const trip = tripById.get(memory.tripId);
    const place = memory.placeId ? places.get(memory.placeId) : undefined;
    const province = destinations.find(
      (destination) => destination.id === place?.provinceId,
    );
    if (province) provinces.set(province.id, province);
    return {
      id: memory.id,
      title: memory.note || place?.name || trip?.name || "Kỷ niệm",
      image: memory.coverMediaUrl ?? "",
      images: memory.media.map((media) => media.url),
      location:
        place?.name ??
        (memory.placeId
          ? "Địa điểm không còn khả dụng"
          : (trip?.destination ?? "Không gắn địa điểm")),
      locationId: memory.placeId,
      province: province?.name,
      createdAt: memory.experiencedAt ?? memory.createdAt,
      journeyId: trip?.id ?? "unassigned-memories",
      journeyTitle: trip?.name ?? "Kỷ niệm khác",
      note: memory.note,
      mediaCount: memory.mediaCount,
    };
  });
  const locationsFor = (journeyId: string): TravelLocation[] => {
    const related = mappedMemories.filter(
      (memory) => memory.journeyId === journeyId,
    );
    return [
      ...new Set(
        related.flatMap((memory) =>
          memory.locationId ? [memory.locationId] : [],
        ),
      ),
    ].flatMap((id) => {
      const place = places.get(id);
      if (!place) return [];
      const entries = related.filter((memory) => memory.locationId === id);
      return [
        {
          id,
          name: place.name,
          address: place.formattedAddress,
          ...place.location,
          journeyId,
          journeyTitle: entries[0].journeyTitle,
          province: entries[0].province,
          visitedAt: entries[0].createdAt,
          memoryCount: entries.length,
          image: entries[0].image,
        },
      ];
    });
  };
  const journeys: Journey[] = trips.map((trip) => ({
    id: trip.id,
    title: trip.name,
    coverImage:
      trip.coverImage ??
      mappedMemories.find(
        (memory) => memory.journeyId === trip.id && memory.image,
      )?.image ??
      "",
    startDate: trip.startDate,
    endDate: trip.endDate,
    status: trip.status,
    description: trip.description,
    provinces: [
      ...new Set(
        mappedMemories
          .filter((memory) => memory.journeyId === trip.id)
          .flatMap((memory) => (memory.province ? [memory.province] : [])),
      ),
    ],
    locations: locationsFor(trip.id),
    memories: mappedMemories.filter((memory) => memory.journeyId === trip.id),
  }));
  const unassigned = mappedMemories.filter(
    (memory) => memory.journeyId === "unassigned-memories",
  );
  if (unassigned.length) {
    const dates = unassigned
      .map((memory) => memory.createdAt.slice(0, 10))
      .sort();
    journeys.push({
      id: "unassigned-memories",
      title: "Kỷ niệm khác",
      coverImage: unassigned.find((memory) => memory.image)?.image ?? "",
      startDate: dates[0],
      endDate: dates[dates.length - 1],
      provinces: [
        ...new Set(
          unassigned.flatMap((memory) =>
            memory.province ? [memory.province] : [],
          ),
        ),
      ],
      locations: locationsFor("unassigned-memories"),
      memories: unassigned,
    });
  }
  return {
    journeys,
    recentMemories: mappedMemories.sort((a, b) =>
      b.createdAt.localeCompare(a.createdAt),
    ),
    stats: {
      totalProvinces: dashboard.data.footprint.provinces,
      totalPlaces: dashboard.data.footprint.places,
      totalMemories: memories.length,
      totalJourneys: trips.length,
      exploredProvinces: [...provinces.values()],
    },
  };
}
export function useJournalDataQuery() {
  return useQuery({
    queryKey: ["journal", "data"],
    queryFn: fetchJournalData,
    staleTime: 30_000,
  });
}
