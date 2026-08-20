'use client';

import { useState } from 'react';
import {
  SortableContext,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { useDroppable } from '@dnd-kit/core';
import { ChevronDown, ChevronUp, Calendar } from 'lucide-react';
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
        'rounded-xl border bg-card shadow-sm transition-colors',
        isOver && 'border-primary/50 bg-primary/5'
      )}
    >
      <div className="flex items-center justify-between px-4 py-3 border-b">
        <div className="flex items-center gap-2">
          <Calendar className="h-4 w-4 text-muted-foreground" />
          <span className="font-semibold text-sm">
            Ngày {day.dayIndex + 1}
          </span>
          <span className="text-sm text-muted-foreground">
            {formatDateShort(day.date)}
          </span>
          <span className="ml-2 rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
            {day.items.length} địa điểm
          </span>
        </div>
        <button
          onClick={() => setIsCollapsed((c) => !c)}
          className="p-1 rounded hover:bg-muted transition-colors"
          aria-label={isCollapsed ? 'Mở rộng ngày' : 'Thu gọn ngày'}
          aria-expanded={!isCollapsed}
        >
          {isCollapsed ? (
            <ChevronDown className="h-4 w-4 text-muted-foreground" />
          ) : (
            <ChevronUp className="h-4 w-4 text-muted-foreground" />
          )}
        </button>
      </div>

      {!isCollapsed && (
        <div className="p-3 space-y-0">
          {day.items.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">
              Chưa có địa điểm nào trong ngày này
            </p>
          ) : (
            <SortableContext
              items={day.items.map((item) => item.id)}
              strategy={verticalListSortingStrategy}
            >
              {day.items.map((item, index) => (
                <div key={item.id}>
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
