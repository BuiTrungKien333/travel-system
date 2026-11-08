import { create } from 'zustand';
import type { MemoryDetail, MemoryGridItem, MemoryViewMode, MemoryFilterYear } from '../types';

type ViewMode = 'grid' | 'timeline';

interface MemoryUIState {
  // ─── Existing State ─────────────────────────────────
  viewMode: ViewMode;
  lightboxMemory: MemoryDetail | null;
  lightboxMediaIndex: number;
  isUploadOpen: boolean;

  // ─── Locket-Style State ─────────────────────────────
  activeTrip: string | null;
  filterYear: MemoryFilterYear;
  memoryViewMode: MemoryViewMode;
  photoViewerOpen: boolean;
  photoViewerItem: MemoryGridItem | null;
  photoViewerLocationItems: MemoryGridItem[];
  photoViewerIndex: number;

  // ─── Existing Actions ───────────────────────────────
  setViewMode: (mode: ViewMode) => void;
  openLightbox: (memory: MemoryDetail, mediaIndex?: number) => void;
  closeLightbox: () => void;
  setLightboxMediaIndex: (index: number) => void;
  setIsUploadOpen: (open: boolean) => void;

  // ─── Locket-Style Actions ───────────────────────────
  setActiveTrip: (tripId: string | null) => void;
  setFilterYear: (year: MemoryFilterYear) => void;
  setMemoryViewMode: (mode: MemoryViewMode) => void;
  openPhotoViewer: (item: MemoryGridItem, items: MemoryGridItem[], index: number) => void;
  closePhotoViewer: () => void;
  navigatePhotoViewer: (direction: 'prev' | 'next') => void;
}

export const useMemoryUIStore = create<MemoryUIState>((set, get) => ({
  // ─── Existing defaults ──────────────────────────────
  viewMode: 'grid',
  lightboxMemory: null,
  lightboxMediaIndex: 0,
  isUploadOpen: false,

  // ─── Locket defaults ───────────────────────────────
  activeTrip: null,
  filterYear: 'all',
  memoryViewMode: 'trips',
  photoViewerOpen: false,
  photoViewerItem: null,
  photoViewerLocationItems: [],
  photoViewerIndex: 0,

  // ─── Existing actions ──────────────────────────────
  setViewMode: (mode) => set({ viewMode: mode }),
  openLightbox: (memory, mediaIndex = 0) =>
    set({ lightboxMemory: memory, lightboxMediaIndex: mediaIndex }),
  closeLightbox: () => set({ lightboxMemory: null, lightboxMediaIndex: 0 }),
  setLightboxMediaIndex: (index) => set({ lightboxMediaIndex: index }),
  setIsUploadOpen: (open) => set({ isUploadOpen: open }),

  // ─── Locket actions ────────────────────────────────
  setActiveTrip: (tripId) => set({ activeTrip: tripId }),
  setFilterYear: (year) => set({ filterYear: year }),
  setMemoryViewMode: (mode) => set({ memoryViewMode: mode }),
  openPhotoViewer: (item, items, index) =>
    set({
      photoViewerOpen: true,
      photoViewerItem: item,
      photoViewerLocationItems: items,
      photoViewerIndex: index,
    }),
  closePhotoViewer: () =>
    set({
      photoViewerOpen: false,
      photoViewerItem: null,
      photoViewerLocationItems: [],
      photoViewerIndex: 0,
    }),
  navigatePhotoViewer: (direction) => {
    const { photoViewerIndex, photoViewerLocationItems } = get();
    const photoItems = photoViewerLocationItems.filter((i) => i.type === 'photo');
    if (photoItems.length === 0) return;

    let newIndex = photoViewerIndex;
    if (direction === 'next') {
      newIndex = (photoViewerIndex + 1) % photoItems.length;
    } else {
      newIndex = (photoViewerIndex - 1 + photoItems.length) % photoItems.length;
    }
    set({
      photoViewerIndex: newIndex,
      photoViewerItem: photoItems[newIndex],
    });
  },
}));
