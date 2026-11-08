import type { TripOverview, TripSummary, TripStatus } from '../types';

export interface TripRecord {
  id: string; title: string; destination: { name: string }; startDate: string | null; endDate: string | null;
  status: TripStatus; coverImageUrl: string | null; stopCount: number; memoryCount: number;
  createdAt: string; updatedAt: string; version: number; preferences?: string[]; notes?: string;
  budget?: { amount: number; currency: string } | null;
}

export function tripSummary(trip: TripRecord): TripSummary {
  return { id: trip.id, name: trip.title, destination: trip.destination.name,
    startDate: trip.startDate ?? '', endDate: trip.endDate ?? '', status: trip.status,
    coverImage: trip.coverImageUrl ?? undefined, placeCount: trip.stopCount, memoryCount: trip.memoryCount };
}

export function tripOverview(trip: TripRecord, userId: string, visitedCount: number): TripOverview {
  return { ...tripSummary(trip), userId, visitedCount, createdAt: trip.createdAt, updatedAt: trip.updatedAt,
    preferences: trip.preferences, description: trip.notes, budget: trip.budget?.amount };
}
