"use client";
import { PlaceThumbnail, PhotoAttribution } from "./PlaceThumbnail";
import { MapPin, Plus, Loader2, Check } from "lucide-react";

export interface PlaceSuggestion {
  id: string;
  name: string;
  formattedAddress?: string;
  category?: { code: string; label: string } | null;
  photo?: {
    url: string;
    source?: string;
    attributions?: { label: string; url?: string }[];
  } | null;
}
export function PlaceSuggestionCard({
  place,
  onAdd,
  disabled,
  pending,
  scheduledDay,
}: {
  place: PlaceSuggestion;
  onAdd: () => void;
  disabled: boolean;
  pending: boolean;
  scheduledDay?: string;
}) {
  return (
    <article className="flex items-start gap-3 rounded-xl border border-border/70 bg-card p-3.5">
      <PlaceThumbnail
        name={place.name}
        photo={place.photo}
        className="h-20 w-20 sm:h-24 sm:w-24"
      />
      <div className="min-w-0 flex-1 space-y-1.5">
        <h4 className="text-sm font-semibold text-foreground">{place.name}</h4>
        {place.category && (
          <p className="text-xs font-medium text-primary">
            {place.category.label}
          </p>
        )}
        {place.formattedAddress && (
          <p className="flex items-start gap-1 text-xs text-muted-foreground">
            <MapPin className="mt-0.5 h-3 w-3 shrink-0" />
            {place.formattedAddress}
          </p>
        )}
        <PhotoAttribution photo={place.photo} />
        <button
          type="button"
          disabled={disabled || !!scheduledDay}
          onClick={onAdd}
          className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground disabled:opacity-50"
        >
          {pending ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
          ) : scheduledDay ? (
            <Check className="h-3.5 w-3.5" />
          ) : (
            <Plus className="h-3.5 w-3.5" />
          )}
          {pending
            ? "Đang thêm..."
            : scheduledDay
              ? `Đã thêm vào ${scheduledDay}`
              : "Thêm"}
        </button>
      </div>
    </article>
  );
}
