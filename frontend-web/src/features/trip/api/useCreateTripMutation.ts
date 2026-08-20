import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { QUERY_KEYS } from '@/lib/constants';
import type { CreateTripRequest, Trip } from '../types';

export function useCreateTripMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: CreateTripRequest): Promise<Trip> => {
      const { data } = await apiClient.post<Trip>('/trips', input);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.trips() });
    },
  });
}
