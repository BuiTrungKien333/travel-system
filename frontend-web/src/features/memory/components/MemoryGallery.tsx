'use client';

import { LayoutGrid, Clock, Plus } from 'lucide-react';
import { cn } from '@/lib/utils';
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
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="aspect-square rounded-xl bg-muted animate-pulse" />
        ))}
      </div>
    );
  }

  if (memories.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center">
        <div className="mb-4 rounded-full bg-muted p-4">
          <LayoutGrid className="h-8 w-8 text-muted-foreground" />
        </div>
        <h3 className="font-semibold">Chưa có kỷ niệm nào</h3>
        <p className="mt-1 text-sm text-muted-foreground">Thêm ảnh và ghi chú kỷ niệm của bạn</p>
        <button
          onClick={() => setIsUploadOpen(true)}
          className="mt-4 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90"
        >
          <Plus className="h-4 w-4" />
          Thêm Memory
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">{memories.length} kỷ niệm</p>
        <div className="flex items-center gap-1 rounded-lg border p-0.5">
          <button
            onClick={() => setViewMode('grid')}
            className={cn(
              'rounded-md p-1.5 transition-colors',
              viewMode === 'grid' ? 'bg-primary text-primary-foreground' : 'hover:bg-muted'
            )}
            aria-label="Chế độ lưới"
            aria-pressed={viewMode === 'grid'}
          >
            <LayoutGrid className="h-4 w-4" />
          </button>
          <button
            onClick={() => setViewMode('timeline')}
            className={cn(
              'rounded-md p-1.5 transition-colors',
              viewMode === 'timeline' ? 'bg-primary text-primary-foreground' : 'hover:bg-muted'
            )}
            aria-label="Chế độ timeline"
            aria-pressed={viewMode === 'timeline'}
          >
            <Clock className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Grid view */}
      {viewMode === 'grid' && (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
          {memories.map((m) => (
            <MemoryCard key={m.id} memory={m} />
          ))}
        </div>
      )}

      {/* Timeline view — grouped by date */}
      {viewMode === 'timeline' && (
        <div className="space-y-6">
          {groupByDate(memories).map(({ date, items }) => (
            <div key={date}>
              <p className="text-sm font-medium text-muted-foreground mb-2">{date}</p>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
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

// Helper: group memories by date string (YYYY-MM-DD)
function groupByDate(memories: Memory[]): { date: string; items: Memory[] }[] {
  const map = new Map<string, Memory[]>();
  for (const m of memories) {
    const date = m.createdAt.slice(0, 10);
    if (!map.has(date)) map.set(date, []);
    map.get(date)!.push(m);
  }
  return Array.from(map.entries()).map(([date, items]) => ({ date, items }));
}
