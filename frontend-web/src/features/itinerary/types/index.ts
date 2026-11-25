export interface ItineraryItem {
  id: string;
  itineraryId: string;
  tripId?: string;
  visited?: boolean;
  placeId: string;
  placeName: string;
  placeAddress?: string;
  placeLat?: number;
  placeLng?: number;
  placeCategory?: string;
  placeImage?: string;
  placePhoto?: import("@/features/place/types").PlacePhoto | null;
  dayIndex: number; // 0-based day index within trip
  order: number; // order within the day
  suggestedStartTime?: string; // "09:00"
  scheduledArrival?: string; // Preserve the full local timestamp for visits crossing midnight.
  suggestedDurationMinutes?: number;
  distanceFromPreviousKm?: number;
  durationFromPreviousMinutes?: number;
  notes?: string;
  visitedAt?: string; // ISO datetime if user checked in
}

export interface ItineraryDay {
  dayIndex: number; // 0-based
  date: string; // ISO date YYYY-MM-DD
  items: ItineraryItem[];
}

export interface Itinerary {
  tripVersion?: number;
  readOnly?: boolean;
  editableFrom?: string;
  id: string;
  tripId: string;
  name: string;
  status: "DRAFT" | "ACTIVE";
  days: ItineraryDay[];
  createdAt: string;
  updatedAt: string;
}

// AI Recommendation response — Phase 4C structured output
export interface AIItineraryRecommendation {
  optimization?: {
    sourceUpdatedAt: string;
    proposal: import("../api/usePlanningMutation").OptimizationProposal;
  };
  type: "AI" | "FALLBACK"; // 'AI' = AI-generated | 'FALLBACK' = rule-based
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
