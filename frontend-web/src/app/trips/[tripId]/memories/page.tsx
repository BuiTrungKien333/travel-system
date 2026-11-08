'use client';

import { use } from 'react';
import { Plus } from 'lucide-react';
import { useMemoryQuery } from '@/features/memory/api/useMemoryQuery';
import { useMemoryUIStore } from '@/features/memory/store/useMemoryUIStore';
import { MemoryGallery } from '@/features/memory/components/MemoryGallery';
import { Lightbox } from '@/features/memory/components/Lightbox';
import { UploadMemoryDialog } from '@/features/memory/components/UploadMemoryDialog';

export default function TripMemoriesPage({
  params,
}: {
  params: Promise<{ tripId: string }>;
}) {
  const resolvedParams = use(params);
  const { tripId } = resolvedParams;
  const query = useMemoryQuery(tripId);
  const memories = query.data?.pages.flatMap(page => page.content) ?? [];
  const isUploadOpen = useMemoryUIStore(s => s.isUploadOpen);
  const setIsUploadOpen = useMemoryUIStore((s) => s.setIsUploadOpen);

  return (
    <div className="p-4 sm:p-8 max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-border/60">
        <div>
          <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
            Kho kỷ niệm & Ảnh chụp
          </h2>
          <p className="text-xs sm:text-sm text-muted-foreground mt-0.5">
            Lưu lại những khoảnh khắc và cảm nhận dọc hành trình
          </p>
        </div>

        <button
          onClick={() => setIsUploadOpen(true)}
          className="inline-flex items-center justify-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:bg-primary/90 transition-colors shadow-xs shrink-0"
        >
          <Plus className="h-4 w-4" />
          <span>Thêm kỷ niệm mới</span>
        </button>
      </div>

      {/* Gallery & timeline */}
      {query.isError && <div role="alert" className="rounded-lg border border-destructive/30 p-4 text-sm text-destructive">
        <p>{query.error.message}</p><button onClick={() => query.refetch()} className="mt-2 underline">Thử lại</button>
      </div>}
      {(!query.isError || memories.length > 0) && <MemoryGallery memories={memories} isLoading={query.isPending} />}
      {query.hasNextPage && <button disabled={query.isFetchingNextPage} onClick={() => query.fetchNextPage()} className="mx-auto block rounded-lg border border-border px-5 py-2 text-sm">
        {query.isFetchingNextPage ? 'Đang tải…' : 'Tải thêm kỷ niệm'}
      </button>}

      {/* Full-screen Lightbox Portal */}
      <Lightbox />
      {isUploadOpen && <UploadMemoryDialog tripId={tripId} onClose={() => setIsUploadOpen(false)} />}
    </div>
  );
}
