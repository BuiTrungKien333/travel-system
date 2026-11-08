import { create } from 'zustand';
import type { JournalSortOption, Memory } from '../types';

interface JournalUIState {
  selectedJourneyId: string | null;
  selectedLocationId: string | null;
  hoveredLocationId: string | null;
  searchQuery: string;
  selectedProvince: string | null;
  selectedDateRange: string | null;
  sortBy: JournalSortOption;
  activeMemoryForLightbox: Memory | null;
  isMapFullscreen: boolean;

  // Actions
  setSelectedJourneyId: (journeyId: string | null) => void;
  setSelectedLocationId: (locationId: string | null) => void;
  setHoveredLocationId: (locationId: string | null) => void;
  setSearchQuery: (query: string) => void;
  setSelectedProvince: (province: string | null) => void;
  setSelectedDateRange: (dateRange: string | null) => void;
  setSortBy: (sortBy: JournalSortOption) => void;
  setActiveMemoryForLightbox: (memory: Memory | null) => void;
  setIsMapFullscreen: (isFullscreen: boolean) => void;
  resetFilters: () => void;
}

export const useJournalUIStore = create<JournalUIState>((set) => ({
  selectedJourneyId: null,
  selectedLocationId: null,
  hoveredLocationId: null,
  searchQuery: '',
  selectedProvince: null,
  selectedDateRange: null,
  sortBy: 'newest',
  activeMemoryForLightbox: null,
  isMapFullscreen: false,

  setSelectedJourneyId: (journeyId) => set({ selectedJourneyId: journeyId }),
  setSelectedLocationId: (locationId) => set({ selectedLocationId: locationId }),
  setHoveredLocationId: (locationId) => set({ hoveredLocationId: locationId }),
  setSearchQuery: (query) => set({ searchQuery: query }),
  setSelectedProvince: (province) => set({ selectedProvince: province }),
  setSelectedDateRange: (dateRange) => set({ selectedDateRange: dateRange }),
  setSortBy: (sortBy) => set({ sortBy }),
  setActiveMemoryForLightbox: (memory) => set({ activeMemoryForLightbox: memory }),
  setIsMapFullscreen: (isMapFullscreen) => set({ isMapFullscreen }),
  resetFilters: () =>
    set({
      searchQuery: '',
      selectedProvince: null,
      selectedJourneyId: null,
      selectedDateRange: null,
      sortBy: 'newest',
    }),
}));
