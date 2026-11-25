import { useQuery } from "@tanstack/react-query";
import { getAllPages } from "@/lib/backend";
export interface Destination {
  id: string;
  code: string;
  name: string;
}
export function useDestinationsQuery() {
  return useQuery({
    queryKey: ["destinations"],
    staleTime: 300_000,
    queryFn: () => getAllPages<Destination>("/destinations"),
  });
}
