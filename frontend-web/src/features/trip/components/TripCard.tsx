import Link from 'next/link';
import Image from 'next/image';
import { Image as ImageIcon } from 'lucide-react';
import type { TripSummary } from '../types';
import { ROUTES } from '@/constants';
import { formatDateRange, daysBetween } from '@/lib/utils';
import { TripStatusBadge } from './TripStatusBadge';

interface TripCardProps {
  trip: TripSummary;
}

export function TripCard({ trip }: TripCardProps) {
  const days = daysBetween(trip.startDate, trip.endDate);

  return (
    <Link
      href={ROUTES.trip(trip.id)}
      className="group flex flex-col rounded-xl overflow-hidden bg-card border border-border/80 hover:border-border transition-all duration-200"
    >
      {/* Photo Container */}
      <div className="relative aspect-[16/10] w-full bg-muted overflow-hidden">
        {trip.coverImage ? (
          <Image
            src={trip.coverImage}
            alt={trip.name}
            fill
            className="object-cover transition-transform duration-500 ease-out group-hover:scale-[1.02]"
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
          />
        ) : (
          <div className="flex h-full items-center justify-center bg-muted text-muted-foreground/40">
            <ImageIcon className="h-10 w-10" />
          </div>
        )}
        <div className="absolute top-3 right-3">
          <TripStatusBadge status={trip.status} className="bg-background/90 backdrop-blur-sm shadow-xs" />
        </div>
      </div>

      {/* Content Section */}
      <div className="p-4 flex flex-col flex-1 justify-between gap-3">
        <div>
          <p className="text-xs font-semibold text-primary uppercase tracking-wider">
            {trip.destination}
          </p>
          <h3 className="font-semibold text-base text-foreground mt-1 line-clamp-1 group-hover:text-primary transition-colors">
            {trip.name}
          </h3>
        </div>

        <div className="pt-2 border-t border-border/40 flex items-center justify-between text-xs text-muted-foreground">
          <span>{formatDateRange(trip.startDate, trip.endDate)} ({days} ngày)</span>
          <span>{trip.placeCount} địa điểm</span>
        </div>
      </div>
    </Link>
  );
}
