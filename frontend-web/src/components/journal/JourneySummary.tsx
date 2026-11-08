'use client';

import React from 'react';
import { Calendar, MapPin, Camera, X, ArrowRight, Compass } from 'lucide-react';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import type { Journey } from '@/features/journal/types';

interface JourneySummaryProps {
  journey: Journey;
  onClear: () => void;
  onViewDetails?: (journey: Journey) => void;
  className?: string;
}

export function JourneySummary({
  journey,
  onClear,
  onViewDetails,
  className = '',
}: JourneySummaryProps) {
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
      className={`rounded-2xl border border-slate-200/80 bg-white/95 p-4 shadow-xl backdrop-blur-md transition-all duration-300 ${className}`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-cyan-500 text-white shadow-md shadow-cyan-500/20">
            <Compass className="h-4 w-4" />
          </div>
          <div>
            <span className="text-[10px] font-bold uppercase tracking-wider text-cyan-700">
              Hành trình đang chọn
            </span>
            <h4 className="text-sm font-extrabold text-slate-900 line-clamp-1">
              {journey.title}
            </h4>
          </div>
        </div>

        <button
          type="button"
          onClick={onClear}
          className="flex h-7 w-7 items-center justify-center rounded-full text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
          aria-label="Bỏ chọn hành trình"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {dateRange && (
        <div className="mt-2.5 flex items-center gap-1.5 text-xs text-slate-600">
          <Calendar className="h-3.5 w-3.5 text-cyan-600 shrink-0" />
          <span>{dateRange}</span>
        </div>
      )}

      {/* Stats row */}
      <div className="mt-3 grid grid-cols-2 gap-2 rounded-xl bg-slate-50/80 p-2.5 text-xs">
        <div className="flex items-center gap-2">
          <div className="flex h-6 w-6 items-center justify-center rounded-lg bg-cyan-100 text-cyan-700">
            <MapPin className="h-3.5 w-3.5" />
          </div>
          <div>
            <p className="font-extrabold text-slate-900">{journey.locations.length}</p>
            <p className="text-[10px] text-slate-500">Địa điểm</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex h-6 w-6 items-center justify-center rounded-lg bg-teal-100 text-teal-700">
            <Camera className="h-3.5 w-3.5" />
          </div>
          <div>
            <p className="font-extrabold text-slate-900">{totalMemories}</p>
            <p className="text-[10px] text-slate-500">Kỷ niệm</p>
          </div>
        </div>
      </div>

      {/* Province Badges */}
      {journey.provinces && journey.provinces.length > 0 && (
        <div className="mt-2.5 flex flex-wrap gap-1">
          {journey.provinces.map((prov) => (
            <span
              key={prov}
              className="rounded-lg bg-cyan-50 px-2 py-0.5 text-[10px] font-semibold text-cyan-800 border border-cyan-100"
            >
              {prov}
            </span>
          ))}
        </div>
      )}

      {/* CTA Button */}
      {onViewDetails && (
        <button
          type="button"
          onClick={() => onViewDetails(journey)}
          className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-xl bg-slate-900 py-2 text-xs font-bold text-white shadow-sm hover:bg-cyan-600 transition"
        >
          <span>Xem các địa điểm & kỷ niệm</span>
          <ArrowRight className="h-3.5 w-3.5" />
        </button>
      )}
    </div>
  );
}
