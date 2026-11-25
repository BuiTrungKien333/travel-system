"use client";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import type { TripRead } from "@/lib/backend";
export interface HomeDashboard {
  state: string;
  selectedTrip: TripRead | null;
  footprint: { provinces: number; places: number; hasHistory: boolean };
  todayStopCount: number;
  nextStop?: { name: string };
}
export function useHomeDashboardQuery() {
  return useQuery({
    queryKey: ["home", "dashboard"],
    queryFn: async () => {
      const { data } = await apiClient.get<HomeDashboard>("/home/dashboard");
      return data;
    },
  });
}
export function HomeDashboardSummary() {
  const { data, error } = useHomeDashboardQuery();
  if (error)
    return (
      <p role="alert" className="text-sm text-destructive">
        {error.message}
      </p>
    );
  if (!data) return null;
  return (
    <aside className="my-5 rounded-xl border p-4 text-sm">
      <p>
        Đã khám phá {data.footprint.provinces} tỉnh/thành ·{" "}
        {data.footprint.places} địa điểm
      </p>
      {data.selectedTrip && (
        <Link
          className="mt-2 block font-semibold text-primary"
          href={`/trips/${data.selectedTrip.trip.id}`}
        >
          Tiếp tục: {data.selectedTrip.trip.title}
        </Link>
      )}
      {data.nextStop && <p>Điểm tiếp theo: {data.nextStop.name}</p>}
    </aside>
  );
}
