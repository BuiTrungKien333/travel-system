'use client';

import { useMemo } from 'react';
import { useJourneyQuery } from '@/features/journey/api/useJourneyQuery';
import { useTripsWithMemoriesQuery } from '@/features/journey/api/useTripsWithMemoriesQuery';
import { MOCK_TRIP_CARDS, MOCK_LOCATION_GROUPS } from '@/constants';
import { HeroJourneyBanner } from '@/components/journey/HeroJourneyBanner';
import { MemoriesHeader } from '@/features/memory/components/MemoriesHeader';
import { TripCarousel } from '@/features/memory/components/TripCarousel';
import { LocationSection } from '@/features/memory/components/LocationSection';
import { PhotoViewerModal } from '@/features/memory/components/PhotoViewerModal';
import { useMemoryUIStore } from '@/features/memory/store/useMemoryUIStore';
import type { TripMemoryCard, LocationMemoryGroup } from '@/features/memory/types';

export default function MemoryPage() {
  const {
    data: stats,
  } = useJourneyQuery();

  const {
    data: tripsFromApi,
    isLoading: isLoadingTrips,
  } = useTripsWithMemoriesQuery();

  const activeTrip = useMemoryUIStore((s) => s.activeTrip);

  const defaultStats = {
    totalTrips: 4,
    totalPlaces: 18,
    totalMemories: 42,
    totalPhotos: 532,
    totalProvinces: 63,
    exploredProvinces: [
      { id: 'p1', code: '48', name: 'Đà Nẵng' },
      { id: 'p2', code: '91', name: 'Kiên Giang' },
      { id: 'p3', code: '68', name: 'Lâm Đồng' },
      { id: 'p4', code: '49', name: 'Quảng Nam' },
      { id: 'p5', code: '10', name: 'Lào Cai' },
    ],
    completedMilestones: 1,
    nextMilestoneTarget: 10,
    achievementBadge: 'Người khám phá',
  };

  const currentStats = stats ?? defaultStats;

  // Transform API trips into TripMemoryCard shape, fallback to mock data
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

  // Use mock location groups, filtered by active trip if set
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
    <div className="min-h-screen bg-white text-slate-900 selection:bg-cyan-500 selection:text-white">
      {/* ─── Section: Explored Provinces & Journey Stats ─── */}
      <HeroJourneyBanner stats={currentStats} />

      {/* ─── Sticky Header ─── */}
      <MemoriesHeader />

      {/* ─── Section A: Trip-Level Memories Carousel ─── */}
      <section className="pt-6 pb-10 bg-slate-50/50">
        <TripCarousel trips={tripCards} isLoading={isLoadingTrips} />
      </section>

      {/* ─── Section B: Location-Level Memories Feed (3 cols on web, 1 col on mobile) ─── */}
      <section className="px-4 sm:px-6 py-10 max-w-5xl mx-auto">
        <LocationSection groups={locationGroups} isLoading={isLoadingTrips} />
      </section>

      {/* ─── Full-Screen Locket Photo Viewer Modal ─── */}
      <PhotoViewerModal />
    </div>
  );
}
