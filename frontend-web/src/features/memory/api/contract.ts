import type { MemoryDetail } from '../types';

export interface MemoryRecord {
  id: string; userId: string; tripId: string | null; placeId?: string | null; note?: string | null;
  version: number; experiencedAt: string; createdAt: string; updatedAt: string;
  coverImageUrl?: string | null; mediaCount: number;
  media: Array<{ id: string; url: string; mimeType?: string; fileSize?: number; width?: number; height?: number; displayOrder: number; createdAt: string }>;
}

export interface MemoryPage { content: MemoryRecord[]; number: number; totalElements: number; totalPages: number; last: boolean; }
export interface UploadLimits {
  maxFileBytes: number; maxBatchBytes: number; maxFilesPerRequest: number; maxFilesPerMemory: number;
  maxPixels: number; maxDimension: number;
}
export const DEFAULT_UPLOAD_LIMITS: UploadLimits = {
  maxFileBytes: 10 * 1024 * 1024, maxBatchBytes: 50 * 1024 * 1024,
  maxFilesPerRequest: 10, maxFilesPerMemory: 20, maxPixels: 25_000_000, maxDimension: 12_000,
};
export const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

export function toMemory(record: MemoryRecord): MemoryDetail {
  return {
    ...record, tripId: record.tripId ?? '', placeId: record.placeId ?? undefined,
    note: record.note ?? undefined, coverMediaUrl: record.coverImageUrl ?? undefined,
    media: record.media.map(m => ({ ...m, memoryId: record.id, storageKey: '', sortOrder: m.displayOrder })),
  };
}

export function selectionError(files: Array<Pick<File, 'size' | 'type'>>, limits: UploadLimits, remaining = limits.maxFilesPerMemory): string | null {
  const maxFiles = Math.min(limits.maxFilesPerRequest, remaining);
  if (files.length > maxFiles) return `Bạn chỉ có thể chọn thêm ${maxFiles} ảnh trong lượt này.`;
  if (files.some(f => !IMAGE_TYPES.includes(f.type))) return 'Chỉ hỗ trợ ảnh JPEG, PNG, GIF và WebP.';
  if (files.some(f => f.size === 0)) return 'Ảnh trống không thể tải lên.';
  if (files.some(f => f.size > limits.maxFileBytes)) return `Mỗi ảnh tối đa ${Math.floor(limits.maxFileBytes / 1024 / 1024)} MB.`;
  if (files.reduce((total, f) => total + f.size, 0) > limits.maxBatchBytes)
    return `Tổng dung lượng tối đa ${Math.floor(limits.maxBatchBytes / 1024 / 1024)} MB mỗi lượt.`;
  return null;
}
