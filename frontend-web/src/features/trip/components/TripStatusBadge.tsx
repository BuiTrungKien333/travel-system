import { cn } from '@/lib/utils';
import {
  TRIP_STATUS_LABELS,
  TRIP_STATUS_COLORS,
  type TripStatus,
} from '@/lib/constants';

interface TripStatusBadgeProps {
  status: TripStatus;
  className?: string;
}

export function TripStatusBadge({ status, className }: TripStatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-md px-2 py-0.5 text-xs font-medium border border-border/40',
        TRIP_STATUS_COLORS[status],
        className
      )}
      aria-label={`Trạng thái: ${TRIP_STATUS_LABELS[status]}`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current opacity-80" aria-hidden="true" />
      <span>{TRIP_STATUS_LABELS[status]}</span>
    </span>
  );
}
