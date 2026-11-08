export interface ItineraryItem {
  id: string;
  itineraryId: string;
  placeId: string;
  placeName: string;
  placeAddress?: string;
  placeLat?: number;
  placeLng?: number;
  placeCategory?: string;
  placeImage?: string;
  dayIndex: number;               // 0-based day index within trip
  order: number;                  // order within the day
  suggestedStartTime?: string;   // "09:00"
  suggestedDurationMinutes?: number;
  // TODO: [PHASE-2] Travel info pending Routing API integration from backend
  distanceFromPreviousKm?: number;
  durationFromPreviousMinutes?: number;
  notes?: string;
  visitedAt?: string;            // ISO datetime if user checked in
}

export interface ItineraryDay {
  dayIndex: number;              // 0-based
  date: string;                  // ISO date YYYY-MM-DD
  items: ItineraryItem[];
}

export interface Itinerary {
  id: string;
  tripId: string;
  name: string;
  status: 'DRAFT' | 'ACTIVE';
  days: ItineraryDay[];
  createdAt: string;
  updatedAt: string;
}

// AI Recommendation response — Phase 4C structured output
export interface AIItineraryRecommendation {
  type: 'AI' | 'FALLBACK';       // 'AI' = AI-generated | 'FALLBACK' = rule-based
  days: {
    date: string;
    items: {
      placeId: string;
      order: number;
      suggestedStartTime: string;
      suggestedDurationMinutes: number;
      reason: string;
    }[];
  }[];
  summary: string;
  notes: string[];
  weatherWarnings?: string[];
}

export interface PlanningRequest {
  tripId: string;
}

export interface ReorderItemRequest {
  itemId: string;
  targetDayIndex: number;
  targetOrder: number;
}
