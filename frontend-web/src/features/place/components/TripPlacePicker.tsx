"use client";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Plus, Search } from "lucide-react";
import apiClient from "@/lib/api";
import { useDestinationsQuery } from "../api/useDestinationsQuery";
import { useTripPlacesQuery } from "../api/useTripPlacesQuery";
import { useAddPlaceToDayMutation } from "../api/useAddPlaceToDayMutation";
import {
  PlaceSuggestionCard,
  type PlaceSuggestion,
} from "./PlaceSuggestionCard";
import { formatDateShort } from "@/lib/utils";
import type { Itinerary } from "@/features/itinerary/types";
export function TripPlacePicker({
  tripId,
  itinerary,
}: {
  tripId: string;
  itinerary: Itinerary;
}) {
  const destinations = useDestinationsQuery();
  const [provinceId, setProvinceId] = useState("");
  const [q, setQ] = useState("");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const availableDays = itinerary.days.filter(
    (day) => !itinerary.editableFrom || day.date >= itinerary.editableFrom,
  );
  const [selectedDate, setSelectedDate] = useState(
    availableDays[0]?.date ?? "",
  );
  const date = availableDays.some((day) => day.date === selectedDate)
    ? selectedDate
    : (availableDays[0]?.date ?? "");
  const [message, setMessage] = useState("");
  const places = useTripPlacesQuery(tripId);
  const add = useAddPlaceToDayMutation(tripId);
  const suggestions = useQuery({
    queryKey: ["places", "suggestions", tripId, provinceId, search, page],
    queryFn: async () => {
      const { data } = await apiClient.get<{
        content: PlaceSuggestion[];
        totalPages: number;
        reason?: string;
      }>("/places/suggestions", {
        params: {
          ...(provinceId ? { provinceId } : { tripId }),
          q: search,
          page,
          size: 10,
        },
      });
      return data;
    },
  });
  const scheduled = new Map(
    itinerary.days
      .filter((day) => day.date === date)
      .flatMap((day) =>
        day.items.map(
          (item) => [item.placeId, `Ngày ${day.dayIndex + 1}`] as const,
        ),
      ),
  );
  const savedPlaces = (places.data ?? []).filter(
    (place) =>
      (!provinceId || place.place.provinceId === provinceId) &&
      (!search ||
        place.place.name
          .toLocaleLowerCase("vi")
          .includes(search.toLocaleLowerCase("vi"))),
  );
  const cards = new Map<string, PlaceSuggestion>();
  savedPlaces.forEach((link) =>
    cards.set(link.placeId, {
      id: link.placeId,
      name: link.place.name,
      formattedAddress: link.place.address,
      category: link.place.category
        ? { code: link.place.category, label: link.place.category }
        : null,
      photo:
        link.place.photo ??
        (link.place.imageUrl ? { url: link.place.imageUrl } : null),
    }),
  );
  suggestions.data?.content.forEach((place) => cards.set(place.id, place));
  const error =
    suggestions.error || places.error || destinations.error || add.error;
  return (
    <details className="rounded-xl border border-border/70 bg-card p-4">
      <summary className="cursor-pointer list-none text-sm font-semibold text-primary">
        <span className="inline-flex items-center gap-2">
          <Plus className="h-4 w-4" />
          Thêm địa điểm
        </span>
      </summary>
      <div className="mt-4 space-y-4">
        <div className="grid gap-3 sm:grid-cols-2">
          <label className="space-y-1 text-xs font-medium">
            Thêm vào ngày
            <select
              aria-label="Ngày thêm địa điểm"
              value={date}
              disabled={add.isPending}
              onChange={(e) => {
                setSelectedDate(e.target.value);
                setMessage("");
              }}
              className="block w-full rounded-lg border bg-background p-2.5 text-sm"
            >
              {itinerary.days.map((day) => (
                <option
                  key={day.date}
                  value={day.date}
                  disabled={
                    !!(
                      itinerary.editableFrom &&
                      day.date < itinerary.editableFrom
                    )
                  }
                >
                  Ngày {day.dayIndex + 1} · {formatDateShort(day.date)}
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-1 text-xs font-medium">
            Tìm tại tỉnh/thành
            <select
              value={provinceId}
              onChange={(e) => {
                setProvinceId(e.target.value);
                setPage(0);
              }}
              className="block w-full rounded-lg border bg-background p-2.5 text-sm"
            >
              <option value="">Theo điểm đến chuyến đi</option>
              {destinations.data?.map((destination) => (
                <option key={destination.id} value={destination.id}>
                  {destination.name}
                </option>
              ))}
            </select>
          </label>
        </div>
        <form
          className="flex gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            setSearch(q.trim());
            setPage(0);
          }}
        >
          <input
            aria-label="Tìm địa điểm"
            placeholder="Tìm tên hoặc địa chỉ địa điểm..."
            value={q}
            onChange={(e) => setQ(e.target.value)}
            className="min-w-0 flex-1 rounded-lg border bg-background p-2.5 text-sm"
          />
          <button className="inline-flex items-center gap-1.5 rounded-lg border px-3 text-sm">
            <Search className="h-4 w-4" />
            Tìm
          </button>
        </form>
        {suggestions.data?.reason === "DESTINATION_NOT_LINKED" && (
          <p className="text-sm text-muted-foreground">
            Chọn tỉnh/thành để xem địa điểm gợi ý.
          </p>
        )}
        {error && (
          <p role="alert" className="text-sm text-destructive">
            {error.message}
            <button
              className="ml-2 underline"
              onClick={() => {
                suggestions.refetch();
                places.refetch();
                destinations.refetch();
              }}
            >
              Tải lại
            </button>
          </p>
        )}
        {message && (
          <p role="status" className="text-sm text-primary">
            {message}
          </p>
        )}
        {suggestions.isLoading && (
          <p role="status" className="text-sm">
            Đang tải địa điểm...
          </p>
        )}
        <div className="space-y-3">
          {[...cards.values()].map((place) => (
            <PlaceSuggestionCard
              key={place.id}
              place={place}
              disabled={add.isPending || !date || !places.data}
              pending={add.isPending && add.variables?.placeId === place.id}
              scheduledDay={scheduled.get(place.id)}
              onAdd={() => {
                setMessage("");
                const day = itinerary.days.find((day) => day.date === date);
                add.mutate(
                  { placeId: place.id, date },
                  {
                    onSuccess: () =>
                      setMessage(
                        `Đã thêm ${place.name} vào Ngày ${(day?.dayIndex ?? 0) + 1}.`,
                      ),
                  },
                );
              }}
            />
          ))}
        </div>
        {!suggestions.isLoading && !error && cards.size === 0 && (
          <p className="py-4 text-center text-sm text-muted-foreground">
            Không tìm thấy địa điểm phù hợp.
          </p>
        )}
        <div className="flex items-center justify-between text-xs">
          <button
            disabled={!page || suggestions.isFetching}
            onClick={() => setPage(page - 1)}
            className="rounded-lg border px-3 py-2 disabled:opacity-40"
          >
            Trang trước
          </button>
          <span>Trang {page + 1}</span>
          <button
            disabled={
              page + 1 >= (suggestions.data?.totalPages ?? 0) ||
              suggestions.isFetching
            }
            onClick={() => setPage(page + 1)}
            className="rounded-lg border px-3 py-2 disabled:opacity-40"
          >
            Trang sau
          </button>
        </div>
      </div>
    </details>
  );
}
