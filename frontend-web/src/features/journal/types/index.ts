export interface TravelLocation {
  id: string;
  name: string;
  address?: string;
  latitude: number;
  longitude: number;
  visitedAt: string;
  journeyId: string;
  journeyTitle?: string;
  province?: string;
  memoryCount: number;
  image?: string;
  order?: number;
  dayIndex?: number;
}

export interface Memory {
  id: string;
  title: string;
  image: string;
  location: string;
  locationId?: string;
  province?: string;
  createdAt: string;
  journeyId: string;
  journeyTitle?: string;
  note?: string;
  mediaCount?: number;
}

export interface Journey {
  id: string;
  title: string;
  coverImage: string;
  startDate: string;
  endDate: string;
  provinces: string[];
  locations: TravelLocation[];
  memories: Memory[];
  description?: string;
  status?: 'PLANNING' | 'READY' | 'ACTIVE' | 'COMPLETED';
}

export interface ExploredProvinceItem {
  id: string;
  code: string;
  name: string;
}

export interface TravelStatsData {
  totalProvinces: number;
  totalPlaces: number;
  totalMemories: number;
  totalJourneys: number;
  exploredProvinces: ExploredProvinceItem[];
}

export type JournalSortOption = 'newest' | 'oldest' | 'most_visited' | 'most_memories';

export interface JournalFilterState {
  searchQuery: string;
  selectedProvince: string | null;
  selectedJourneyId: string | null;
  selectedLocationId: string | null;
  selectedDateRange: string | null;
  sortBy: JournalSortOption;
}
