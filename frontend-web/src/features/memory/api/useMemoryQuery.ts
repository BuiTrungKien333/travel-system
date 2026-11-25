import { useQuery } from "@tanstack/react-query";
import { fetchMemories } from "@/lib/backend";
import { QUERY_KEYS } from "@/lib/constants";
export function useMemoryQuery(tripId: string) {
  return useQuery({
    queryKey: QUERY_KEYS.memories(tripId),
    queryFn: () => fetchMemories(tripId),
    enabled: !!tripId,
  });
}
