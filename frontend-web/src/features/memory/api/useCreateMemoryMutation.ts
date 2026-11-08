import { useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { toMemory, type MemoryRecord } from './contract';
import { getMemory } from './useMemoryActions';
import { invalidateMemories } from './invalidateMemories';
import type { CreateMemoryRequest, MemoryDetail } from '../types';

export class MemorySaveError extends Error {
  constructor(message: string, public readonly memory?: MemoryDetail) { super(message); }
}

interface SaveMemory {
  creation: CreateMemoryRequest;
  note: string;
  files: File[];
  existing?: MemoryDetail;
  signal: AbortSignal;
  onProgress: (percent: number) => void;
}

export function useCreateMemoryMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (input: SaveMemory): Promise<MemoryDetail> => {
      let memory = input.existing;
      try {
        if (!memory) {
          const { data } = await apiClient.post<MemoryRecord>('/journal/memories', input.creation, { signal: input.signal });
          memory = toMemory(data);
        }
        if ((memory.note ?? '') !== input.note.trim()) {
          const { data } = await apiClient.patch<MemoryRecord>(`/journal/memories/${memory.id}`, {
            version: memory.version, note: input.note.trim() || null,
          }, { signal: input.signal });
          memory = toMemory(data);
        }
        if (input.files.length) {
          const form = new FormData();
          input.files.forEach(file => form.append('files', file));
          await apiClient.post('/media/uploads', form, {
            params: { memoryId: memory.id, tripId: memory.tripId || undefined },
            headers: { 'Content-Type': 'multipart/form-data' }, timeout: 360_000, signal: input.signal,
            onUploadProgress: event => input.onProgress(event.total ? Math.min(99, Math.round(event.loaded / event.total * 100)) : 0),
          });
        }
        const saved = await getMemory(memory.id);
        input.onProgress(100);
        return saved;
      } catch (error) {
        throw new MemorySaveError(input.signal.aborted ? 'Đã dừng chờ tải lên. Hãy tải lại để kiểm tra các ảnh đã được lưu.'
          : error instanceof Error ? error.message : 'Không thể lưu kỷ niệm.', memory);
      } finally {
        await invalidateMemories(client);
      }
    },
    retry: false,
  });
}
