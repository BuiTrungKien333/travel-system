'use client';

import React, { useRef } from 'react';
import { ChevronLeft, ChevronRight, Sparkles } from 'lucide-react';
import { MemoryCard } from './MemoryCard';
import { useJournalUIStore } from '@/features/journal/store/useJournalUIStore';
import type { Memory } from '@/features/journal/types';

interface RecentMemoriesProps {
  memories: Memory[];
  onSelectMemory?: (memory: Memory) => void;
}

export function RecentMemories({ memories, onSelectMemory }: RecentMemoriesProps) {
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const setActiveMemoryForLightbox = useJournalUIStore(
    (s) => s.setActiveMemoryForLightbox
  );

  const handleScroll = (direction: 'left' | 'right') => {
    if (!scrollContainerRef.current) return;
    const offset = direction === 'left' ? -340 : 340;
    scrollContainerRef.current.scrollBy({ left: offset, behavior: 'smooth' });
  };

  const handleCardClick = (memory: Memory) => {
    if (onSelectMemory) {
      onSelectMemory(memory);
    } else {
      setActiveMemoryForLightbox(memory);
    }
  };

  if (!memories || memories.length === 0) {
    return null;
  }

  return (
    <section className="py-8 bg-slate-50/50 border-y border-slate-100">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="flex items-end justify-between mb-6">
          <div>
            <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-cyan-600 mb-1">
              <Sparkles className="h-3.5 w-3.5" />
              <span>Khoảnh khắc & Kỷ niệm</span>
            </div>
            <h2 className="text-2xl font-black tracking-tight text-slate-900 sm:text-3xl">
              Kỷ niệm gần đây
            </h2>
            <p className="mt-1 text-xs sm:text-sm text-slate-500">
              Những bức ảnh và ghi chú được lưu lại từ các điểm đến mới nhất của bạn.
            </p>
          </div>

          {/* Navigation Arrows for Carousel */}
          <div className="hidden sm:flex items-center gap-2">
            <button
              type="button"
              onClick={() => handleScroll('left')}
              className="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm hover:border-cyan-400 hover:text-cyan-600 active:scale-95 transition"
              aria-label="Xem kỷ niệm trước"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={() => handleScroll('right')}
              className="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm hover:border-cyan-400 hover:text-cyan-600 active:scale-95 transition"
              aria-label="Xem kỷ niệm kế tiếp"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>

        {/* Carousel Container */}
        <div
          ref={scrollContainerRef}
          className="flex gap-4 overflow-x-auto pb-4 pt-1 snap-x snap-mandatory scrollbar-thin scrollbar-thumb-slate-200 scrollbar-track-transparent -mx-4 px-4 sm:mx-0 sm:px-0"
        >
          {memories.map((mem) => (
            <div
              key={mem.id}
              className="w-[280px] sm:w-[320px] shrink-0 snap-start"
            >
              <MemoryCard memory={mem} onClick={handleCardClick} />
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
