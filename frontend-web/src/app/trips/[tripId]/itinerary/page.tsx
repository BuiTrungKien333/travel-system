"use client";

import { useState, use, useEffect, useMemo } from "react";
import { TripPlacePicker } from "@/features/place/components/TripPlacePicker";
import { TripWeatherPanel } from "@/features/weather/components/TripWeatherPanel";
import { reorderItinerary } from "@/features/itinerary/reorder";
import {
  DndContext,
  DragOverlay,
  closestCorners,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragStartEvent,
  type DragEndEvent,
} from "@dnd-kit/core";
import { sortableKeyboardCoordinates } from "@dnd-kit/sortable";
import { Sparkles, Loader2, Map, List } from "lucide-react";
import { useItineraryQuery } from "@/features/itinerary/api/useItineraryQuery";
import { usePlanningMutation } from "@/features/itinerary/api/usePlanningMutation";
import { useUpdateItineraryMutation } from "@/features/itinerary/api/useUpdateItineraryMutation";
import { useItineraryUIStore } from "@/features/itinerary/store/useItineraryUIStore";
import { DayContainer } from "@/features/itinerary/components/DayContainer";
import { PlaceCard } from "@/features/itinerary/components/PlaceCard";
import { PlanningResultPreview } from "@/features/itinerary/components/PlanningResultPreview";
import { AI_LOADING_MESSAGES } from "@/lib/constants";
import type { ItineraryItem } from "@/features/itinerary/types";
import type { MapMarker } from "@/features/map/types";
import { MapboxView } from "@/features/map/components/MapBoxView";
import { useItineraryConnectionsQuery } from "@/features/itinerary/api/useItineraryConnectionsQuery";
import {
  itineraryRouteGroups,
  itineraryDayColor,
} from "@/features/itinerary/connections";
import { formatDistance, formatDuration } from "@/lib/utils";

export default function ItineraryPage({
  params,
}: {
  params: Promise<{ tripId: string }>;
}) {
  const resolvedParams = use(params);
  const { tripId } = resolvedParams;
  const {
    data: itinerary,
    isLoading,
    error,
    refetch,
  } = useItineraryQuery(tripId);
  const connections = useItineraryConnectionsQuery(tripId, itinerary);
  const routeGroups = useMemo(
    () => itineraryRouteGroups(itinerary, connections.data),
    [itinerary, connections.data],
  );
  const planningMutation = usePlanningMutation(tripId);
  const updateItineraryMutation = useUpdateItineraryMutation(tripId);

  const isGenerating = useItineraryUIStore((s) => s.isGenerating);
  const pendingRecommendation = useItineraryUIStore(
    (s) => s.pendingRecommendation,
  );
  const setPendingRecommendation = useItineraryUIStore(
    (s) => s.setPendingRecommendation,
  );

  useEffect(() => {
    setPendingRecommendation(null);
  }, [tripId, setPendingRecommendation]);
  const [activeItem, setActiveItem] = useState<ItineraryItem | null>(null);
  const [mobileMap, setMobileMap] = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const handleDragStart = (event: DragStartEvent) => {
    const { active } = event;
    const item = itinerary?.days
      .flatMap((d) => d.items)
      .find((i) => i.id === active.id);
    if (item) setActiveItem(item);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setActiveItem(null);
    if (!itinerary || !event.over || updateItineraryMutation.isPending) return;
    const updated = reorderItinerary(
      itinerary,
      String(event.active.id),
      String(event.over.id),
    );
    if (updated) {
      connections.preserveManualOrder(updated);
      updateItineraryMutation.mutate(updated);
    }
  };

  const handleApplyRecommendation = () => {
    if (!pendingRecommendation) return;
    if (!updateItineraryMutation.isPending)
      updateItineraryMutation.mutate(pendingRecommendation);
  };

  const handleIgnoreRecommendation = () => {
    setPendingRecommendation(null);
  };

  // Convert itinerary items to MapMarkers
  const markers: MapMarker[] = useMemo(
    () =>
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
            isVisited: item.visited ?? !!item.visitedAt,
          })),
      ) ?? [],
    [itinerary],
  );

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
    <div className="relative flex h-full bg-background overflow-hidden">
      <button
        type="button"
        aria-pressed={mobileMap}
        onClick={() => setMobileMap(!mobileMap)}
        className="absolute bottom-5 right-5 z-20 inline-flex items-center gap-2 rounded-full bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground shadow-lg lg:hidden"
      >
        {mobileMap ? <List className="h-4 w-4" /> : <Map className="h-4 w-4" />}
        {mobileMap ? "Xem lịch trình" : "Xem bản đồ"}
      </button>
      {/* Left panel: Itinerary list */}
      <div
        className={`${mobileMap ? "hidden lg:block" : "block"} min-w-0 flex-1 overflow-y-auto px-4 py-6 sm:px-6 space-y-5 max-w-2xl hide-scrollbar`}
      >
        {/* Header action */}
        <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-border/60">
          <div>
            <h2 className="text-xl font-bold tracking-tight text-foreground">
              Lên lịch trình
            </h2>
            <p className="text-xs text-muted-foreground mt-0.5">
              Tự sắp xếp theo khoảng cách; bạn có thể kéo thả để đổi thứ tự.
            </p>
          </div>

          <button
            onClick={() => planningMutation.mutate()}
            disabled={
              itinerary?.readOnly ||
              isGenerating ||
              planningMutation.isPending ||
              connections.isFetching
            }
            className="flex items-center gap-1.5 rounded-lg bg-primary px-3.5 py-2 text-xs font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors shadow-xs shrink-0"
          >
            {isGenerating ? (
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
            ) : (
              <Sparkles className="h-3.5 w-3.5" />
            )}
            <span>
              {isGenerating ? AI_LOADING_MESSAGES[0] : "Tối ưu lịch trình"}
            </span>
          </button>
        </div>

        {(error || planningMutation.error || updateItineraryMutation.error) && (
          <div role="alert" className="text-sm text-destructive">
            {
              (error || planningMutation.error || updateItineraryMutation.error)
                ?.message
            }
            <button onClick={() => refetch()} className="ml-2 underline">
              Tải lại lịch trình
            </button>
          </div>
        )}
        <TripWeatherPanel tripId={tripId} />
        {connections.isFetching && (
          <p role="status" className="text-xs text-muted-foreground">
            Đang nối tuyến từng ngày...
          </p>
        )}
        {connections.error && (
          <p role="alert" className="text-sm text-destructive">
            {connections.error.message}{" "}
            <button className="underline" onClick={() => connections.refetch()}>
              Thử nối lại
            </button>
          </p>
        )}
        {itinerary?.readOnly && (
          <p className="rounded-lg bg-muted p-3 text-sm text-muted-foreground">
            Chuyến đi đã kết thúc hoặc đã hủy. Lịch trình chỉ để xem lại.
          </p>
        )}
        {itinerary && !itinerary.readOnly && (
          <TripPlacePicker key={tripId} tripId={tripId} itinerary={itinerary} />
        )}
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
                readOnly={
                  itinerary.readOnly ||
                  connections.isFetching ||
                  updateItineraryMutation.isPending ||
                  !!(
                    itinerary.editableFrom && day.date < itinerary.editableFrom
                  )
                }
              />
            ))}
          </div>

          <DragOverlay
            dropAnimation={{
              duration: 180,
              easing: "cubic-bezier(0.25, 0.1, 0.25, 1)",
            }}
          >
            {activeItem ? (
              <div className="opacity-90 shadow-lg">
                <PlaceCard item={activeItem} isDragging />
              </div>
            ) : null}
          </DragOverlay>
        </DndContext>
      </div>

      {/* Right panel: Mapbox View */}
      <div
        className={`${mobileMap ? "block" : "hidden"} lg:block w-full lg:w-[45%] xl:w-[50%] border-l border-border/60 h-full relative bg-muted/20`}
      >
        <MapboxView
          markers={markers}
          routeGroups={routeGroups}
          className="w-full h-full"
        />
        {!!routeGroups.length && (
          <div
            data-testid="itinerary-route-legend"
            className="absolute left-3 top-3 z-10 max-w-[calc(100%_-_5rem)] space-y-2 rounded-xl border bg-background/95 p-3 shadow-sm text-xs"
          >
            {routeGroups.map((group) => {
              const day = itinerary!.days.find((day) => day.date === group.id)!;
              const result =
                connections.data &&
                connections.data.itinerary.updatedAt === itinerary?.updatedAt
                  ? connections.data.days.find((day) => day.date === group.id)
                  : undefined;
              return (
                <p
                  key={group.id}
                  className="flex flex-wrap items-center gap-1.5"
                >
                  <span
                    className="h-2.5 w-2.5 rounded-full"
                    style={{ backgroundColor: itineraryDayColor(day.dayIndex) }}
                  />
                  <span className="font-semibold">Ngày {day.dayIndex + 1}</span>
                  {result?.route ? (
                    <span>
                      · {formatDistance(result.route.distanceMeters / 1000)} ·{" "}
                      {formatDuration(
                        Math.ceil(result.route.durationSeconds / 60),
                      )}
                    </span>
                  ) : (
                    <span>· Nối điểm tham khảo</span>
                  )}
                </p>
              );
            })}
            {routeGroups.some((group) => group.estimated) && (
              <p className="max-w-64 text-muted-foreground">
                Đường mờ chỉ nối các điểm, chưa phải đường đi thực tế.
                {connections.data?.days.some(
                  (day) => day.routeErrorCode === "NAV_002",
                ) && " Geoapify đã đạt hạn mức."}{" "}
                <button
                  className="underline"
                  disabled={connections.isFetching}
                  onClick={() => connections.refetch()}
                >
                  Tải tuyến đường
                </button>
              </p>
            )}
            {connections.data?.days.some(
              (day) => day.optimization === "LIMIT_EXCEEDED",
            ) && (
              <p className="max-w-64 text-muted-foreground">
                Tối ưu tự động hỗ trợ tối đa 16 điểm/ngày, tính cả điểm xuất
                phát.
              </p>
            )}
            {connections.data?.days.some((day) => day.route) && (
              <p className="text-[10px] text-muted-foreground">
                Geoapify · © OpenStreetMap contributors
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
