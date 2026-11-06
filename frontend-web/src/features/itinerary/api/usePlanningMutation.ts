import { useMutation } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { useItineraryUIStore } from '../store/useItineraryUIStore';
import type { AIItineraryRecommendation } from '../types';

export function usePlanningMutation(tripId: string) {
  const setPendingRecommendation = useItineraryUIStore(
    (s) => s.setPendingRecommendation
  );
  const setIsGenerating = useItineraryUIStore((s) => s.setIsGenerating);

  return useMutation({
    mutationFn: async (): Promise<AIItineraryRecommendation> => {
      const { data } = await apiClient.post<AIItineraryRecommendation>(
        `/trips/${tripId}/planning`
      );
      return data;
    },
    onMutate: () => {
      setIsGenerating(true);
    },
    onSuccess: (recommendation) => {
      setPendingRecommendation(recommendation);
      setIsGenerating(false);
    },
    onError: () => {
      setIsGenerating(false);
    },
  });
}
