'use client';

import { useJourneyQuery } from '@/features/journey/api/useJourneyQuery';
import { useAllMemoriesQuery } from '@/features/journey/api/useAllMemoriesQuery';
import { useTripsWithMemoriesQuery } from '@/features/journey/api/useTripsWithMemoriesQuery';
import { HeroJourneyBanner } from '@/components/journey/HeroJourneyBanner';
import { VietnamMapSection } from '@/components/journey/VietnamMapSection';
import { TripMemoryCards } from '@/components/journey/TripMemoryCards';
import { AllMemoriesTimeline } from '@/components/journey/AllMemoriesTimeline';
import { JourneyMilestones } from '@/components/journey/JourneyMilestones';
import { LoadingState, ErrorState } from '@/components/ui';

export default function MemoryPage() {
  const {
    data: stats,
    isLoading: isLoadingStats,
    isError: isErrorStats,
    refetch: refetchStats,
  } = useJourneyQuery();

  const {
    data: allMemories,
    isLoading: isLoadingMemories,
  } = useAllMemoriesQuery();

  const {
    data: tripsWithMemories,
    isLoading: isLoadingTrips,
  } = useTripsWithMemoriesQuery();

  if (isLoadingStats && !stats) {
    return <LoadingState message="Đang tải hành trình và kỷ niệm của bạn..." />;
  }

  if (isErrorStats) {
    return (
      <ErrorState
        title="Không thể tải thông tin hành trình & kỷ niệm"
        message="Kiểm tra kết nối mạng và thử lại. Dữ liệu kỷ niệm của bạn vẫn được bảo quản an toàn."
        onRetry={() => refetchStats()}
      />
    );
  }

  const defaultStats = {
    totalTrips: 0,
    totalPlaces: 0,
    totalMemories: 0,
    totalPhotos: 0,
    totalProvinces: 63,
    exploredProvinces: [],
    completedMilestones: 0,
    nextMilestoneTarget: 5,
    achievementBadge: 'Chưa có huy hiệu',
  };

  const currentStats = stats ?? defaultStats;

  return (
    <div className="min-h-screen bg-white">
      {/* ─── Section 1: Hero Banner with Stats ─── */}
      <HeroJourneyBanner stats={currentStats} />

      {/* ─── Section 2: Vietnam Province Map ─── */}
      <section className="py-10 sm:py-14 bg-gradient-to-b from-white via-cyan-50/20 to-sky-50/40">
        <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
          <VietnamMapSection
            exploredProvinces={currentStats.exploredProvinces}
          />
        </div>
      </section>

      {/* ─── Section 3: Journey Milestones ─── */}
      <section className="py-8 bg-sky-50/40">
        <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
          <JourneyMilestones
            exploredCount={currentStats.exploredProvinces.length}
          />
        </div>
      </section>

      {/* ─── Section 4: Trip Memory Cards ─── */}
      <section className="py-10 sm:py-14 bg-gradient-to-b from-sky-50/40 via-cyan-50/10 to-white">
        <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
          <TripMemoryCards
            trips={tripsWithMemories ?? []}
            isLoading={isLoadingTrips}
          />
        </div>
      </section>

      {/* ─── Section 5: All Memories Timeline ─── */}
      <section className="py-10 sm:py-14 bg-gradient-to-b from-white via-sky-50/30 to-sky-100/60">
        <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
          <AllMemoriesTimeline
            memories={allMemories ?? []}
            isLoading={isLoadingMemories}
          />
        </div>
      </section>
    </div>
  );
}
