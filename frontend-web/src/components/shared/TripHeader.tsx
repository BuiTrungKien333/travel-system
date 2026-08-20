"use client";

import Link from "next/link";
import { ArrowLeft, Settings } from "lucide-react";
import { ROUTES, TRIP_STATUS_LABELS, TRIP_STATUS_COLORS } from "@/lib/constants";
import { formatDateRange, daysBetween } from "@/lib/utils";
import { cn } from "@/lib/utils";
import type { TripOverview } from "@/features/trip/types";

interface TripHeaderProps {
  trip: TripOverview;
}

export function TripHeader({ trip }: TripHeaderProps) {
  const days = daysBetween(trip.startDate, trip.endDate);

  return (
    <header className="border-b bg-card px-4 h-14 flex items-center gap-3 flex-shrink-0">
      {/* Back to trips */}
      <Link
        href={ROUTES.trips}
        className="p-1.5 rounded hover:bg-muted transition-colors"
        aria-label="Quay lại danh sách chuyến đi"
      >
        <ArrowLeft className="h-4 w-4 text-muted-foreground" />
      </Link>

      {/* Trip info */}
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <h1 className="font-semibold text-sm truncate">{trip.name}</h1>
          {/* Status: uses BOTH text label and color (Accessibility rule) */}
          <span
            className={cn(
              "inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-medium flex-shrink-0",
              TRIP_STATUS_COLORS[trip.status]
            )}
            aria-label={`Trạng thái: ${TRIP_STATUS_LABELS[trip.status]}`}
          >
            {TRIP_STATUS_LABELS[trip.status]}
          </span>
        </div>
        <p className="text-xs text-muted-foreground truncate">
          {trip.destination} · {formatDateRange(trip.startDate, trip.endDate)} ·{" "}
          {days} ngày
        </p>
      </div>

      {/* Stats */}
      <div className="hidden md:flex items-center gap-4 text-xs text-muted-foreground flex-shrink-0">
        <span>{trip.placeCount} địa điểm</span>
        <span>{trip.visitedCount} đã đến</span>
        <span>{trip.memoryCount} kỷ niệm</span>
      </div>

      {/* Settings link */}
      <Link
        href={ROUTES.tripSettings(trip.id)}
        className="p-1.5 rounded hover:bg-muted transition-colors"
        aria-label="Cài đặt chuyến đi"
      >
        <Settings className="h-4 w-4 text-muted-foreground" />
      </Link>
    </header>
  );
}
