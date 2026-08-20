import Link from 'next/link';
import Image from 'next/image';
import { CalendarDays, MapPin, Image as ImageIcon } from 'lucide-react';
import type { TripSummary } from '../types';
import { TripStatusBadge } from './TripStatusBadge';
import { ROUTES } from '@/lib/constants';
import { formatDateRange, daysBetween } from '@/lib/utils';

interface TripCardProps {
  trip: TripSummary;
}

export function TripCard({ trip }: TripCardProps) {
  const days = daysBetween(trip.startDate, trip.endDate);

  return (
    <Link
      href={ROUTES.trip(trip.id)}
      className="group block rounded-xl border bg-card overflow-hidden shadow-sm hover:shadow-md transition-shadow"
    >
      <div className="relative h-40 w-full bg-muted">
        {trip.coverImage ? (
          <Image
            src={trip.coverImage}
            alt={`Ảnh bìa chuyến đi ${trip.name}`}
            fill
            className="object-cover"
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
          />
        ) : (
          <div className="flex h-full items-center justify-center">
            <ImageIcon className="h-10 w-10 text-muted-foreground/40" />
          </div>
        )}
        <div className="absolute top-2 right-2">
          <TripStatusBadge status={trip.status} />
        </div>
      </div>

      <div className="p-4 space-y-2">
        <h3 className="font-semibold text-base leading-tight line-clamp-1">
          {trip.name}
        </h3>

        <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
          <MapPin className="h-3.5 w-3.5 flex-shrink-0" />
          <span className="truncate">{trip.destination}</span>
        </div>

        <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
          <CalendarDays className="h-3.5 w-3.5 flex-shrink-0" />
          <span>
            {formatDateRange(trip.startDate, trip.endDate)}
            {' '}·{' '}
            {days} ngày
          </span>
        </div>

        <div className="flex items-center gap-3 pt-1 text-xs text-muted-foreground">
          <span>{trip.placeCount} địa điểm</span>
          <span>·</span>
          <span>{trip.memoryCount} kỷ niệm</span>
        </div>
      </div>
    </Link>
  );
}
