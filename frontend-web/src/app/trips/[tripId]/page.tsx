'use client';

import { use } from 'react';
import Link from 'next/link';
import {
  CalendarDays,
  MapPin,
  Compass,
  Camera,
  ArrowRight,
  Sparkles,
} from 'lucide-react';
import { useTripQuery } from '@/features/trip/api/useTripQuery';
import { ROUTES } from '@/lib/constants';
import { formatDateRange, daysBetween } from '@/lib/utils';

export default function TripOverviewPage({
  params,
}: {
  params: Promise<{ tripId: string }>;
}) {
  const resolvedParams = use(params);
  const { data: trip } = useTripQuery(resolvedParams.tripId);

  if (!trip) return null;

  const days = daysBetween(trip.startDate, trip.endDate);

  return (
    <div className="p-6 max-w-4xl space-y-6">
      {/* Welcome banner */}
      <div className="rounded-xl border bg-card p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-bold">{trip.name}</h2>
            <div className="flex flex-wrap items-center gap-4 text-sm text-muted-foreground mt-2">
              <span className="flex items-center gap-1.5">
                <MapPin className="h-4 w-4" />
                {trip.destination}
              </span>
              <span className="flex items-center gap-1.5">
                <CalendarDays className="h-4 w-4" />
                {formatDateRange(trip.startDate, trip.endDate)} ({days} ngày)
              </span>
            </div>
          </div>
          <Link
            href={ROUTES.tripItinerary(trip.id)}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition flex-shrink-0"
          >
            Xem lịch trình
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
      </div>

      {/* Quick stats grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="rounded-xl border bg-card p-4">
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-primary/10 p-2 text-primary">
              <Compass className="h-5 w-5" />
            </div>
            <div>
              <p className="text-2xl font-bold">{trip.placeCount}</p>
              <p className="text-xs text-muted-foreground">Địa điểm đã thêm</p>
            </div>
          </div>
        </div>

        <div className="rounded-xl border bg-card p-4">
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-green-100 p-2 text-green-700">
              <Compass className="h-5 w-5" />
            </div>
            <div>
              <p className="text-2xl font-bold">{trip.visitedCount}</p>
              <p className="text-xs text-muted-foreground">Địa điểm đã đến</p>
            </div>
          </div>
        </div>

        <div className="rounded-xl border bg-card p-4">
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-blue-100 p-2 text-blue-700">
              <Camera className="h-5 w-5" />
            </div>
            <div>
              <p className="text-2xl font-bold">{trip.memoryCount}</p>
              <p className="text-xs text-muted-foreground">Kỷ niệm đã lưu</p>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Link
          href={ROUTES.tripItinerary(trip.id)}
          className="group rounded-xl border bg-card p-5 hover:border-primary/50 transition-all"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-primary/10 p-2 text-primary">
                <Sparkles className="h-5 w-5" />
              </div>
              <div>
                <h3 className="font-semibold text-sm">Lịch trình chuyến đi</h3>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Lập kế hoạch theo ngày và nhận gợi ý AI
                </p>
              </div>
            </div>
            <ArrowRight className="h-4 w-4 text-muted-foreground group-hover:translate-x-1 transition-transform" />
          </div>
        </Link>

        <Link
          href={ROUTES.tripMemories(trip.id)}
          className="group rounded-xl border bg-card p-5 hover:border-primary/50 transition-all"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-primary/10 p-2 text-primary">
                <Camera className="h-5 w-5" />
              </div>
              <div>
                <h3 className="font-semibold text-sm">Kho kỷ niệm</h3>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Lưu ảnh và nhật ký hành trình
                </p>
              </div>
            </div>
            <ArrowRight className="h-4 w-4 text-muted-foreground group-hover:translate-x-1 transition-transform" />
          </div>
        </Link>
      </div>
    </div>
  );
}
