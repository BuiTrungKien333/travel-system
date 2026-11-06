// ─── Query Keys ───────────────────────────────────────────────────────────────
export const QUERY_KEYS = {
  // Auth / User
  me: () => ['me'] as const,

  // Trips
  trips: () => ['trips'] as const,
  trip: (tripId: string) => ['trips', tripId] as const,

  // Places within a Trip
  tripPlaces: (tripId: string) => ['trips', tripId, 'places'] as const,

  // Itinerary
  itinerary: (tripId: string) => ['trips', tripId, 'itinerary'] as const,

  // Memories
  memories: (tripId: string) => ['trips', tripId, 'memories'] as const,
  memory: (memoryId: string) => ['memories', memoryId] as const,

  // Journey (Province exploration progress)
  journey: () => ['journey'] as const,

  // Provinces
  provinces: () => ['provinces'] as const,
} as const;

// ─── Trip Status ──────────────────────────────────────────────────────────────
export const TRIP_STATUS = {
  PLANNING: 'PLANNING',
  READY: 'READY',
  ACTIVE: 'ACTIVE',
  COMPLETED: 'COMPLETED',
} as const;

export type TripStatus = (typeof TRIP_STATUS)[keyof typeof TRIP_STATUS];

export const TRIP_STATUS_LABELS: Record<TripStatus, string> = {
  PLANNING: 'Đang lập kế hoạch',
  READY: 'Sẵn sàng',
  ACTIVE: 'Đang diễn ra',
  COMPLETED: 'Đã hoàn thành',
};

export const TRIP_STATUS_COLORS: Record<TripStatus, string> = {
  PLANNING: 'bg-yellow-100 text-yellow-800',
  READY: 'bg-blue-100 text-blue-800',
  ACTIVE: 'bg-green-100 text-green-800',
  COMPLETED: 'bg-gray-100 text-gray-700',
};

// ─── Place Categories ─────────────────────────────────────────────────────────
export const PLACE_CATEGORIES = [
  'Nature',
  'Beach',
  'Culture',
  'History',
  'Food',
  'Cafe',
  'Entertainment',
  'Shopping',
  'Other',
] as const;

export type PlaceCategory = (typeof PLACE_CATEGORIES)[number];

export const PLACE_CATEGORY_LABELS: Record<PlaceCategory, string> = {
  Nature: 'Thiên nhiên',
  Beach: 'Biển',
  Culture: 'Văn hóa',
  History: 'Lịch sử',
  Food: 'Ẩm thực',
  Cafe: 'Cafe',
  Entertainment: 'Giải trí',
  Shopping: 'Mua sắm',
  Other: 'Khác',
};

// ─── Travel Preferences ───────────────────────────────────────────────────────
export const TRAVEL_PACES = ['Relaxed', 'Balanced', 'Active'] as const;
export type TravelPace = (typeof TRAVEL_PACES)[number];

export const TRAVEL_PACE_LABELS: Record<TravelPace, string> = {
  Relaxed: 'Nhẹ nhàng',
  Balanced: 'Cân bằng',
  Active: 'Năng động',
};

export const TRAVEL_INTERESTS = [
  'Nature',
  'Food',
  'Culture',
  'Photography',
  'Shopping',
  'Entertainment',
] as const;

export type TravelInterest = (typeof TRAVEL_INTERESTS)[number];

export const TRAVEL_INTEREST_LABELS: Record<TravelInterest, string> = {
  Nature: 'Thiên nhiên',
  Food: 'Ẩm thực',
  Culture: 'Văn hóa',
  Photography: 'Nhiếp ảnh',
  Shopping: 'Mua sắm',
  Entertainment: 'Giải trí',
};

// ─── Transportation ───────────────────────────────────────────────────────────
export const TRANSPORTATION_MODES = ['Motorbike', 'Car', 'Walking'] as const;
export type TransportationMode = (typeof TRANSPORTATION_MODES)[number];

export const TRANSPORTATION_LABELS: Record<TransportationMode, string> = {
  Motorbike: 'Xe máy',
  Car: 'Ô tô',
  Walking: 'Đi bộ',
};

// ─── Vietnam Constants ────────────────────────────────────────────────────────
export const VIETNAM_TOTAL_PROVINCES = 63;

// ─── AI Loading Messages ──────────────────────────────────────────────────────
export const AI_LOADING_MESSAGES = [
  '✨ Đang phân tích các địa điểm...',
  '🗺️ Đang tối ưu lịch trình...',
  '✨ Đang hoàn thiện đề xuất...',
] as const;

// ─── Pagination ───────────────────────────────────────────────────────────────
export const DEFAULT_PAGE_SIZE = 20;
