import { useJournalDataQuery } from "@/features/journal/api/useJournalDataQuery";
export function useJourneyQuery() {
  const query = useJournalDataQuery();
  const data = query.data;
  return {
    ...query,
    data: data
      ? {
          exploredProvinces: data.stats.exploredProvinces,
          totalProvinces: 63,
          totalTrips: data.stats.totalJourneys,
          totalPlaces: data.stats.totalPlaces,
          totalMemories: data.stats.totalMemories,
          totalPhotos: data.recentMemories.reduce(
            (sum, m) => sum + (m.mediaCount ?? 0),
            0,
          ),
        }
      : undefined,
  };
}
