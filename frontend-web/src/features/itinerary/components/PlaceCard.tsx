'use client';

import Image from 'next/image';
import { MapPin, Clock, CheckCircle2, Image as ImageIcon, GripVertical } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useItineraryUIStore } from '../store/useItineraryUIStore';
import type { ItineraryItem } from '../types';

interface PlaceCardProps {
  item: ItineraryItem;
  isDragging?: boolean;
  dragHandleProps?: React.HTMLAttributes<HTMLDivElement>;
}

export function PlaceCard({ item, isDragging, dragHandleProps }: PlaceCardProps) {
  const setHoveredPlaceId = useItineraryUIStore((s) => s.setHoveredPlaceId);
  const setSelectedPlaceId = useItineraryUIStore((s) => s.setSelectedPlaceId);
  const selectedPlaceId = useItineraryUIStore((s) => s.selectedPlaceId);

  const isSelected = selectedPlaceId === item.placeId;
  const isVisited = !!item.visitedAt;

  return (
    <div
      id={`place-card-${item.placeId}`}
      className={cn(
        'group relative rounded-lg border bg-card shadow-sm transition-all duration-150',
        'hover:shadow-md hover:border-ring/50',
        isSelected && 'border-ring shadow-md',
        isDragging && 'opacity-50 scale-[0.98]'
      )}
      onMouseEnter={() => setHoveredPlaceId(item.placeId)}
      onMouseLeave={() => setHoveredPlaceId(null)}
      onClick={() => setSelectedPlaceId(item.placeId)}
      role="button"
      tabIndex={0}
      aria-pressed={isSelected}
      aria-label={`Địa điểm: ${item.placeName}`}
      onKeyDown={(e) => e.key === 'Enter' && setSelectedPlaceId(item.placeId)}
    >
      <div className="flex gap-3 p-3">
        <div
          {...dragHandleProps}
          className="flex items-center text-muted-foreground/40 hover:text-muted-foreground cursor-grab active:cursor-grabbing flex-shrink-0"
          aria-label="Kéo để sắp xếp"
        >
          <GripVertical className="h-4 w-4" />
        </div>

        <div className="flex-shrink-0 mt-0.5">
          <span className={cn(
            'flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold',
            isVisited
              ? 'bg-green-100 text-green-700'
              : 'bg-primary/10 text-primary'
          )}>
            {isVisited ? <CheckCircle2 className="h-3.5 w-3.5" /> : item.order + 1}
          </span>
        </div>

        <div className="relative h-16 w-16 flex-shrink-0 rounded-md overflow-hidden bg-muted">
          {item.placeImage ? (
            <Image
              src={item.placeImage}
              alt={`Ảnh ${item.placeName}`}
              fill
              className="object-cover"
              sizes="64px"
            />
          ) : (
            <div className="flex h-full items-center justify-center">
              <ImageIcon className="h-5 w-5 text-muted-foreground/40" />
            </div>
          )}
        </div>

        <div className="min-w-0 flex-1">
          <h4 className="font-medium text-sm leading-tight truncate">{item.placeName}</h4>
          {item.placeAddress && (
            <p className="flex items-center gap-1 text-xs text-muted-foreground mt-0.5 truncate">
              <MapPin className="h-3 w-3 flex-shrink-0" />
              {item.placeAddress}
            </p>
          )}
          {item.suggestedStartTime && (
            <p className="flex items-center gap-1 text-xs text-muted-foreground mt-1">
              <Clock className="h-3 w-3" />
              {item.suggestedStartTime}
              {item.suggestedDurationMinutes && (
                <span>· {Math.round(item.suggestedDurationMinutes / 60 * 10) / 10}h</span>
              )}
            </p>
          )}
          {item.notes && (
            <p className="text-xs text-muted-foreground/70 mt-1 line-clamp-1">{item.notes}</p>
          )}
        </div>
      </div>
    </div>
  );
}
