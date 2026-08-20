import { create } from 'zustand';

interface TripUIState {
  sidebarOpen: boolean;
  sidebarCollapsed: boolean;
  mobileView: 'content' | 'map';
  activeTripId: string | null;
  setSidebarOpen: (open: boolean) => void;
  setSidebarCollapsed: (collapsed: boolean) => void;
  setMobileView: (view: 'content' | 'map') => void;
  setActiveTripId: (id: string | null) => void;
}

export const useTripUIStore = create<TripUIState>((set) => ({
  sidebarOpen: true,
  sidebarCollapsed: false,
  mobileView: 'content',
  activeTripId: null,
  setSidebarOpen: (open) => set({ sidebarOpen: open }),
  setSidebarCollapsed: (collapsed) => set({ sidebarCollapsed: collapsed }),
  setMobileView: (view) => set({ mobileView: view }),
  setActiveTripId: (id) => set({ activeTripId: id }),
}));
