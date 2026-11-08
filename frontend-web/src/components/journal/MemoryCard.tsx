'use client';

import React from 'react';
import { MapPin, Calendar, Compass, Camera } from 'lucide-react';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import type { Memory } from '@/features/journal/types';

interface MemoryCardProps {
  memory: Memory;
  onClick: (memory: Memory) => void;
  className?: string;
}

export function MemoryCard({ memory, onClick, className = '' }: MemoryCardProps) {
  const formattedDate = memory.createdAt
    ? format(new Date(memory.createdAt), 'dd MMMM yyyy', { locale: vi })
    : '';

  return (
    <div
      onClick={() => onClick(memory)}
      className={`group relative flex flex-col overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-xl hover:border-cyan-300 cursor-pointer ${className}`}
    >
      {/* Cover Image Container */}
      <div className="relative aspect-[4/3] w-full overflow-hidden bg-slate-100">
        <img
          src={memory.image}
          alt={memory.title}
          className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          loading="lazy"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-80 group-hover:opacity-90 transition-opacity" />

        {/* Location Badge */}
        <div className="absolute bottom-2.5 left-3 right-3 flex items-center justify-between text-white">
          <span className="inline-flex items-center gap-1 rounded-full bg-black/40 px-2.5 py-0.5 text-xs font-semibold backdrop-blur-md">
            <MapPin className="h-3 w-3 text-cyan-400" />
            <span className="truncate max-w-[150px]">{memory.location}</span>
          </span>

          {memory.mediaCount && memory.mediaCount > 1 && (
            <span className="inline-flex items-center gap-1 rounded-full bg-black/40 px-2 py-0.5 text-[10px] font-bold backdrop-blur-md">
              <Camera className="h-2.5 w-2.5" />
              <span>+{memory.mediaCount - 1}</span>
            </span>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="flex flex-1 flex-col justify-between p-4">
        <div>
          <h4 className="text-sm font-bold text-slate-900 line-clamp-2 group-hover:text-cyan-600 transition-colors leading-snug">
            {memory.title}
          </h4>

          {memory.note && (
            <p className="mt-1 text-xs text-slate-500 line-clamp-2 leading-relaxed">
              {memory.note}
            </p>
          )}
        </div>

        <div className="mt-3.5 flex items-center justify-between border-t border-slate-100 pt-2.5 text-[11px] text-slate-400">
          <span className="flex items-center gap-1 text-slate-500">
            <Calendar className="h-3 w-3 text-cyan-600" />
            <span>{formattedDate}</span>
          </span>

          {memory.journeyTitle && (
            <span className="flex items-center gap-1 text-cyan-700 font-medium truncate max-w-[130px]">
              <Compass className="h-3 w-3 text-cyan-600 shrink-0" />
              <span className="truncate">{memory.journeyTitle}</span>
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
