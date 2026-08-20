import { create } from 'zustand';
import type { AIItineraryRecommendation } from '../types';

interface ItineraryUIState {
  hoveredPlaceId: string | null;
  selectedPlaceId: string | null;
  activeDayIndex: number;
  isGenerating: boolean;
  pendingRecommendation: AIItineraryRecommendation | null;

  setHoveredPlaceId: (id: string | null) => void;
  setSelectedPlaceId: (id: string | null) => void;
  setActiveDayIndex: (index: number) => void;
  setIsGenerating: (loading: boolean) => void;
  setPendingRecommendation: (rec: AIItineraryRecommendation | null) => void;
}

export const useItineraryUIStore = create<ItineraryUIState>((set) => ({
  hoveredPlaceId: null,
  selectedPlaceId: null,
  activeDayIndex: 0,
  isGenerating: false,
  pendingRecommendation: null,

  setHoveredPlaceId: (id) => set({ hoveredPlaceId: id }),
  setSelectedPlaceId: (id) => set({ selectedPlaceId: id }),
  setActiveDayIndex: (index) => set({ activeDayIndex: index }),
  setIsGenerating: (loading) => set({ isGenerating: loading }),
  setPendingRecommendation: (rec) => set({ pendingRecommendation: rec }),
}));
