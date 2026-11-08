'use client';

import React from 'react';
import { InfoWindow } from '@vis.gl/react-google-maps';
import { MapPin, Calendar, Camera, Compass, X, ArrowRight } from 'lucide-react';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import type { TravelLocation } from '@/features/journal/types';

interface MapPopupProps {
  location: TravelLocation;
  onClose: () => void;
  onViewMemories: (location: TravelLocation) => void;
}

export function MapPopup({ location, onClose, onViewMemories }: MapPopupProps) {
  const formattedDate = location.visitedAt
    ? format(new Date(location.visitedAt), 'dd MMMM yyyy', { locale: vi })
    : null;

  return (
    <InfoWindow
      position={{ lat: location.latitude, lng: location.longitude }}
      onCloseClick={onClose}
      headerDisabled
      pixelOffset={[0, -20]}
    >
      <div className="w-72 overflow-hidden rounded-2xl bg-white shadow-2xl border border-slate-100 text-slate-800 animate-in fade-in zoom-in-95 duration-200">
        {/* Header Image */}
        {location.image ? (
          <div className="relative h-32 w-full overflow-hidden bg-slate-100">
            <img
              src={location.image}
              alt={location.name}
              className="h-full w-full object-cover transition-transform duration-500 hover:scale-105"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-black/20" />
            <button
              type="button"
              onClick={onClose}
              className="absolute right-2.5 top-2.5 flex h-7 w-7 items-center justify-center rounded-full bg-black/40 text-white backdrop-blur-md hover:bg-black/70 transition"
              aria-label="Đóng popup"
            >
              <X className="h-4 w-4" />
            </button>

            {location.province && (
              <span className="absolute bottom-2.5 left-3 flex items-center gap-1 rounded-full bg-cyan-600/90 px-2.5 py-0.5 text-[11px] font-semibold text-white backdrop-blur-sm">
                <MapPin className="h-3 w-3" />
                {location.province}
              </span>
            )}
          </div>
        ) : (
          <div className="relative flex h-14 items-center justify-between bg-gradient-to-r from-cyan-600 to-teal-600 px-3 text-white">
            <span className="text-xs font-semibold">{location.province || 'Địa điểm'}</span>
            <button
              type="button"
              onClick={onClose}
              className="flex h-6 w-6 items-center justify-center rounded-full bg-white/20 hover:bg-white/30"
              aria-label="Đóng"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          </div>
        )}

        {/* Content Body */}
        <div className="p-3.5 space-y-2.5">
          <div>
            <h4 className="text-base font-extrabold text-slate-900 leading-snug">
              {location.name}
            </h4>
            {location.address && (
              <p className="text-xs text-slate-500 line-clamp-1 mt-0.5">
                {location.address}
              </p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-2 pt-1 border-t border-slate-100 text-xs">
            {formattedDate && (
              <div className="flex items-center gap-1.5 text-slate-600">
                <Calendar className="h-3.5 w-3.5 text-cyan-600 shrink-0" />
                <span className="truncate">{formattedDate}</span>
              </div>
            )}

            <div className="flex items-center gap-1.5 text-slate-600">
              <Camera className="h-3.5 w-3.5 text-teal-600 shrink-0" />
              <span className="font-semibold">{location.memoryCount} kỷ niệm</span>
            </div>

            {location.journeyTitle && (
              <div className="col-span-2 flex items-center gap-1.5 text-slate-600">
                <Compass className="h-3.5 w-3.5 text-sky-600 shrink-0" />
                <span className="truncate font-medium text-slate-700">
                  {location.journeyTitle}
                </span>
              </div>
            )}
          </div>

          {/* Action Button */}
          <button
            type="button"
            onClick={() => onViewMemories(location)}
            className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-cyan-500 to-teal-500 px-3 py-2 text-xs font-bold text-white shadow-md shadow-cyan-500/20 hover:from-cyan-600 hover:to-teal-600 active:scale-[0.98] transition"
          >
            <span>Xem kỷ niệm</span>
            <ArrowRight className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    </InfoWindow>
  );
}
