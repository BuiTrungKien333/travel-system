import {
  MOCK_USER,
  INITIAL_MOCK_TRIPS,
  INITIAL_MOCK_ITINERARY,
  MOCK_AI_RECOMMENDATION,
  INITIAL_MOCK_MEMORIES,
  INITIAL_MOCK_JOURNEY,
} from './mock-data';
import type { Trip, TripOverview, TripSummary } from '@/features/trip/types';
import type { Itinerary } from '@/features/itinerary/types';
import type { Memory } from '@/features/memory/types';
import type { JourneyStats } from '@/features/journey/types';

const STORAGE_KEYS = {
  trips: 'tripzy_mock_trips',
  itinerary: 'tripzy_mock_itinerary_',
  memories: 'tripzy_mock_memories_',
  journey: 'tripzy_mock_journey',
};

// Helper: safe local storage read/write
function getStored<T>(key: string, fallback: T): T {
  if (typeof window === 'undefined') return fallback;
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function setStored<T>(key: string, value: T): void {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Ignore storage quota
  }
}

// ─── Mock Service Methods ─────────────────────────────────────────────────────
export const mockService = {
  // Auth
  login: async (email: string) => {
    const user = { ...MOCK_USER, email: email || MOCK_USER.email };
    return {
      user,
      accessToken: 'mock-jwt-token-12345',
      refreshToken: 'mock-refresh-token-12345',
    };
  },

  register: async () => ({ verificationRequired: true, resendAfterSeconds: 60 }),

  getMe: async () => {
    return MOCK_USER;
  },

  // Trips
  getTrips: async (): Promise<TripSummary[]> => {
    const trips = getStored<TripOverview[]>(STORAGE_KEYS.trips, INITIAL_MOCK_TRIPS);
    return trips.map((t) => ({
      id: t.id,
      name: t.name,
      destination: t.destination,
      startDate: t.startDate,
      endDate: t.endDate,
      status: t.status,
      coverImage: t.coverImage,
      placeCount: t.placeCount,
      memoryCount: t.memoryCount,
    }));
  },

  getTrip: async (tripId: string): Promise<TripOverview> => {
    const trips = getStored<TripOverview[]>(STORAGE_KEYS.trips, INITIAL_MOCK_TRIPS);
    const found = trips.find((t) => t.id === tripId);
    if (found) return found;

    // Fallback for new trips
    return {
      id: tripId,
      userId: MOCK_USER.id,
      name: 'Chuyến đi mới',
      destination: 'Việt Nam',
      startDate: '2026-08-25',
      endDate: '2026-08-28',
      status: 'PLANNING',
      placeCount: 0,
      visitedCount: 0,
      memoryCount: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
  },

  createTrip: async (data: Partial<Trip>): Promise<Trip> => {
    const trips = getStored<TripOverview[]>(STORAGE_KEYS.trips, INITIAL_MOCK_TRIPS);
    const newTrip: TripOverview = {
      id: `trip-${Date.now()}`,
      userId: MOCK_USER.id,
      name: data.name ?? 'Chuyến đi không tên',
      destination: data.destination ?? 'Việt Nam',
      startDate: data.startDate ?? '2026-09-01',
      endDate: data.endDate ?? '2026-09-04',
      status: 'PLANNING',
      transportation: data.transportation,
      preferences: data.preferences,
      budget: data.budget,
      constraints: data.constraints,
      description: data.description,
      coverImage: 'https://images.unsplash.com/photo-1528127269322-539801943592?w=800',
      placeCount: 0,
      visitedCount: 0,
      memoryCount: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    const nextTrips = [newTrip, ...trips];
    setStored(STORAGE_KEYS.trips, nextTrips);
    return newTrip;
  },

  updateTrip: async (tripId: string, data: Partial<Trip>): Promise<TripOverview> => {
    const trips = getStored<TripOverview[]>(STORAGE_KEYS.trips, INITIAL_MOCK_TRIPS);
    const updated = trips.map((t) => (t.id === tripId ? { ...t, ...data, updatedAt: new Date().toISOString() } : t));
    setStored(STORAGE_KEYS.trips, updated);
    return updated.find((t) => t.id === tripId) ?? (trips[0] as TripOverview);
  },

  deleteTrip: async (tripId: string): Promise<void> => {
    const trips = getStored<TripOverview[]>(STORAGE_KEYS.trips, INITIAL_MOCK_TRIPS);
    const nextTrips = trips.filter((t) => t.id !== tripId);
    setStored(STORAGE_KEYS.trips, nextTrips);
  },

  // Itinerary
  getItinerary: async (tripId: string): Promise<Itinerary> => {
    const stored = getStored<Itinerary | null>(STORAGE_KEYS.itinerary + tripId, null);
    if (stored) return stored;
    if (tripId === 'trip-danang-2026') return INITIAL_MOCK_ITINERARY;

    // Create empty 3-day itinerary for other trips
    return {
      id: `itin-${tripId}`,
      tripId,
      name: 'Lịch trình dự kiến',
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      days: [
        { dayIndex: 0, date: '2026-09-01', items: [] },
        { dayIndex: 1, date: '2026-09-02', items: [] },
        { dayIndex: 2, date: '2026-09-03', items: [] },
      ],
    };
  },

  generatePlanning: async (_tripId: string) => {
    void _tripId;
    // Artificial AI thinking delay (1.2s)
    await new Promise((r) => setTimeout(r, 1200));
    return MOCK_AI_RECOMMENDATION;
  },

  updateItinerary: async (tripId: string, data: Partial<Itinerary>): Promise<Itinerary> => {
    const current = await mockService.getItinerary(tripId);
    const updated = { ...current, ...data, updatedAt: new Date().toISOString() };
    setStored(STORAGE_KEYS.itinerary + tripId, updated);
    return updated;
  },

  // Memories
  getMemories: async (tripId: string): Promise<Memory[]> => {
    const stored = getStored<Memory[] | null>(STORAGE_KEYS.memories + tripId, null);
    if (stored) return stored;
    if (tripId === 'trip-danang-2026') return INITIAL_MOCK_MEMORIES;
    return [];
  },

  createMemory: async (tripId: string, formData: FormData): Promise<Memory> => {
    const memories = await mockService.getMemories(tripId);
    const note = (formData.get('note') as string) || '';
    const newMemory: Memory = {
      id: `mem-${Date.now()}`,
      userId: MOCK_USER.id,
      tripId,
      note,
      mediaCount: 1,
      coverMediaUrl: 'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=600',
      createdAt: new Date().toISOString(),
    };
    const nextMemories = [newMemory, ...memories];
    setStored(STORAGE_KEYS.memories + tripId, nextMemories);
    return newMemory;
  },

  deleteMemory: async (memoryId: string): Promise<void> => {
    // Look through all trips to remove
    const trips = getStored<TripOverview[]>(STORAGE_KEYS.trips, INITIAL_MOCK_TRIPS);
    for (const t of trips) {
      const mems = getStored<Memory[]>(STORAGE_KEYS.memories + t.id, []);
      if (mems.some((m) => m.id === memoryId)) {
        setStored(STORAGE_KEYS.memories + t.id, mems.filter((m) => m.id !== memoryId));
      }
    }
  },

  // Journey
  getJourney: async (): Promise<JourneyStats> => {
    return getStored<JourneyStats>(STORAGE_KEYS.journey, INITIAL_MOCK_JOURNEY);
  },

  // All Memories (cross-trip aggregation for Memory Hub)
  getAllMemories: async (): Promise<(Memory & { tripName?: string })[]> => {
    const trips = getStored<TripOverview[]>(STORAGE_KEYS.trips, INITIAL_MOCK_TRIPS);
    const allMemories: (Memory & { tripName?: string })[] = [];
    for (const t of trips) {
      const mems = await mockService.getMemories(t.id);
      for (const m of mems) {
        allMemories.push({ ...m, tripName: t.name });
      }
    }
    // Sort by createdAt descending
    allMemories.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    return allMemories;
  },

  // Trips with memories (for Memory Hub trip cards)
  getTripsWithMemories: async (): Promise<TripOverview[]> => {
    const trips = getStored<TripOverview[]>(STORAGE_KEYS.trips, INITIAL_MOCK_TRIPS);
    return trips.filter((t) => t.memoryCount > 0);
  },
};
