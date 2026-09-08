'use client';

import React from 'react';
import { Calendar, MapPin, Camera, ArrowRight, CheckCircle2 } from 'lucide-react';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import type { Journey } from '@/features/journal/types';

interface JourneyCardProps {
  journey: Journey;
  isSelected?: boolean;
  onSelect: (journey: Journey) => void;
}

export function JourneyCard({ journey, isSelected = false, onSelect }: JourneyCardProps) {
  const startDateStr = journey.startDate
    ? format(new Date(journey.startDate), 'dd/MM', { locale: vi })
    : '';
  const endDateStr = journey.endDate
    ? format(new Date(journey.endDate), 'dd/MM/yyyy', { locale: vi })
    : '';
  const dateRange = startDateStr && endDateStr ? `${startDateStr} – ${endDateStr}` : '';

  const totalMemories =
    journey.memories?.length ||
    journey.locations.reduce((acc, loc) => acc + (loc.memoryCount || 0), 0);

  return (
    <div
      onClick={() => onSelect(journey)}
      className={`group relative flex flex-col overflow-hidden rounded-2xl border bg-white shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-xl cursor-pointer ${
        isSelected
          ? 'border-cyan-500 ring-2 ring-cyan-400 shadow-cyan-500/20'
          : 'border-slate-200/80 hover:border-cyan-400'
      }`}
    >
      {/* Cover Image */}
      <div className="relative aspect-[16/10] w-full overflow-hidden bg-slate-100">
        <img
          src={journey.coverImage}
          alt={journey.title}
          className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          loading="lazy"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />

        {/* Selected / Status badge */}
        <div className="absolute top-3 left-3 right-3 flex items-center justify-between">
          {journey.status === 'COMPLETED' ? (
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/90 px-2.5 py-0.5 text-[11px] font-bold text-white backdrop-blur-md">
              <CheckCircle2 className="h-3 w-3" />
              <span>Đã hoàn thành</span>
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 rounded-full bg-cyan-500/90 px-2.5 py-0.5 text-[11px] font-bold text-white backdrop-blur-md">
              <span>Đang lên lịch</span>
            </span>
          )}

          {isSelected && (
            <span className="rounded-full bg-cyan-500 px-2 py-0.5 text-[10px] font-bold text-white shadow-md animate-pulse">
              Đang xem trên Map
            </span>
          )}
        </div>

        {/* Date on image */}
        {dateRange && (
          <div className="absolute bottom-2.5 left-3 text-xs font-semibold text-white/90 flex items-center gap-1">
            <Calendar className="h-3.5 w-3.5 text-cyan-400" />
            <span>{dateRange}</span>
          </div>
        )}
      </div>

      {/* Body Content */}
      <div className="flex flex-1 flex-col justify-between p-4 sm:p-5">
        <div>
          <h3 className="text-base font-extrabold text-slate-900 line-clamp-1 group-hover:text-cyan-600 transition-colors">
            {journey.title}
          </h3>

          {journey.description && (
            <p className="mt-1.5 text-xs text-slate-500 line-clamp-2 leading-relaxed">
              {journey.description}
            </p>
          )}

          {/* Quick Metrics */}
          <div className="mt-3.5 grid grid-cols-2 gap-2 rounded-xl bg-slate-50 p-2 text-xs">
            <div className="flex items-center gap-1.5 text-slate-700">
              <MapPin className="h-3.5 w-3.5 text-cyan-600 shrink-0" />
              <span className="font-bold">{journey.locations.length}</span>
              <span className="text-slate-500 text-[11px]">địa điểm</span>
            </div>
            <div className="flex items-center gap-1.5 text-slate-700">
              <Camera className="h-3.5 w-3.5 text-teal-600 shrink-0" />
              <span className="font-bold">{totalMemories}</span>
              <span className="text-slate-500 text-[11px]">kỷ niệm</span>
            </div>
          </div>

          {/* Province Badges */}
          {journey.provinces && journey.provinces.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-1">
              {journey.provinces.map((prov) => (
                <span
                  key={prov}
                  className="rounded-md bg-cyan-50 px-2 py-0.5 text-[10px] font-semibold text-cyan-800 border border-cyan-100"
                >
                  {prov}
                </span>
              ))}
            </div>
          )}
        </div>

        {/* Card Footer Action */}
        <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3 text-xs font-bold text-cyan-600 group-hover:text-cyan-700">
          <span>Xem trên bản đồ</span>
          <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
        </div>
      </div>
    </div>
  );
}
