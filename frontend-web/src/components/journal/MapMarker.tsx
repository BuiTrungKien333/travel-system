'use client';

import React from 'react';
import { AdvancedMarker } from '@vis.gl/react-google-maps';
import { MapPin, Camera } from 'lucide-react';
import type { TravelLocation } from '@/features/journal/types';

interface MapMarkerProps {
  location: TravelLocation;
  isSelected?: boolean;
  isHovered?: boolean;
  isDimmed?: boolean;
  onClick: () => void;
}

export const MapMarker = React.memo(function MapMarker({
  location,
  isSelected = false,
  isHovered = false,
  isDimmed = false,
  onClick,
}: MapMarkerProps) {
  const isHighlighted = isSelected || isHovered;

  return (
    <AdvancedMarker
      position={{ lat: location.latitude, lng: location.longitude }}
      title={location.name}
      onClick={onClick}
      zIndex={isHighlighted ? 100 : 10}
    >
      <div
        className={`group relative flex items-center justify-center transition-all duration-300 cursor-pointer ${
          isHighlighted ? 'scale-125 z-50' : 'scale-100'
        } ${isDimmed ? 'opacity-40' : 'opacity-100'}`}
      >
        {/* Outer Glow Pulse when selected */}
        {isSelected && (
          <span className="absolute -inset-2 rounded-full bg-cyan-500/30 animate-ping" />
        )}

        {/* Pin Body */}
        <div
          className={`flex items-center gap-1.5 rounded-full px-2.5 py-1.5 shadow-lg border backdrop-blur-md transition-all duration-200 ${
            isSelected
              ? 'bg-gradient-to-r from-cyan-600 to-teal-600 border-white text-white shadow-cyan-500/40 ring-2 ring-cyan-400'
              : isHovered
              ? 'bg-slate-900 border-cyan-400 text-white shadow-xl'
              : 'bg-white/95 border-slate-200 text-slate-800 hover:border-cyan-400 hover:bg-white'
          }`}
        >
          {location.order !== undefined ? (
            <span
              className={`flex h-5 w-5 items-center justify-center rounded-full text-[11px] font-extrabold ${
                isSelected
                  ? 'bg-white text-cyan-700'
                  : 'bg-cyan-500 text-white'
              }`}
            >
              {location.order}
            </span>
          ) : (
            <MapPin
              className={`h-3.5 w-3.5 ${
                isSelected ? 'text-white' : 'text-cyan-600'
              }`}
            />
          )}

          <span className="text-xs font-bold tracking-tight max-w-[120px] truncate">
            {location.name}
          </span>

          {location.memoryCount > 0 && (
            <span
              className={`flex items-center gap-0.5 text-[10px] font-semibold px-1 rounded-full ${
                isSelected
                  ? 'bg-white/20 text-white'
                  : 'bg-cyan-50 text-cyan-700 border border-cyan-100'
              }`}
            >
              <Camera className="h-2.5 w-2.5" />
              {location.memoryCount}
            </span>
          )}
        </div>

        {/* Pointer Arrow */}
        <div
          className={`absolute -bottom-1 h-2 w-2 rotate-45 border-b border-r transition-colors ${
            isSelected
              ? 'bg-teal-600 border-white'
              : isHovered
              ? 'bg-slate-900 border-cyan-400'
              : 'bg-white border-slate-200'
          }`}
        />
      </div>
    </AdvancedMarker>
  );
});
