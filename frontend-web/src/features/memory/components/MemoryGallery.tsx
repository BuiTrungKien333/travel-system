'use client';

import { LayoutGrid, Clock, Plus, Image as ImageIcon } from 'lucide-react';
import { cn, formatDateShort } from '@/lib/utils';
import { useMemoryUIStore } from '../store/useMemoryUIStore';
import { MemoryCard } from './MemoryCard';
import type { Memory } from '../types';

interface MemoryGalleryProps {
  memories: Memory[];
  isLoading?: boolean;
}

export function MemoryGallery({ memories, isLoading }: MemoryGalleryProps) {
  const viewMode = useMemoryUIStore((s) => s.viewMode);
  const setViewMode = useMemoryUIStore((s) => s.setViewMode);
  const setIsUploadOpen = useMemoryUIStore((s) => s.setIsUploadOpen);

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="aspect-[4/3] rounded-xl bg-muted animate-pulse" />
        ))}
      </div>
    );
  }

  if (memories.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center rounded-xl border border-dashed border-border/70 bg-card p-6">
        <div className="mb-4 h-12 w-12 rounded-full bg-muted flex items-center justify-center text-muted-foreground">
          <ImageIcon className="h-6 w-6" />
        </div>
        <h3 className="text-base font-semibold text-foreground">Chưa có kỷ niệm nào</h3>
        <p className="mt-1 text-xs text-muted-foreground max-w-sm">
          Lưu giữ những khoảnh khắc đẹp, ghi chú và hình ảnh gắn với các điểm dừng chân trong chuyến đi.
        </p>
        <button
          onClick={() => setIsUploadOpen(true)}
          className="mt-5 inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:bg-primary/90 transition-colors shadow-xs"
        >
          <Plus className="h-4 w-4" />
          <span>Thêm kỷ niệm đầu tiên</span>
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* View Switcher Toolbar */}
      <div className="flex items-center justify-between pb-3 border-b border-border/60">
        <p className="text-xs text-muted-foreground">
          <strong className="font-semibold text-foreground">{memories.length}</strong> kỷ niệm đã lưu
        </p>

        <div className="flex items-center rounded-lg border border-border bg-muted/20 p-0.5">
          <button
            onClick={() => setViewMode('grid')}
            className={cn(
              'flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-medium transition-colors',
              viewMode === 'grid'
                ? 'bg-background text-foreground shadow-xs font-semibold'
                : 'text-muted-foreground hover:text-foreground'
            )}
            aria-label="Chế độ lưới"
            aria-pressed={viewMode === 'grid'}
          >
            <LayoutGrid className="h-3.5 w-3.5" />
            <span>Lưới</span>
          </button>
          <button
            onClick={() => setViewMode('timeline')}
            className={cn(
              'flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-medium transition-colors',
              viewMode === 'timeline'
                ? 'bg-background text-foreground shadow-xs font-semibold'
                : 'text-muted-foreground hover:text-foreground'
            )}
            aria-label="Chế độ dòng thời gian"
            aria-pressed={viewMode === 'timeline'}
          >
            <Clock className="h-3.5 w-3.5" />
            <span>Dòng thời gian</span>
          </button>
        </div>
      </div>

      {/* Grid view */}
      {viewMode === 'grid' && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-5">
          {memories.map((m) => (
            <MemoryCard key={m.id} memory={m} />
          ))}
        </div>
      )}

      {/* Timeline view — grouped by date */}
      {viewMode === 'timeline' && (
        <div className="space-y-8 pl-4 border-l border-border/60">
          {groupByDate(memories).map(({ date, items }) => (
            <div key={date} className="relative space-y-3">
              <div className="flex items-center gap-2 -ml-[21px]">
                <div className="h-2.5 w-2.5 rounded-full bg-primary ring-4 ring-background" />
                <span className="text-xs font-semibold text-foreground">{date}</span>
                <span className="text-[11px] text-muted-foreground">({items.length} khoảnh khắc)</span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 pt-1">
                {items.map((m) => (
                  <MemoryCard key={m.id} memory={m} />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function groupByDate(memories: Memory[]): Array<{ date: string; items: Memory[] }> {
  const map: Record<string, Memory[]> = {};
  for (const m of memories) {
    const key = formatDateShort(m.createdAt);
    if (!map[key]) map[key] = [];
    map[key].push(m);
  }
  return Object.entries(map).map(([date, items]) => ({ date, items }));
}
