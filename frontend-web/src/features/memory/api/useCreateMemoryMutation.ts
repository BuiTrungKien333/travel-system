import { useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import { mapMemory, type BackendMemory } from "@/lib/backend";
export interface CreateMemoryInput {
  clientRequestId: string;
  note?: string;
  placeId?: string;
  files: File[];
}
export function useCreateMemoryMutation(tripId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateMemoryInput) => {
      const { data: memory } = await apiClient.post<BackendMemory>(
        "/journal/memories",
        {
          clientRequestId: input.clientRequestId,
          ...(input.files.length
            ? { pendingMediaCount: input.files.length }
            : {}),
          tripId,
          note: input.note?.trim() || null,
          placeId: input.placeId || null,
        },
      );
      try {
        if (input.files.length) {
          const form = new FormData();
          input.files.forEach((file) => form.append("files", file));
          const { data: uploaded } = await apiClient.post<{
            succeeded: number;
            failed: number;
          }>("/media/uploads", form, {
            params: { tripId, memoryId: memory.id },
            timeout: 120_000,
          });
          if (uploaded.failed)
            throw new Error(
              `Đã lưu kỷ niệm và ${uploaded.succeeded} ảnh; ${uploaded.failed} ảnh chưa tải được. Thử lại sẽ không tạo kỷ niệm trùng.`,
            );
        }
        const { data } = await apiClient.get<BackendMemory>(
          "/journal/memories/detail",
          { params: { memoryId: memory.id } },
        );
        return mapMemory(data);
      } catch (error) {
        // Creation succeeded: retain the original request/files for an idempotent retry.
        throw new Error(
          `Kỷ niệm đã được tạo. ${error instanceof Error ? error.message : "Chưa thể hoàn tất tải ảnh."}`,
        );
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["trips"] });
      queryClient.invalidateQueries({ queryKey: ["journal"] });
      queryClient.invalidateQueries({ queryKey: ["me", "memories"] });
      queryClient.invalidateQueries({ queryKey: ["home"] });
    },
  });
}
