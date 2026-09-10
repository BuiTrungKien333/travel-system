'use client';

import { use } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  CalendarDays,
  Camera,
  ArrowRight,
  Settings,
  Route,
  Map,
} from 'lucide-react';
import { useTripQuery } from '@/features/trip/api/useTripQuery';
import { ROUTES } from '@/constants';
import { formatDateRange, daysBetween } from '@/lib/utils';
import { TripStatusBadge } from '@/features/trip/components/TripStatusBadge';

export default function TripOverviewPage({
  params,
}: {
  params: Promise<{ tripId: string }>;
}) {
  const resolvedParams = use(params);
  const { data: trip, isLoading } = useTripQuery(resolvedParams.tripId);

  if (isLoading || !trip) {
    return (
      <div className="p-6 sm:p-10 max-w-5xl mx-auto space-y-6 animate-pulse">
        <div className="h-64 rounded-xl bg-muted" />
        <div className="h-8 w-1/3 bg-muted rounded" />
        <div className="h-4 w-1/2 bg-muted rounded" />
      </div>
    );
  }

  const days = daysBetween(trip.startDate, trip.endDate);

  return (
    <div className="p-4 sm:p-8 max-w-5xl mx-auto space-y-8">
      
      {/* ─── Editorial Trip Hero ─── */}
      <div className="relative rounded-xl overflow-hidden border border-border/70 bg-card">
        {/* Cover Photo Plate */}
        <div className="relative h-56 sm:h-72 w-full bg-muted">
          {trip.coverImage ? (
            <Image
              src={trip.coverImage}
              alt={trip.name}
              fill
              priority
              className="object-cover"
              sizes="(max-width: 1024px) 100vw, 1000px"
            />
          ) : (
            <Image
              src="https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&auto=format&fit=crop&q=80"
              alt={trip.destination}
              fill
              priority
              className="object-cover"
              sizes="(max-width: 1024px) 100vw, 1000px"
            />
          )}
          <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />
          
          <div className="absolute top-4 right-4">
            <TripStatusBadge status={trip.status} className="bg-background/90 backdrop-blur-sm" />
          </div>

          <div className="absolute bottom-6 left-6 right-6 text-white">
            <p className="text-xs uppercase tracking-widest text-white/80 font-medium">
              {trip.destination}
            </p>
            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-bold tracking-tight text-white mt-1">
              {trip.name}
            </h1>
            <div className="flex flex-wrap items-center gap-4 text-xs sm:text-sm text-white/90 mt-2">
              <span className="flex items-center gap-1.5">
                <CalendarDays className="h-4 w-4 text-white/80" />
                {formatDateRange(trip.startDate, trip.endDate)} ({days} ngày)
              </span>
              <span>·</span>
              <span>{trip.placeCount} địa điểm đã lên kế hoạch</span>
            </div>
          </div>
        </div>

        {/* Quick Action Strip */}
        <div className="p-4 bg-background border-t border-border/60 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-4 text-xs text-muted-foreground">
            <span><strong className="font-semibold text-foreground">{trip.placeCount}</strong> địa điểm</span>
            <span>·</span>
            <span><strong className="font-semibold text-foreground">{trip.visitedCount}</strong> đã ghé</span>
            <span>·</span>
            <span><strong className="font-semibold text-foreground">{trip.memoryCount}</strong> kỷ niệm</span>
          </div>

          <div className="flex items-center gap-2">
            <Link
              href={ROUTES.tripSettings(trip.id)}
              className="p-2 rounded-lg border border-border text-muted-foreground hover:text-foreground hover:bg-muted/50 transition-colors"
              aria-label="Cài đặt chuyến đi"
            >
              <Settings className="h-4 w-4" />
            </Link>
            <Link
              href={ROUTES.tripItinerary(trip.id)}
              className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:bg-primary/90 transition-colors shadow-xs"
            >
              <span>Xem lịch trình chi tiết</span>
              <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </div>
        </div>
      </div>

      {/* ─── Two-Column Editorial Layout ─── */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* Left Column: Itinerary & Next Steps */}
        <div className="lg:col-span-7 space-y-6">
          <div className="flex items-center justify-between pb-3 border-b border-border/60">
            <h2 className="text-base font-bold text-foreground">
              Lịch trình hành trình
            </h2>
            <Link
              href={ROUTES.tripItinerary(trip.id)}
              className="text-xs font-medium text-primary hover:underline"
            >
              Chỉnh sửa theo ngày →
            </Link>
          </div>

          {trip.placeCount > 0 ? (
            <div className="space-y-3">
              <div className="rounded-lg border border-border/70 p-4 bg-muted/10 space-y-2">
                <div className="flex items-center gap-2 text-xs font-semibold text-primary">
                  <Route className="h-4 w-4" />
                  <span>Kế hoạch {days} ngày tại {trip.destination}</span>
                </div>
                <p className="text-xs text-muted-foreground leading-relaxed">
                  Lịch trình đã được sắp xếp khoa học theo khoảng cách và thời gian. Bạn có thể mở bản đồ để xem trực quan hoặc kéo thả để đổi thứ tự.
                </p>
                <div className="pt-2">
                  <Link
                    href={ROUTES.tripItinerary(trip.id)}
                    className="inline-flex items-center gap-1.5 text-xs font-medium text-foreground hover:text-primary transition-colors"
                  >
                    <span>Mở bảng sắp xếp lịch trình</span>
                    <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                </div>
              </div>
            </div>
          ) : (
            <div className="rounded-lg border border-dashed border-border p-6 text-center space-y-2">
              <p className="text-sm font-medium text-foreground">Chưa có địa điểm nào trong lịch trình</p>
              <p className="text-xs text-muted-foreground">Thêm các địa điểm ăn uống, tham quan để AI gợi ý thứ tự di chuyển tối ưu.</p>
              <div className="pt-2">
                <Link
                  href={ROUTES.tripItinerary(trip.id)}
                  className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary hover:underline"
                >
                  <span>Thêm địa điểm đầu tiên</span>
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </div>
            </div>
          )}
        </div>

        {/* Right Column: Memories & Overview Links */}
        <div className="lg:col-span-5 space-y-6">
          <div className="flex items-center justify-between pb-3 border-b border-border/60">
            <h2 className="text-base font-bold text-foreground">
              Kho kỷ niệm & Ảnh
            </h2>
            <Link
              href={ROUTES.tripMemories(trip.id)}
              className="text-xs font-medium text-primary hover:underline"
            >
              Xem tất cả ({trip.memoryCount}) →
            </Link>
          </div>

          <div className="rounded-lg border border-border/70 p-4 bg-card space-y-3">
            <div className="flex items-center gap-2 text-xs font-semibold text-foreground">
              <Camera className="h-4 w-4 text-primary" />
              <span>Ghi lại từng khoảnh khắc</span>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">
              Lưu giữ ảnh chụp và cảm nhận gắn với từng điểm dừng chân để xem lại theo dạng nhật ký hành trình.
            </p>
            <div className="pt-1">
              <Link
                href={ROUTES.tripMemories(trip.id)}
                className="inline-flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
              >
                <span>Mở kho kỷ niệm chuyến đi</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          </div>

          {/* Map Shortcut */}
          <div className="rounded-lg border border-border/70 p-4 bg-card space-y-2">
            <div className="flex items-center gap-2 text-xs font-semibold text-foreground">
              <Map className="h-4 w-4 text-primary" />
              <span>Bản đồ lộ trình</span>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">
              Theo dõi toàn bộ các điểm đến và chặng đường di chuyển trực quan trên Google Maps.
            </p>
            <div className="pt-1">
              <Link
                href={`${ROUTES.trip(trip.id)}/map`}
                className="inline-flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
              >
                <span>Xem trên bản đồ</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          </div>
        </div>

      </div>

    </div>
  );
}
