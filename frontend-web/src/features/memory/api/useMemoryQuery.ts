import { useInfiniteQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { toMemory, type MemoryPage } from './contract';
import { useAuthStore } from '@/features/auth/store/useAuthStore';

export function useMemoryQuery(tripId?: string) {
  const userId = useAuthStore(s => s.user?.id);
  const authenticated = useAuthStore(s => s.isAuthenticated);
  return useInfiniteQuery({
    queryKey: ['memories', userId, tripId ?? 'all'],
    enabled: authenticated,
    initialPageParam: 0,
    queryFn: async ({ pageParam, signal }) => {
      const { data } = await apiClient.get<MemoryPage>('/journal/memories', {
        params: { tripId: tripId || undefined, page: pageParam, size: 20, sort: 'experiencedAt,desc' }, signal,
      });
      return { ...data, content: data.content.map(toMemory) };
    },
    getNextPageParam: last => last.last ? undefined : last.number + 1,
    staleTime: 30_000,
  });
}
