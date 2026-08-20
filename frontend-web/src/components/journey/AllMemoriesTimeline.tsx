"use client";

import { useState, useMemo } from 'react';
import Image from 'next/image';
import { MapPin, Image as ImageIcon, Camera } from 'lucide-react';
import type { Memory } from '@/features/memory/types';
import { formatDateShort } from '@/lib/utils';

export interface MemoryWithTrip extends Memory {
  tripName?: string;
}

interface AllMemoriesTimelineProps {
  memories: MemoryWithTrip[];
  isLoading?: boolean;
}

export function AllMemoriesTimeline({ memories, isLoading }: AllMemoriesTimelineProps) {
  const [showAll, setShowAll] = useState(false);

  const groupedMemories = useMemo(() => {
    const displayedMemories = showAll ? memories : memories.slice(0, 12);
    
    const groups: Record<string, MemoryWithTrip[]> = {};
    displayedMemories.forEach(memory => {
      const trip = memory.tripName || 'Chuyến đi khác';
      if (!groups[trip]) {
        groups[trip] = [];
      }
      groups[trip].push(memory);
    });
    return groups;
  }, [memories, showAll]);

  const hasMore = memories.length > 12;

  if (isLoading) {
    return (
      <div className="rounded-3xl border border-cyan-100 bg-white p-6 shadow-sm space-y-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">📖 Dòng thời gian kỷ niệm</h2>
          <p className="text-sm text-slate-500">Toàn bộ kỷ niệm được sắp xếp theo thời gian</p>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
          {[...Array(8)].map((_, i) => (
            <div key={i} className="animate-pulse flex flex-col gap-2">
              <div className="aspect-square bg-slate-200 rounded-xl w-full" />
              <div className="h-3 bg-slate-200 rounded w-2/3" />
              <div className="h-2 bg-slate-200 rounded w-1/2" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-3xl border border-cyan-100 bg-white/90 backdrop-blur-sm p-6 sm:p-8 shadow-sm shadow-cyan-900/5 space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-900">📖 Dòng thời gian kỷ niệm</h2>
        <p className="text-sm text-slate-500">Toàn bộ kỷ niệm được sắp xếp theo thời gian</p>
      </div>

      {memories.length === 0 ? (
        <div className="py-8 text-center">
          <Camera className="mx-auto h-12 w-12 text-slate-300 mb-4" />
          <p className="text-slate-600">Bạn chưa có kỷ niệm nào. Hãy tạo chuyến đi và lưu lại khoảnh khắc!</p>
        </div>
      ) : (
        <div className="space-y-8">
          {Object.entries(groupedMemories).map(([tripName, tripMemories]) => (
            <div key={tripName} className="space-y-4">
              <div className="inline-block rounded-full bg-cyan-50 border border-cyan-200 px-3.5 py-1 text-xs font-bold text-cyan-800">
                {tripName}
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                {tripMemories.map((memory) => (
                  <div key={memory.id} className="group rounded-2xl border border-slate-200/80 bg-white overflow-hidden hover:shadow-md transition-shadow flex flex-col">
                    {/* No hover scale on images */}
                    <div className="relative aspect-square bg-slate-100 flex items-center justify-center overflow-hidden">
                      {memory.coverMediaUrl ? (
                        <Image
                          src={memory.coverMediaUrl}
                          alt={memory.note || 'Memory'}
                          fill
                          className="object-cover"
                        />
                      ) : (
                        <ImageIcon className="h-8 w-8 text-slate-300" />
                      )}
                      {(memory.mediaCount || 0) > 1 && (
                        <div className="absolute bottom-1 right-1 bg-black/60 text-white text-[10px] px-1.5 py-0.5 rounded">
                          +{(memory.mediaCount || 1) - 1}
                        </div>
                      )}
                    </div>
                    <div className="p-2.5 flex-1 flex flex-col">
                      {memory.placeName && (
                        <div className="flex items-start text-xs text-slate-500 mb-1">
                          <MapPin className="h-3 w-3 mr-1 flex-shrink-0 mt-0.5 text-cyan-600" />
                          <span className="truncate">{memory.placeName}</span>
                        </div>
                      )}
                      <div className="text-[11px] text-slate-400 mb-1">
                        {formatDateShort(memory.createdAt)}
                      </div>
                      {memory.note && (
                        <p className="line-clamp-2 text-xs text-slate-700 mt-auto">
                          {memory.note}
                        </p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}

          {hasMore && (
            <div className="pt-4 text-center border-t border-slate-100">
              <button
                onClick={() => setShowAll(!showAll)}
                className="text-sm font-semibold text-cyan-600 hover:text-cyan-700 transition-colors"
              >
                {showAll ? 'Ẩn bớt' : 'Xem thêm kỷ niệm'}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
