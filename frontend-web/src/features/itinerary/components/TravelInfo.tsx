import { Clock, Milestone } from 'lucide-react';
import { formatDuration, formatDistance } from '@/lib/utils';

interface TravelInfoProps {
  distanceKm?: number;
  durationMinutes?: number;
}

export function TravelInfo({ distanceKm, durationMinutes }: TravelInfoProps) {
  if (!distanceKm && !durationMinutes) {
    return (
      <div className="flex justify-center py-2 relative">
        <div className="absolute top-0 bottom-0 left-[2.2rem] w-[2px] bg-border/50" aria-hidden />
      </div>
    );
  }

  return (
    <div className="relative py-3 pl-[2.2rem] pr-4">
      {/* Route connector line */}
      <div className="absolute top-0 bottom-0 left-[2.2rem] w-[2px] bg-border/50" aria-hidden />

      <div className="relative flex items-center gap-4 bg-muted/30 w-fit rounded-lg px-3 py-1.5 ml-4 border border-border/50 shadow-sm backdrop-blur-sm">
        {durationMinutes !== undefined && (
          <span className="flex items-center gap-1.5 text-[11px] font-medium text-muted-foreground">
            <Clock className="h-3 w-3 text-primary/70" />
            {formatDuration(durationMinutes)}
          </span>
        )}
        {distanceKm !== undefined && durationMinutes !== undefined && (
          <div className="w-1 h-1 rounded-full bg-border" />
        )}
        {distanceKm !== undefined && (
          <span className="flex items-center gap-1.5 text-[11px] font-medium text-muted-foreground">
            <Milestone className="h-3 w-3 text-primary/70" />
            {formatDistance(distanceKm)}
          </span>
        )}
      </div>
    </div>
  );
}
