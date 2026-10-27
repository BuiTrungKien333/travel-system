"use client";

import Link from "next/link";
import { ArrowLeft, Settings } from "lucide-react";
import { ROUTES } from "@/constants";
import { formatDateRange, daysBetween } from "@/lib/utils";
import type { TripOverview } from "@/features/trip/types";
import { TripStatusBadge } from "@/features/trip/components/TripStatusBadge";

interface TripHeaderProps {
  trip: TripOverview;
}

export function TripHeader({ trip }: TripHeaderProps) {
  const days = daysBetween(trip.startDate, trip.endDate);

  return (
    <header className="border-b border-border/60 bg-background/95 backdrop-blur-sm px-4 sm:px-6 h-15 flex items-center gap-4 flex-shrink-0 sticky top-0 z-20">
      {/* Back to trips */}
      <Link
        href={ROUTES.trips}
        className="p-1.5 rounded-md hover:bg-muted/70 text-muted-foreground hover:text-foreground transition-colors"
        aria-label="Quay lại danh sách chuyến đi"
      >
        <ArrowLeft className="h-4.5 w-4.5" />
      </Link>

      {/* Trip info */}
      <div className="min-w-0 flex-1 flex items-center gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2.5">
            <h1 className="font-bold text-base text-foreground truncate">
              {trip.name}
            </h1>
            <TripStatusBadge status={trip.status} />
          </div>
          <p className="text-xs text-muted-foreground truncate mt-0.5">
            {trip.destination} · {formatDateRange(trip.startDate, trip.endDate)} ({days} ngày)
          </p>
        </div>
      </div>

      {/* Quick stats on desktop */}
      <div className="hidden md:flex items-center gap-4 text-xs text-muted-foreground flex-shrink-0">
        <span>{trip.placeCount} địa điểm</span>
        <span>·</span>
        <span>{trip.visitedCount} đã đến</span>
        <span>·</span>
        <span>{trip.memoryCount} kỷ niệm</span>
      </div>

      {/* Settings link */}
      <Link
        href={ROUTES.tripSettings(trip.id)}
        className="p-2 rounded-md hover:bg-muted/70 text-muted-foreground hover:text-foreground transition-colors"
        aria-label="Cài đặt chuyến đi"
      >
        <Settings className="h-4.5 w-4.5" />
      </Link>
    </header>
  );
}
