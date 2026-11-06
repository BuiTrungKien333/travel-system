'use client';

import { MapPin } from 'lucide-react';
import { motion } from 'framer-motion';
import type { LocationMemoryGroup } from '../types';
import { LocationGroupHeader } from './LocationGroupHeader';
import { MasonryPhotoGrid } from './MasonryPhotoGrid';

interface LocationSectionProps {
  groups: LocationMemoryGroup[];
  isLoading?: boolean;
}

export function LocationSection({ groups, isLoading }: LocationSectionProps) {
  if (isLoading) {
    return (
      <div className="space-y-8">
        <h2 className="text-xl font-bold text-slate-900">
          📍 Kỷ niệm theo địa điểm
        </h2>
        {Array.from({ length: 2 }).map((_, i) => (
          <div key={i} className="space-y-3">
            {/* Header skeleton */}
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-slate-200 animate-pulse" />
              <div className="space-y-1.5">
                <div className="w-44 h-4 rounded bg-slate-200 animate-pulse" />
                <div className="w-28 h-3 rounded bg-slate-200 animate-pulse" />
              </div>
            </div>
            {/* Grid skeleton */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {Array.from({ length: 3 }).map((_, j) => (
                <div
                  key={j}
                  className="rounded-[20px] bg-slate-200 animate-pulse aspect-[3/4]"
                />
              ))}
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (groups.length === 0) {
    return (
      <div className="rounded-[28px] bg-slate-50 border border-slate-200/80 p-10 text-center shadow-sm">
        <MapPin className="mx-auto h-10 w-10 text-slate-300 mb-3" />
        <p className="text-slate-500 text-sm font-medium">Chưa có kỷ niệm nào theo địa điểm</p>
      </div>
    );
  }

  return (
    <div className="space-y-12">
      <h2 className="text-xl font-bold text-slate-900">
        📍 Kỷ niệm theo địa điểm
      </h2>

      {groups.map((group, index) => (
        <motion.div
          key={group.locationId}
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: index * 0.08 }}
        >
          <LocationGroupHeader
            locationName={group.locationName}
            address={group.address}
            timestamp={group.timestamp}
            latitude={group.latitude}
            longitude={group.longitude}
          />
          <MasonryPhotoGrid items={group.memories} />
        </motion.div>
      ))}
    </div>
  );
}
