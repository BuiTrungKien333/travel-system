'use client';

import React, { useMemo } from 'react';
import { useJourneyQuery } from '@/features/journey/api/useJourneyQuery';
import { useTripsWithMemoriesQuery } from '@/features/journey/api/useTripsWithMemoriesQuery';
import { MOCK_TRIP_CARDS, MOCK_LOCATION_GROUPS } from '@/constants';
import { MemoriesHeader } from '@/features/memory/components/MemoriesHeader';
import { TripCarousel } from '@/features/memory/components/TripCarousel';
import { LocationSection } from '@/features/memory/components/LocationSection';
import { PhotoViewerModal } from '@/features/memory/components/PhotoViewerModal';
import { MemoryMap } from '@/features/memory/components/MemoryMap';
import { useMemoryUIStore } from '@/features/memory/store/useMemoryUIStore';
import { MapPin, Compass } from 'lucide-react';
import type { TripMemoryCard, LocationMemoryGroup } from '@/features/memory/types';

export default function MemoryPage() {
  const { data: stats } = useJourneyQuery();
  const { data: tripsFromApi, isLoading: isLoadingTrips } = useTripsWithMemoriesQuery();
  const activeTrip = useMemoryUIStore((s) => s.activeTrip);

  const defaultStats = {
    totalTrips: 4,
    totalPlaces: 18,
    totalMemories: 42,
    totalPhotos: 532,
    totalProvinces: 63,
    exploredProvinces: [
      { id: 'p1', code: '48', name: 'Đà Nẵng' },
      { id: 'p2', code: '91', name: 'Kiên Giang (Phú Quốc)' },
      { id: 'p3', code: '03', name: 'Hà Giang' },
      { id: 'p4', code: '68', name: 'Lâm Đồng' },
      { id: 'p5', code: '49', name: 'Quảng Nam' },
    ],
    completedMilestones: 1,
    nextMilestoneTarget: 10,
    achievementBadge: 'Người khám phá',
  };

  const currentStats = stats ?? defaultStats;

  // Transform API trips or fallback to mock data
  const tripCards: TripMemoryCard[] = useMemo(() => {
    if (tripsFromApi && tripsFromApi.length > 0) {
      return tripsFromApi.map((trip) => ({
        id: trip.id,
        name: trip.name,
        destination: trip.destination,
        startDate: trip.startDate,
        endDate: trip.endDate,
        coverImage: trip.coverImage,
        totalPhotos: trip.memoryCount || 0,
        companions: [],
        status: trip.status,
        previewImages: trip.coverImage ? [trip.coverImage] : [],
      }));
    }
    return MOCK_TRIP_CARDS;
  }, [tripsFromApi]);

  const locationGroups: LocationMemoryGroup[] = useMemo(() => {
    if (activeTrip) {
      const tripIndex = tripCards.findIndex((t) => t.id === activeTrip);
      if (tripIndex >= 0) {
        const start = (tripIndex * 2) % MOCK_LOCATION_GROUPS.length;
        const end = Math.min(start + 2, MOCK_LOCATION_GROUPS.length);
        return MOCK_LOCATION_GROUPS.slice(start, end);
      }
    }
    return MOCK_LOCATION_GROUPS;
  }, [activeTrip, tripCards]);

  return (
    <div className="min-h-screen bg-white text-slate-900 selection:bg-red-500 selection:text-white">
      {/* ─── Hero Header ─── */}
      <section className="relative overflow-hidden bg-gradient-to-b from-red-50/50 via-white to-white pt-8 pb-6 border-b border-slate-100">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="max-w-2xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-red-200/80 bg-red-50/80 px-3 py-1 text-xs font-bold text-red-800 shadow-sm backdrop-blur-sm mb-3">
              <Compass className="h-3.5 w-3.5 text-red-600" />
              <span>Hành trình khám phá dải đất hình chữ S</span>
              <span className="h-1 w-1 rounded-full bg-red-400" />
              <span className="text-red-600 font-semibold">{currentStats.totalTrips} Chuyến đi</span>
            </div>

            <h1 className="text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">
              Nhật ký kỷ niệm{' '}
              <span className="bg-gradient-to-r from-red-600 to-rose-600 bg-clip-text text-transparent">
                (Memories)
              </span>
            </h1>

            <p className="mt-2 text-sm text-slate-600 sm:text-base leading-relaxed">
              Trực quan hóa tuyến hành trình xuyên suốt từ địa đầu cực Bắc <strong>Hà Giang</strong> đến thiên đường biển đảo phía Nam <strong>Phú Quốc</strong>.
            </p>
          </div>
        </div>
      </section>

      {/* ─── Interactive Leaflet Map: Hà Giang ⟷ Phú Quốc (Red Line) ─── */}
      <section className="py-6 sm:py-8 bg-slate-50/60 border-b border-slate-200/80">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-red-500 text-white shadow-md shadow-red-500/20">
                <MapPin className="h-4 w-4" />
              </div>
              <div>
                <h2 className="text-lg font-black text-slate-900">
                  Bản đồ tuyến đường Hà Giang – Phú Quốc
                </h2>
                <p className="text-xs text-slate-500">
                  Đường màu đỏ nối liền 2 điểm cực Bắc và Nam của dải đất Việt Nam
                </p>
              </div>
            </div>

            <div className="hidden sm:flex items-center gap-2 text-xs font-bold text-slate-600 bg-white px-3 py-1.5 rounded-xl border border-slate-200 shadow-sm">
              <span className="h-2 w-2 rounded-full bg-red-500 animate-ping" />
              <span>Đường nối màu đỏ</span>
            </div>
          </div>

          <MemoryMap />
        </div>
      </section>

      {/* ─── Sticky Header ─── */}
      <MemoriesHeader />

      {/* ─── Trip Carousel ─── */}
      <section className="pt-6 pb-10 bg-slate-50/50">
        <TripCarousel trips={tripCards} isLoading={isLoadingTrips} />
      </section>

      {/* ─── Location-Level Memories Feed ─── */}
      <section className="px-4 sm:px-6 py-10 max-w-5xl mx-auto">
        <LocationSection groups={locationGroups} isLoading={isLoadingTrips} />
      </section>

      {/* ─── Full-Screen Photo Viewer Modal ─── */}
      <PhotoViewerModal />
    </div>
  );
}
