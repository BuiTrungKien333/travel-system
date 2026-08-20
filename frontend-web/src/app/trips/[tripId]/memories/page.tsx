'use client';

import { use } from 'react';
import { Plus } from 'lucide-react';
import { useMemoryQuery } from '@/features/memory/api/useMemoryQuery';
import { useMemoryUIStore } from '@/features/memory/store/useMemoryUIStore';
import { MemoryGallery } from '@/features/memory/components/MemoryGallery';

export default function TripMemoriesPage({
  params,
}: {
  params: Promise<{ tripId: string }>;
}) {
  const resolvedParams = use(params);
  const { tripId } = resolvedParams;
  const { data: memories, isLoading } = useMemoryQuery(tripId);
  const setIsUploadOpen = useMemoryUIStore((s) => s.setIsUploadOpen);

  return (
    <div className="p-6 max-w-5xl space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold">Kỷ niệm chuyến đi</h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Lưu giữ khoảnh khắc và nhật ký hành trình
          </p>
        </div>
        <button
          onClick={() => setIsUploadOpen(true)}
          className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90 transition"
        >
          <Plus className="h-4 w-4" />
          Thêm kỷ niệm
        </button>
      </div>

      {/* Gallery & timeline */}
      <MemoryGallery memories={memories ?? []} isLoading={isLoading} />
    </div>
  );
}
