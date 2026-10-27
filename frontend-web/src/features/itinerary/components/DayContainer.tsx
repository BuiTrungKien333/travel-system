'use client';

import { useState } from 'react';
import {
  SortableContext,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { useDroppable } from '@dnd-kit/core';
import { ChevronDown, ChevronUp, Calendar, Map, CloudSun } from 'lucide-react';
import { cn, formatDateShort } from '@/lib/utils';
import type { ItineraryDay } from '../types';
import { SortablePlaceCard } from './SortablePlaceCard';
import { TravelInfo } from './TravelInfo';

interface DayContainerProps {
  day: ItineraryDay;
  tripName: string;
}

export function DayContainer({ day }: DayContainerProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const { setNodeRef, isOver } = useDroppable({ id: `day-${day.dayIndex}` });

  return (
    <div
      ref={setNodeRef}
      className={cn(
        'rounded-xl border bg-card transition-colors duration-150 overflow-hidden shadow-xs',
        isOver ? 'border-primary ring-1 ring-primary/20 bg-primary/[0.02]' : 'border-border/80'
      )}
    >
      {/* Day Section Header */}
      <div
        className={cn(
          "flex items-center justify-between px-4 py-3 cursor-pointer transition-colors hover:bg-muted/40",
          !isCollapsed && "border-b border-border/60 bg-muted/20"
        )}
        onClick={() => setIsCollapsed((c) => !c)}
      >
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <Calendar className="h-4 w-4" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-bold text-sm text-foreground">Ngày {day.dayIndex + 1}</h3>
              <span className="text-xs text-muted-foreground">·</span>
              <span className="text-xs font-medium text-muted-foreground">{formatDateShort(day.date)}</span>
            </div>
            <div className="flex items-center gap-3 text-xs text-muted-foreground mt-0.5">
              <span>{day.items.length} địa điểm</span>
              <span>·</span>
              <span className="flex items-center gap-1 text-muted-foreground/80">
                <CloudSun className="h-3 w-3 text-primary/70" />
                28°C · Nắng đẹp
              </span>
            </div>
          </div>
        </div>

        <button
          className="p-1.5 rounded-md hover:bg-muted text-muted-foreground transition-colors"
          aria-label={isCollapsed ? 'Mở rộng ngày' : 'Thu gọn ngày'}
          aria-expanded={!isCollapsed}
        >
          {isCollapsed ? (
            <ChevronDown className="h-4 w-4" />
          ) : (
            <ChevronUp className="h-4 w-4" />
          )}
        </button>
      </div>

      {/* Day Items List */}
      {!isCollapsed && (
        <div className="p-3 sm:p-4 space-y-0 relative bg-card">
          {day.items.length === 0 ? (
            <div className="py-6 flex flex-col items-center justify-center text-center">
              <Map className="h-5 w-5 text-muted-foreground/40 mb-2" />
              <p className="text-xs font-medium text-foreground">Ngày này chưa có địa điểm</p>
              <p className="text-xs text-muted-foreground mt-0.5 max-w-xs">
                Kéo thả địa điểm từ ngày khác hoặc thêm địa điểm mới vào đây.
              </p>
            </div>
          ) : (
            <SortableContext
              items={day.items.map((item) => item.id)}
              strategy={verticalListSortingStrategy}
            >
              {day.items.map((item, index) => (
                <div key={item.id} className="relative z-10">
                  {index > 0 && (
                    <TravelInfo
                      distanceKm={item.distanceFromPreviousKm}
                      durationMinutes={item.durationFromPreviousMinutes}
                    />
                  )}
                  <SortablePlaceCard item={item} />
                </div>
              ))}
            </SortableContext>
          )}
        </div>
      )}
    </div>
  );
}
