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
      <div className="flex h-full items-center justify-center p-8">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="flex h-full">
      {/* Left panel: Itinerary list */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 max-w-xl">
        {/* Header action: Generate / Optimize */}
        <div className="flex items-center justify-between pb-2 border-b">
          <div>
            <h2 className="font-semibold text-base">Lịch trình</h2>
            <p className="text-xs text-muted-foreground">
              Kéo thả để sắp xếp các địa điểm theo ngày
            </p>
          </div>
          <button
            onClick={() => planningMutation.mutate()}
            disabled={isGenerating || planningMutation.isPending}
            className="flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90 disabled:opacity-60 transition"
          >
            {isGenerating ? (
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
            ) : (
              <Sparkles className="h-3.5 w-3.5" />
            )}
            {isGenerating ? AI_LOADING_MESSAGES[0] : '✨ AI gợi ý lịch trình'}
          </button>
        </div>

        {/* AI Recommendation Preview (Apply / Edit / Ignore) */}
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
          <div className="space-y-4">
            {itinerary?.days.map((day) => (
              <DayContainer
                key={day.dayIndex}
                day={day}
                tripName={itinerary.name}
              />
            ))}
          </div>

          <DragOverlay>
            {activeItem ? <PlaceCard item={activeItem} isDragging /> : null}
          </DragOverlay>
        </DndContext>
      </div>

      {/* Right panel: Google Map View */}
      <div className="hidden lg:block w-[45%] border-l h-full">
        <MapView markers={markers} />
      </div>
    </div>
  );
}
