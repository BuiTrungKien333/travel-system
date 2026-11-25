import apiClient from "./api";
import type {
  TripOverview,
  CreateTripRequest,
  UpdateTripRequest,
} from "@/features/trip/types";
import type { TripStatus } from "@/lib/constants";
import type { Itinerary } from "@/features/itinerary/types";
import type { MemoryDetail } from "@/features/memory/types";

export interface Page<T> {
  content: T[];
  totalPages: number;
}
export async function getAllPages<T>(
  url: string,
  params: Record<string, string> = {},
): Promise<T[]> {
  const rows: T[] = [];
  let page = 0;
  let totalPages = 1;
  do {
    const { data } = await apiClient.get<Page<T>>(url, {
      params: { ...params, page, size: 100 },
    });
    rows.push(...data.content);
    totalPages = data.totalPages;
    page++;
  } while (page < totalPages);
  return rows;
}

export interface BackendTrip {
  id: string;
  title: string;
  destination: { name: string; provinceId: string | null };
  startDate: string;
  endDate: string;
  status: TripStatus;
  mode: string;
  stopCount: number;
  memoryCount: number;
  coverImageUrl: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  budget?: { amount: number; currency: string } | null;
  preferences?: string[];
  notes?: string | null;
  days?: { id: string; date: string; dayNumber: number }[];
}
export interface TripRead {
  trip: BackendTrip;
}
export function mapTrip(trip: BackendTrip): TripOverview {
  return {
    id: trip.id,
    name: trip.title,
    destination: trip.destination.name,
    startDate: trip.startDate,
    endDate: trip.endDate,
    status: trip.status,
    coverImage: trip.coverImageUrl ?? undefined,
    placeCount: trip.stopCount,
    memoryCount: trip.memoryCount,
    version: trip.version,
    mode: trip.mode,
    createdAt: trip.createdAt,
    updatedAt: trip.updatedAt,
    budget: trip.budget?.amount,
    preferences: trip.preferences,
    description: trip.notes ?? undefined,
    days: trip.days,
  };
}
export async function fetchTrips() {
  return (await getAllPages<BackendTrip>("/trips")).map(mapTrip);
}
export async function fetchTrip(id: string) {
  const { data } = await apiClient.get<TripRead>(`/trips/${id}`);
  return mapTrip(data.trip);
}
export function tripPayload(input: CreateTripRequest | UpdateTripRequest) {
  return {
    ...(input.name !== undefined && { title: input.name }),
    ...(input.destination !== undefined && {
      destination: {
        name: input.destination,
        ...(input.provinceId && { provinceId: input.provinceId }),
      },
    }),
    ...(input.startDate !== undefined && { startDate: input.startDate }),
    ...(input.endDate !== undefined && { endDate: input.endDate }),
    ...(input.budget !== undefined && {
      budget: { amount: input.budget, currency: "VND" },
    }),
    ...(input.preferences !== undefined && {
      preferences: input.preferences.map((p) => p.toLowerCase()),
    }),
    ...(input.description !== undefined && { notes: input.description }),
  };
}

export interface BackendItinerary {
  id: string;
  tripId: string;
  name: string;
  status: Itinerary["status"];
  updatedAt: string;
  days: {
    date: string;
    stops: {
      id: string;
      placeId: string;
      name: string;
      address?: string;
      latitude?: number;
      longitude?: number;
      category?: string;
      photo?: import("@/features/place/types").PlacePhoto | null;
      arrivalTime?: string;
      durationMinutes: number;
      sequence: number;
      note?: string;
      distanceFromPreviousKm?: number;
      travelMinutesFromPrevious?: number;
    }[];
  }[];
}
export function mapItinerary(data: BackendItinerary): Itinerary {
  return {
    ...data,
    createdAt: data.updatedAt,
    days: data.days.map((day, dayIndex) => ({
      date: day.date,
      dayIndex,
      items: day.stops.map((stop) => ({
        id: stop.id,
        tripId: data.tripId,
        itineraryId: data.id,
        placeId: stop.placeId,
        placeName: stop.name,
        placeAddress: stop.address,
        placeLat: stop.latitude,
        placeLng: stop.longitude,
        placeCategory: stop.category,
        placeImage: stop.photo?.url,
        placePhoto: stop.photo,
        dayIndex,
        order: stop.sequence,
        suggestedStartTime: stop.arrivalTime?.slice(11, 16),
        scheduledArrival: stop.arrivalTime,
        suggestedDurationMinutes: stop.durationMinutes,
        notes: stop.note,
        distanceFromPreviousKm: stop.distanceFromPreviousKm,
        durationFromPreviousMinutes: stop.travelMinutesFromPrevious,
      })),
    })),
  };
}
export function itineraryPayload(itinerary: Partial<Itinerary>) {
  if (!itinerary.days) throw new Error("Thiếu lịch trình cần lưu.");
  return {
    expectedTripVersion: itinerary.tripVersion,
    expectedUpdatedAt: itinerary.updatedAt || null,
    days: itinerary.days.map((day) => ({
      date: day.date,
      stops: day.items.map((item, sequence) => ({
        placeId: item.placeId,
        sequence,
        durationMinutes: item.suggestedDurationMinutes ?? 60,
        arrivalTime: item.scheduledArrival ?? (item.suggestedStartTime
          ? `${day.date}T${item.suggestedStartTime}:00`
          : null),
        note: item.notes ?? null,
      })),
    })),
  };
}

export interface BackendMemory {
  id: string;
  version: number;
  userId: string;
  tripId: string;
  placeId?: string;
  note?: string;
  experiencedAt: string;
  coverImageUrl?: string;
  mediaCount: number;
  createdAt: string;
  updatedAt: string;
  media?: {
    id: string;
    url: string;
    mimeType?: string;
    width?: number;
    height?: number;
    fileSize?: number;
    displayOrder: number;
    createdAt: string;
  }[];
}
export function mapMemory(memory: BackendMemory): MemoryDetail {
  return {
    ...memory,
    coverMediaUrl: memory.coverImageUrl,
    media: (memory.media ?? []).map((media) => ({
      ...media,
      memoryId: memory.id,
      storageKey: "",
      sortOrder: media.displayOrder,
    })),
  };
}
export async function fetchMemories(tripId?: string) {
  return (
    await getAllPages<BackendMemory>(
      "/journal/memories",
      tripId ? { tripId } : {},
    )
  ).map(mapMemory);
}
