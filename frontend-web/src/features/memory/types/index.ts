// Memory data model — Phase 7 schema
// Note: Memory does NOT have a 'type' field. Media[] is separate.
export interface Memory {
  id: string;
  userId: string;
  tripId: string;
  placeId?: string;      // optional — photo taken on the road, not at a specific place
  placeName?: string;    // denormalised for display
  note?: string;         // optional caption / note
  mediaCount: number;    // count of Media[] attached
  coverMediaUrl?: string; // first media URL for thumbnail
  createdAt: string;
}

export interface Media {
  id: string;
  memoryId: string;
  storageKey: string;    // key in Media Storage (never the binary itself)
  url: string;           // CDN delivery URL
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
