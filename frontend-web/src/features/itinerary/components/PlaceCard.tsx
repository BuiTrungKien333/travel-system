'use client';

import Image from 'next/image';
import { MapPin, Clock, CheckCircle2, Image as ImageIcon, GripVertical, Navigation } from 'lucide-react';
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
        'group relative rounded-xl border bg-card p-3.5 transition-all duration-150',
        'hover:border-primary/40',
        isSelected ? 'border-primary ring-1 ring-primary/20 shadow-xs' : 'border-border/70',
        isDragging && 'opacity-60 scale-[0.99] shadow-md z-50'
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
      <div className="flex gap-3 items-start">
        {/* Drag Handle & Step Index */}
        <div className="flex items-center gap-1.5 pt-0.5 shrink-0">
          <div
            {...dragHandleProps}
            className="p-1 rounded text-muted-foreground/30 hover:text-muted-foreground cursor-grab active:cursor-grabbing transition-colors"
            aria-label="Kéo để sắp xếp"
          >
            <GripVertical className="h-4 w-4" />
          </div>

          <div
            className={cn(
              'flex h-6 w-6 items-center justify-center rounded-md text-xs font-semibold',
              isVisited
                ? 'bg-primary text-primary-foreground'
                : 'bg-muted text-muted-foreground group-hover:text-primary transition-colors'
            )}
          >
            {isVisited ? <CheckCircle2 className="h-3.5 w-3.5" /> : item.order + 1}
          </div>
        </div>

        {/* Thumbnail Image */}
        <div className="relative h-16 w-16 shrink-0 rounded-lg overflow-hidden bg-muted border border-border/50">
          {item.placeImage ? (
            <Image
              src={item.placeImage}
              alt={item.placeName}
              fill
              className="object-cover group-hover:scale-105 transition-transform duration-300"
              sizes="64px"
            />
          ) : (
            <div className="flex h-full items-center justify-center text-muted-foreground/40">
              <ImageIcon className="h-5 w-5" />
            </div>
          )}
        </div>

        {/* Main Content */}
        <div className="flex-1 min-w-0 space-y-1">
          <div className="flex items-start justify-between gap-2">
            <h4 className="font-semibold text-sm text-foreground truncate group-hover:text-primary transition-colors">
              {item.placeName}
            </h4>

            {item.suggestedStartTime && (
              <span className="flex items-center gap-1 text-[11px] font-mono text-muted-foreground shrink-0">
                <Clock className="h-3 w-3" />
                {item.suggestedStartTime}
              </span>
            )}
          </div>

          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            {item.placeCategory && (
              <span className="font-medium text-primary/80 truncate">
                {item.placeCategory}
              </span>
            )}
            {item.placeAddress && (
              <>
                <span className="opacity-40">·</span>
                <span className="truncate flex items-center gap-1">
                  <MapPin className="h-3 w-3 shrink-0" />
                  <span className="truncate">{item.placeAddress}</span>
                </span>
              </>
            )}
          </div>

          {item.notes && (
            <p className="text-xs text-muted-foreground/90 italic pt-0.5 line-clamp-2">
              &ldquo;{item.notes}&rdquo;
            </p>
          )}

          {/* Quiet Action Row */}
          <div className="pt-2 flex items-center gap-3 text-xs">
            <button
              onClick={(e) => {
                e.stopPropagation();
                // TODO: Connect mark as visited API
              }}
              className={cn(
                "font-medium transition-colors hover:underline",
                isVisited ? "text-primary" : "text-muted-foreground hover:text-foreground"
              )}
            >
              {isVisited ? '✓ Đã ghé thăm' : 'Đánh dấu đã ghé'}
            </button>
            <span className="opacity-30">·</span>
            <button
              onClick={(e) => e.stopPropagation()}
              className="text-muted-foreground hover:text-foreground transition-colors flex items-center gap-1"
            >
              <Navigation className="h-3 w-3" />
              <span>Chỉ đường</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
