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
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        TRIP_STATUS_COLORS[status],
        className
      )}
      aria-label={`Trạng thái: ${TRIP_STATUS_LABELS[status]}`}
    >
      {TRIP_STATUS_LABELS[status]}
    </span>
  );
}
