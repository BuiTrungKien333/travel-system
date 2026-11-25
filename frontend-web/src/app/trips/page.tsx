"use client";

import { useState, useMemo } from "react";
import Link from "next/link";
import { HomeDashboardSummary } from "@/features/trip/components/HomeDashboardSummary";
import { Plus } from "lucide-react";
import { useTripsQuery } from "@/features/trip/api/useTripsQuery";
import { TripCard } from "@/features/trip/components/TripCard";
import { TripEmptyState } from "@/features/trip/components/TripEmptyState";
import { ROUTES, TRIP_STATUS_LABELS } from "@/constants";
import { LoadingState, ErrorState } from "@/components/ui";

export default function TripsPage() {
  const { data: trips, isLoading, isError, refetch } = useTripsQuery();
  const [selectedStatus, setSelectedStatus] = useState<string>("ALL");

  const filteredTrips = useMemo(() => {
    if (!trips) return [];
    if (selectedStatus === "ALL") return trips;
    return trips.filter((t) => t.status === selectedStatus);
  }, [trips, selectedStatus]);

  const filterTabs: Array<{ key: string; label: string }> = [
    { key: "ALL", label: "Tất cả" },
    { key: "PLANNING", label: TRIP_STATUS_LABELS.PLANNING },
    { key: "READY", label: TRIP_STATUS_LABELS.READY },
    { key: "ACTIVE", label: TRIP_STATUS_LABELS.ACTIVE },
    { key: "COMPLETED", label: TRIP_STATUS_LABELS.COMPLETED },
    { key: "CANCELLED", label: TRIP_STATUS_LABELS.CANCELLED },
  ];

  return (
    <div className="min-h-screen bg-background">
      <main className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 pb-6 border-b border-border/60">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-foreground">
              Chuyến đi của bạn
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              Quản lý và tiếp tục các kế hoạch du lịch cá nhân.
            </p>
          </div>

          <Link
            href={ROUTES.tripNew}
            className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors shrink-0 shadow-sm"
          >
            <Plus className="h-4 w-4" />
            <span>Tạo chuyến đi</span>
          </Link>
        </div>

        <HomeDashboardSummary />
        {/* Filter Bar */}
        {trips && trips.length > 0 && (
          <div className="flex items-center gap-2 overflow-x-auto py-4 border-b border-border/40">
            {filterTabs.map((tab) => {
              const isActive = selectedStatus === tab.key;
              return (
                <button
                  key={tab.key}
                  onClick={() => setSelectedStatus(tab.key)}
                  className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors shrink-0 ${
                    isActive
                      ? "bg-primary/10 text-primary font-semibold"
                      : "text-muted-foreground hover:text-foreground hover:bg-muted/50"
                  }`}
                >
                  {tab.label}
                </button>
              );
            })}
          </div>
        )}

        {/* Loading State */}
        {isLoading && (
          <div className="py-16">
            <LoadingState message="Đang tải danh sách chuyến đi..." />
          </div>
        )}

        {/* Error State */}
        {isError && (
          <div className="py-16">
            <ErrorState
              title="Không thể tải danh sách chuyến đi"
              message="Vui lòng kiểm tra kết nối mạng và thử lại."
              onRetry={() => refetch()}
            />
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !isError && trips?.length === 0 && (
          <div className="mt-8 rounded-xl border border-border/60 bg-card">
            <TripEmptyState />
          </div>
        )}

        {/* Trips Grid */}
        {!isLoading && !isError && filteredTrips.length > 0 && (
          <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filteredTrips.map((trip) => (
              <TripCard key={trip.id} trip={trip} />
            ))}
          </div>
        )}

        {/* Filter Empty State */}
        {!isLoading &&
          !isError &&
          trips &&
          trips.length > 0 &&
          filteredTrips.length === 0 && (
            <div className="py-16 text-center text-sm text-muted-foreground">
              Không có chuyến đi nào ở trạng thái này.
            </div>
          )}
      </main>
    </div>
  );
}
