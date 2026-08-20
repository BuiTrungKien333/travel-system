import { Clock, Milestone } from 'lucide-react';
import { formatDuration, formatDistance } from '@/lib/utils';

interface TravelInfoProps {
  distanceKm?: number;
  durationMinutes?: number;
}

export function TravelInfo({ distanceKm, durationMinutes }: TravelInfoProps) {
  if (!distanceKm && !durationMinutes) {
    return (
      <div className="flex justify-center py-1">
        <div className="w-px h-6 bg-border" aria-hidden />
      </div>
    );
  }

  return (
    <div className="flex items-center gap-2 py-1 px-4 justify-center">
      <div className="w-px h-3 bg-border" aria-hidden />
      <div className="flex items-center gap-3 rounded-full border bg-muted/50 px-3 py-1 text-xs text-muted-foreground">
        {durationMinutes !== undefined && (
          <span className="flex items-center gap-1">
            <Clock className="h-3 w-3" />
            {formatDuration(durationMinutes)}
          </span>
        )}
        {distanceKm !== undefined && (
          <span className="flex items-center gap-1">
            <Milestone className="h-3 w-3" />
            {formatDistance(distanceKm)}
          </span>
        )}
      </div>
      <div className="w-px h-3 bg-border" aria-hidden />
    </div>
  );
}
