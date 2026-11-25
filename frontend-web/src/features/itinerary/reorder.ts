import type { Itinerary } from "./types";
export function reorderItinerary(
  itinerary: Itinerary,
  activeId: string,
  overId: string,
): Itinerary | null {
  if (activeId === overId) return null;
  const days = itinerary.days.map((day) => ({ ...day, items: [...day.items] }));
  const source = days.find((day) =>
    day.items.some((item) => item.id === activeId),
  );
  const target = days.find(
    (day) =>
      `day-${day.dayIndex}` === overId ||
      day.items.some((item) => item.id === overId),
  );
  if (!source || !target) return null;
  if (
    itinerary.readOnly ||
    (itinerary.editableFrom &&
      (source.date < itinerary.editableFrom ||
        target.date < itinerary.editableFrom)) ||
    source.items.some((item) => item.id === activeId && item.visited) ||
    target.items.some((item) => item.id === overId && item.visited)
  )
    return null;
  const sourceIndex = source.items.findIndex((item) => item.id === activeId);
  const targetIndex = target.items.findIndex((item) => item.id === overId);
  const [item] = source.items.splice(sourceIndex, 1);
  target.items.splice(
    targetIndex < 0 ? target.items.length : targetIndex,
    0,
    item,
  );
  days.forEach((day) => {
    if (day !== source && day !== target) return;
    day.items = day.items.map((item, order) => ({
      ...item,
      dayIndex: day.dayIndex,
      order,
      suggestedStartTime: item.visited ? item.suggestedStartTime : undefined,
      scheduledArrival: item.visited ? item.scheduledArrival : undefined,
      distanceFromPreviousKm: undefined,
      durationFromPreviousMinutes: undefined,
    }));
  });
  return { ...itinerary, days };
}
