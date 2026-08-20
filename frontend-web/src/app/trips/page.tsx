"use client";

import Link from "next/link";
import { Plus, Map } from "lucide-react";
import { useTripsQuery } from "@/features/trip/api/useTripsQuery";
import { TripCard } from "@/features/trip/components/TripCard";
import { TripEmptyState } from "@/features/trip/components/TripEmptyState";
import { ROUTES } from "@/constants";
import { LoadingState, ErrorState } from "@/components/ui";

export default function TripsPage() {
  const { data: trips, isLoading, isError, refetch } = useTripsQuery();

  return (
    <div className="min-h-screen bg-background">
      {/* Top bar */}
      <header className="border-b border-cyan-100 bg-white/90 backdrop-blur-sm px-4 h-14 flex items-center justify-between">
        <h1 className="font-bold text-sm text-slate-800">Chuyến đi của tôi</h1>
        <div className="flex items-center gap-2">
          <Link
            href={ROUTES.memory}
            className="flex items-center gap-1.5 text-xs sm:text-sm font-semibold text-slate-600 hover:text-cyan-600 transition"
          >
            <Map className="h-4 w-4" />
            <span className="hidden sm:inline">Hành trình & Kỷ niệm</span>
          </Link>
          <Link
            href={ROUTES.tripNew}
            className="inline-flex items-center gap-1.5 rounded-xl bg-cyan-500 px-3.5 py-1.5 text-xs sm:text-sm font-bold text-white shadow-sm hover:bg-cyan-600 transition"
          >
            <Plus className="h-4 w-4 stroke-[2.5]" />
            <span>Tạo chuyến đi</span>
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8">
        {isLoading && <LoadingState message="Đang tải danh sách chuyến đi của bạn..." />}

        {isError && (
          <ErrorState
            title="Không thể tải danh sách chuyến đi"
            message="Kiểm tra kết nối mạng và thử lại."
            onRetry={() => refetch()}
          />
        )}

        {!isLoading && !isError && trips?.length === 0 && <TripEmptyState />}

        {!isLoading && !isError && trips && trips.length > 0 && (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {trips.map((trip) => (
              <TripCard key={trip.id} trip={trip} />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
