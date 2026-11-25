import { useMutation } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import { useItineraryUIStore } from "../store/useItineraryUIStore";
import type { AIItineraryRecommendation } from "../types";
export interface OptimizationProposal {
  status: string;
  totalTripDistanceKm: number;
  totalTripTransitMinutes: number;
  dailySchedules: {
    dayDate: string;
    scheduleItems: {
      placeId: string;
      placeName: string;
      arrivalTime?: string;
      durationMinutes: number;
    }[];
  }[];
  globalIssues?: { reason: string; suggestionHint?: string }[];
  copilotRecommendations?: {
    greetingAndCaringReminder?: string;
    recommendationCards?: { title: string; explanation: string }[];
    aiProvider: string;
  };
}
export function usePlanningMutation(tripId: string) {
  const setPendingRecommendation = useItineraryUIStore(
    (s) => s.setPendingRecommendation,
  );
  const setIsGenerating = useItineraryUIStore((s) => s.setIsGenerating);
  return useMutation({
    mutationFn: async (): Promise<AIItineraryRecommendation> => {
      const { data } = await apiClient.post<{
        sourceUpdatedAt: string;
        proposal: OptimizationProposal;
      }>(`/trips/${tripId}/itinerary/optimize`, undefined, {
        timeout: 120_000,
      });
      return {
        type:
          data.proposal.copilotRecommendations &&
          data.proposal.copilotRecommendations.aiProvider !== "mock"
            ? "AI"
            : "FALLBACK",
        optimization: data,
        summary: `${data.proposal.totalTripDistanceKm} km · ${data.proposal.totalTripTransitMinutes} phút di chuyển`,
        notes: [
          ...(data.proposal.globalIssues ?? []).map((i) =>
            [i.reason, i.suggestionHint].filter(Boolean).join(" — "),
          ),
          ...(data.proposal.copilotRecommendations?.greetingAndCaringReminder
            ? [data.proposal.copilotRecommendations.greetingAndCaringReminder]
            : []),
          ...(
            data.proposal.copilotRecommendations?.recommendationCards ?? []
          ).map((card) => `${card.title}: ${card.explanation}`),
        ],
        days: data.proposal.dailySchedules.map((day) => ({
          date: day.dayDate,
          items: day.scheduleItems.map((item, order) => ({
            placeId: item.placeId,
            order,
            suggestedStartTime: item.arrivalTime?.slice(11, 16) ?? "",
            suggestedDurationMinutes: item.durationMinutes,
            reason: item.placeName,
          })),
        })),
      };
    },
    onMutate: () => {
      setPendingRecommendation(null);
      setIsGenerating(true);
    },
    onSuccess: setPendingRecommendation,
    onSettled: () => setIsGenerating(false),
  });
}
