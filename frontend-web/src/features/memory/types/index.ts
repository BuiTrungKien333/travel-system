// Memory data model — Phase 7 schema
// Note: Memory does NOT have a 'type' field. Media[] is separate.
export interface Memory {
  id: string;
  userId: string;
  tripId: string;
  placeId?: string; // optional — photo taken on the road, not at a specific place
  placeName?: string; // denormalised for display
  note?: string; // optional caption / note
  mediaCount: number; // count of Media[] attached
  coverMediaUrl?: string; // first media URL for thumbnail
  experiencedAt?: string;
  version?: number;
  createdAt: string;
}

export interface Media {
  id: string;
  memoryId: string;
  storageKey: string; // key in Media Storage (never the binary itself)
  url: string; // CDN delivery URL
  width?: number;
  height?: number;
  fileSize?: number;
  mimeType?: string;
  sortOrder: number;
  createdAt: string;
}

export interface MemoryDetail extends Memory {
  media: Media[];
}

export interface CreateMemoryRequest {
  tripId: string;
  placeId?: string;
  note?: string;
  // Media files are uploaded separately via multipart POST
}

export interface UpdateMemoryRequest {
  note?: string;
}

// ─── Locket-Style Memory Types ──────────────────────────────────────────────

/** Companion/member shown on trip carousel cards */
export interface TripCompanion {
  id: string;
  name: string;
  avatarUrl?: string;
}

/** Extended trip data for the memory carousel (Section A) */
export interface TripMemoryCard {
  id: string;
  name: string;
  destination: string;
  startDate: string;
  endDate: string;
  coverImage?: string;
  totalPhotos: number;
  companions: TripCompanion[];
  status: string;
  previewImages: string[];
}

/** Location group for Section B - location-level feed */
export interface LocationMemoryGroup {
  locationId: string;
  locationName: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  timestamp: string;
  memories: MemoryGridItem[];
}

/** Individual grid item in the masonry photo grid */
export interface MemoryGridItem {
  id: string;
  type: "photo" | "audio" | "note";
  imageUrl?: string;
  audioUrl?: string;
  noteText?: string;
  caption?: string;
  aspectRatio: "square" | "portrait" | "landscape";
  createdAt: string;
}

/** View mode toggle */
export type MemoryViewMode = "trips" | "map";

/** Year filter */
export type MemoryFilterYear = number | "all";
