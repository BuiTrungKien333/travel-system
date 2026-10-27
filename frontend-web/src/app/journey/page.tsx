"use client";

import { useJourneyQuery } from "@/features/journey/api/useJourneyQuery";
import { HeroJourneyBanner } from "@/components/journey/HeroJourneyBanner";
import { VietnamMapSection } from "@/components/journey/VietnamMapSection";
import { LoadingState, ErrorState } from "@/components/ui/FeedbackStates";

export default function JourneyPage() {
  const { data: stats, isLoading, error, refetch } = useJourneyQuery();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background py-16">
        <LoadingState message="Đang tải hành trình khám phá..." />
      </div>
    );
  }

  if (error || !stats) {
    return (
      <div className="min-h-screen bg-background py-16">
        <ErrorState
          title="Không thể tải dữ liệu hành trình"
          message="Đã có lỗi xảy ra khi tải dữ liệu của bạn. Vui lòng thử lại."
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-20">
      <HeroJourneyBanner stats={stats} />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 mt-10">
        <VietnamMapSection exploredProvinces={stats.exploredProvinces} />
      </div>
    </div>
  );
}
