"use client";
import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { Navigation, LocateFixed, Loader2, MapPin } from "lucide-react";
import { useItineraryQuery } from "@/features/itinerary/api/useItineraryQuery";
import { useTripNavigationMutation } from "../api/useTripNavigationMutation";
import { maneuverText, type NavigationVehicle } from "../navigation";
import { MapboxView } from "./MapBoxView";
import { ROUTES } from "@/constants";
import { formatDateShort, formatDistance, formatDuration } from "@/lib/utils";
import type { MapMarker } from "../types";

export function TripNavigation({
  tripId,
  initialDay = 0,
  initialPlaceId,
}: {
  tripId: string;
  initialDay?: number;
  initialPlaceId?: string;
}) {
  const itinerary = useItineraryQuery(tripId);
  const [dayIndex, setDayIndex] = useState(initialDay);
  const [targetPlaceId, setTargetPlaceId] = useState(initialPlaceId ?? "");
  const [vehicle, setVehicle] = useState<NavigationVehicle>("DRIVE");
  const navigation = useTripNavigationMutation(tripId);
  const day =
    itinerary.data?.days.find((day) => day.dayIndex === dayIndex) ??
    itinerary.data?.days[0];
  const requestedIndex =
    day?.items.findIndex((item) => item.placeId === targetPlaceId) ?? -1;
  const nextStopIndex = Math.max(0, requestedIndex);
  const destination = day?.items[nextStopIndex];
  const route = navigation.isPending ? undefined : navigation.data;
  const { reset } = navigation;
  useEffect(() => {
    reset();
  }, [itinerary.data?.updatedAt, reset]);
  const markers: MapMarker[] = useMemo(
    () =>
      (day?.items ?? [])
        .filter((item) => item.placeLat != null && item.placeLng != null)
        .map((item) => ({
          placeId: item.placeId,
          placeName: item.placeName,
          lat: item.placeLat!,
          lng: item.placeLng!,
          dayIndex: item.dayIndex,
          order: item.order + 1,
          isVisited: item.visited,
        })),
    [day],
  );
  const geometry = useMemo(
    () => route?.legs.map((leg) => leg.geometry) ?? [],
    [route],
  );
  return (
    <div className="flex h-full flex-col overflow-y-auto lg:flex-row lg:overflow-hidden">
      <section className="space-y-5 p-4 sm:p-6 lg:w-[380px] lg:shrink-0 lg:overflow-y-auto">
        <div>
          <h2 className="flex items-center gap-2 text-xl font-bold">
            <Navigation className="h-5 w-5 text-primary" />
            Chỉ đường
          </h2>

        </div>
        {itinerary.error && (
          <p role="alert" className="text-sm text-destructive">
            {itinerary.error.message}
            <button
              onClick={() => itinerary.refetch()}
              className="ml-2 underline"
            >
              Thử lại
            </button>
          </p>
        )}
        {itinerary.isLoading && <p role="status">Đang tải lịch trình...</p>}
        <label className="block space-y-1 text-xs font-semibold">
          Ngày di chuyển
          <select
            aria-label="Ngày chỉ đường"
            disabled={navigation.isPending}
            value={day?.dayIndex ?? 0}
            onChange={(e) => {
              reset();
              setDayIndex(Number(e.target.value));
              setTargetPlaceId("");
            }}
            className="block w-full rounded-lg border bg-background p-2.5 text-sm"
          >
            {itinerary.data?.days.map((day) => (
              <option key={day.date} value={day.dayIndex}>
                Ngày {day.dayIndex + 1} · {formatDateShort(day.date)}
              </option>
            ))}
          </select>
        </label>
        <label className="block space-y-1 text-xs font-semibold">
          Phương tiện
          <select
            disabled={navigation.isPending}
            value={vehicle}
            onChange={(e) => {
              reset();
              setVehicle(e.target.value as NavigationVehicle);
            }}
            className="block w-full rounded-lg border bg-background p-2.5 text-sm"
          >
            <option value="DRIVE">Ô tô</option>
            <option value="TWO_WHEELER">Xe máy</option>
          </select>
        </label>
        {!!day?.items.length && (
          <label className="block space-y-1 text-xs font-semibold">
            Đi đến điểm tiếp theo
            <select
              disabled={navigation.isPending}
              value={destination?.placeId ?? ""}
              onChange={(e) => {
                reset();
                setTargetPlaceId(e.target.value);
              }}
              className="block w-full rounded-lg border bg-background p-2.5 text-sm"
            >
              {day.items.map((item, index) => (
                <option key={item.id} value={item.placeId}>
                  {index + 1}. {item.placeName}
                </option>
              ))}
            </select>
          </label>
        )}
        {!itinerary.isLoading && !day?.items.length && (
          <p className="rounded-xl border border-dashed p-4 text-sm text-muted-foreground">
            Ngày này chưa có địa điểm.{" "}
            <Link
              href={ROUTES.tripItinerary(tripId)}
              className="font-medium text-primary"
            >
              Lên lịch trình
            </Link>
          </p>
        )}
        <button
          disabled={
            !destination ||
            navigation.isPending ||
            (day?.items.length ?? 0) > 24
          }
          onClick={() =>
            day &&
            navigation.mutate({
              date: day.date,
              nextStopIndex,
              expectedPlaceIds: day.items.map((item) => item.placeId),
              vehicle,
            })
          }
          className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground disabled:opacity-50"
        >
          {navigation.isPending ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <LocateFixed className="h-4 w-4" />
          )}
          {navigation.isPending
            ? "Đang tìm đường..."
            : "Tìm đường từ vị trí hiện tại"}
        </button>
        {(day?.items.length ?? 0) > 24 && (
          <p role="alert" className="text-sm text-muted-foreground">
            Tuyến chỉ đường hỗ trợ tối đa 24 địa điểm trong ngày.
          </p>
        )}
        {navigation.error && (
          <p
            role="alert"
            className="rounded-lg bg-destructive/5 p-3 text-sm text-destructive"
          >
            {navigation.error.message}
          </p>
        )}
        {route && (
          <div className="space-y-3 rounded-xl border p-4">
            <p className="text-xs font-medium text-muted-foreground">
              Tuyến đường Geoapify
            </p>
            <p className="font-semibold">
              {formatDistance(route.distanceMeters / 1000)} ·{" "}
              {formatDuration(Math.ceil(route.durationSeconds / 60))}
            </p>
            {route.legs.map((leg, index) => (
              <div key={index} className="space-y-2 text-sm">
                <h3 className="font-semibold">
                  Chặng {index + 1} ·{" "}
                  {formatDistance(leg.distanceMeters / 1000)}
                </h3>
                <ol className="space-y-2">
                  {leg.steps
                    .filter((step) => step.maneuver)
                    .map((step, stepIndex) => (
                      <li
                        key={stepIndex}
                        className="border-l-2 border-primary/30 pl-3"
                      >
                        {maneuverText(step.maneuver!)}
                      </li>
                    ))}
                </ol>
              </div>
            ))}
            {route.attributions.map((text, index) => (
              <p key={index} className="text-[10px] text-muted-foreground">
                {text}
              </p>
            ))}
          </div>
        )}
        <ol className="space-y-3">
          {day?.items.slice(nextStopIndex).map((item, index) => (
            <li key={item.id} className="flex gap-2 text-sm">
              <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
              <div>
                <p className="font-medium">
                  {nextStopIndex + index + 1}. {item.placeName}
                </p>
                <p className="text-xs text-muted-foreground">
                  {item.placeAddress}
                </p>
              </div>
            </li>
          ))}
        </ol>
      </section>
      <div className="min-h-[400px] flex-1 lg:h-full">
        <MapboxView
          markers={markers}
          routeGeometry={geometry}
          className="h-full min-h-[400px]"
        />
      </div>
    </div>
  );
}
