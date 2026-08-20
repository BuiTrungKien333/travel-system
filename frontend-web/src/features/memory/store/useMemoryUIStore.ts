import { create } from 'zustand';
import type { MemoryDetail } from '../types';

type ViewMode = 'grid' | 'timeline';

interface MemoryUIState {
  viewMode: ViewMode;
  // Lightbox state
  lightboxMemory: MemoryDetail | null;
  lightboxMediaIndex: number;
  // Upload panel
  isUploadOpen: boolean;

  setViewMode: (mode: ViewMode) => void;
  openLightbox: (memory: MemoryDetail, mediaIndex?: number) => void;
  closeLightbox: () => void;
  setLightboxMediaIndex: (index: number) => void;
  setIsUploadOpen: (open: boolean) => void;
}

export const useMemoryUIStore = create<MemoryUIState>((set) => ({
  viewMode: 'grid',
  lightboxMemory: null,
  lightboxMediaIndex: 0,
  isUploadOpen: false,

  setViewMode: (mode) => set({ viewMode: mode }),
  openLightbox: (memory, mediaIndex = 0) =>
    set({ lightboxMemory: memory, lightboxMediaIndex: mediaIndex }),
  closeLightbox: () => set({ lightboxMemory: null, lightboxMediaIndex: 0 }),
  setLightboxMediaIndex: (index) => set({ lightboxMediaIndex: index }),
  setIsUploadOpen: (open) => set({ isUploadOpen: open }),
}));
