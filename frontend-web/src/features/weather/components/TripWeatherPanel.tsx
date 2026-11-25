"use client";
import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api";
export function TripWeatherPanel({ tripId }: { tripId: string }) {
  const { data } = useQuery({
    queryKey: ["trips", tripId, "weather"],
    retry: false,
    queryFn: async () => {
      const { data } = await apiClient.get<{
        location?: { name: string };
        current?: { summary: string };
        unavailableReason?: string;
      }>(`/trips/${tripId}/weather`);
      return data;
    },
  });
  if (!data?.current) return null;
  return (
    <aside className="rounded-lg border p-3 text-sm">
      {data.location?.name}: {data.current.summary}
    </aside>
  );
}
