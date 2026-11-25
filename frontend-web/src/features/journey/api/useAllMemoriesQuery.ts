import { useQuery } from "@tanstack/react-query";
import { fetchMemories } from "@/lib/backend";
export function useAllMemoriesQuery() {
  return useQuery({
    queryKey: ["me", "memories"],
    queryFn: () => fetchMemories(),
    staleTime: 30_000,
  });
}
