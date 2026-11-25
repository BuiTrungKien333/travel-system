'use client';

import { MapPin, ExternalLink } from 'lucide-react';
import { formatDistanceToNow, parseISO } from 'date-fns';
import { vi } from 'date-fns/locale';

interface LocationGroupHeaderProps {
  locationName: string;
  address?: string;
  timestamp: string;
  latitude?: number;
  longitude?: number;
}

export function LocationGroupHeader({
  locationName,
  address,
  timestamp,
  latitude,
  longitude,
}: LocationGroupHeaderProps) {
  const timeAgo = (() => {
    try {
      return formatDistanceToNow(parseISO(timestamp), {
        addSuffix: true,
        locale: vi,
      });
    } catch {
      return '';
    }
  })();

  const mapsUrl =
    latitude && longitude
      ? `https://www.google.com/maps?q=${latitude},${longitude}`
      : undefined;

  return (
    <div className="mb-3.5">
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-start gap-2">
          <span className="text-xl mt-0.5">📍</span>
          <div>
            <h3 className="text-lg font-bold text-slate-900 leading-tight">
              {locationName}
            </h3>
            <p className="text-xs text-slate-500 font-medium mt-0.5">{timeAgo}</p>
          </div>
        </div>

        {mapsUrl && (
          <a
            href={mapsUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="flex-shrink-0 rounded-full bg-slate-100 hover:bg-slate-200 p-2 text-slate-600 transition-colors shadow-sm"
            aria-label="Xem trên Google Maps"
          >
            <ExternalLink className="h-4 w-4" />
          </a>
        )}
      </div>

      {address && (
        <div className="ml-8 mt-1.5">
          <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600 border border-slate-200/80">
            <MapPin className="h-3 w-3 text-cyan-600" />
            {address}
          </span>
        </div>
      )}
    </div>
  );
}
