'use client';

import { useState, use } from 'react';
import {
  DndContext,
  DragOverlay,
  closestCorners,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragStartEvent,
} from '@dnd-kit/core';
import { sortableKeyboardCoordinates } from '@dnd-kit/sortable';
import { Sparkles, Loader2 } from 'lucide-react';
import { useItineraryQuery } from '@/features/itinerary/api/useItineraryQuery';
import { usePlanningMutation } from '@/features/itinerary/api/usePlanningMutation';
import { useUpdateItineraryMutation } from '@/features/itinerary/api/useUpdateItineraryMutation';
import { useItineraryUIStore } from '@/features/itinerary/store/useItineraryUIStore';
import { DayContainer } from '@/features/itinerary/components/DayContainer';
import { PlaceCard } from '@/features/itinerary/components/PlaceCard';
import { PlanningResultPreview } from '@/features/itinerary/components/PlanningResultPreview';
import { MapView } from '@/features/map/components/MapView';
import { AI_LOADING_MESSAGES } from '@/lib/constants';
import type { ItineraryItem } from '@/features/itinerary/types';
import type { MapMarker } from '@/features/map/types';

export default function ItineraryPage({
  params,
}: {
  params: Promise<{ tripId: string }>;
}) {
  const resolvedParams = use(params);
  const { tripId } = resolvedParams;
  const { data: itinerary, isLoading } = useItineraryQuery(tripId);
  const planningMutation = usePlanningMutation(tripId);
  const updateItineraryMutation = useUpdateItineraryMutation(tripId);

  const isGenerating = useItineraryUIStore((s) => s.isGenerating);
  const pendingRecommendation = useItineraryUIStore(
    (s) => s.pendingRecommendation
  );
  const setPendingRecommendation = useItineraryUIStore(
    (s) => s.setPendingRecommendation
  );

  const [activeItem, setActiveItem] = useState<ItineraryItem | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

  const handleDragStart = (event: DragStartEvent) => {
    const { active } = event;
    const item = itinerary?.days
      .flatMap((d) => d.items)
      .find((i) => i.id === active.id);
    if (item) setActiveItem(item);
  };

  const handleDragEnd = () => {
    setActiveItem(null);
  };

  const handleApplyRecommendation = () => {
    if (!pendingRecommendation) return;
    updateItineraryMutation.mutate({});
  };

  const handleIgnoreRecommendation = () => {
    setPendingRecommendation(null);
  };

  // Convert itinerary items to MapMarkers
  const markers: MapMarker[] =
    itinerary?.days.flatMap((day) =>
      day.items
        .filter((item) => item.placeLat && item.placeLng)
        .map((item) => ({
          placeId: item.placeId,
          placeName: item.placeName,
          lat: item.placeLat!,
          lng: item.placeLng!,
          dayIndex: day.dayIndex,
          order: item.order + 1,
          isVisited: !!item.visitedAt,
        }))
    ) ?? [];

  if (isLoading) {
    return (
      <div className="p-6 space-y-4 max-w-2xl animate-pulse">
        <div className="h-8 w-48 bg-muted rounded-lg" />
        <div className="h-4 w-72 bg-muted rounded" />
        <div className="space-y-3 pt-4">
          <div className="h-32 bg-muted rounded-xl" />
          <div className="h-32 bg-muted rounded-xl" />
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-full bg-background overflow-hidden">
      {/* Left panel: Itinerary list */}
      <div className="flex-1 overflow-y-auto px-4 py-6 sm:px-6 space-y-5 max-w-2xl hide-scrollbar">
        
        {/* Header action */}
        <div className="flex items-center justify-between gap-4 pb-4 border-b border-border/60">
          <div>
            <h2 className="text-xl font-bold tracking-tight text-foreground">
              Lịch trình
            </h2>
            <p className="text-xs text-muted-foreground mt-0.5">
              Kéo thả để sắp xếp thứ tự hoặc nhận gợi ý từ AI
            </p>
          </div>

          <button
            onClick={() => planningMutation.mutate()}
            disabled={isGenerating || planningMutation.isPending}
            className="flex items-center gap-1.5 rounded-lg bg-primary px-3.5 py-2 text-xs font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors shadow-xs shrink-0"
          >
            {isGenerating ? (
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
            ) : (
              <Sparkles className="h-3.5 w-3.5" />
            )}
            <span>{isGenerating ? AI_LOADING_MESSAGES[0] : 'AI gợi ý lịch trình'}</span>
          </button>
        </div>

        {/* AI Recommendation Preview */}
        {pendingRecommendation && (
          <PlanningResultPreview
            recommendation={pendingRecommendation}
            onApply={handleApplyRecommendation}
            onIgnore={handleIgnoreRecommendation}
          />
        )}

        {/* Days list with drag & drop */}
        <DndContext
          sensors={sensors}
          collisionDetection={closestCorners}
          onDragStart={handleDragStart}
          onDragEnd={handleDragEnd}
        >
          <div className="space-y-5 pb-20">
            {itinerary?.days.map((day) => (
              <DayContainer
                key={day.dayIndex}
                day={day}
                tripName={itinerary.name}
              />
            ))}
          </div>

          <DragOverlay dropAnimation={{
            duration: 180,
            easing: 'cubic-bezier(0.25, 0.1, 0.25, 1)',
          }}>
            {activeItem ? (
              <div className="opacity-90 shadow-lg">
                <PlaceCard item={activeItem} isDragging />
              </div>
            ) : null}
          </DragOverlay>
        </DndContext>
      </div>

      {/* Right panel: Google Map View */}
      <div className="hidden lg:block w-[45%] xl:w-[50%] border-l border-border/60 h-full relative bg-muted/20">
        <MapView markers={markers} />
      </div>
    </div>
  );
}
