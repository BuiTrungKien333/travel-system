'use client';

import Image from 'next/image';
import { Camera } from 'lucide-react';
import { motion } from 'framer-motion';
import { formatDateShort } from '@/lib/utils';
import type { TripMemoryCard } from '../types';
import { useMemoryUIStore } from '../store/useMemoryUIStore';

interface TripCarouselProps {
  trips: TripMemoryCard[];
  isLoading?: boolean;
}

export function TripCarousel({ trips, isLoading }: TripCarouselProps) {
  const activeTrip = useMemoryUIStore((s) => s.activeTrip);
  const setActiveTrip = useMemoryUIStore((s) => s.setActiveTrip);

  if (isLoading) {
    return (
      <div className="px-4 sm:px-6">
        <h2 className="text-xl font-bold text-slate-900 mb-4">📸 Chuyến đi</h2>
        <div className="flex gap-4 overflow-x-auto hide-scrollbar scroll-snap-x pb-2">
          {Array.from({ length: 3 }).map((_, i) => (
            <div
              key={i}
              className="flex-shrink-0 w-[280px] sm:w-[320px] h-[360px] sm:h-[380px] rounded-[28px] bg-slate-200 animate-pulse"
            />
          ))}
        </div>
      </div>
    );
  }

  if (trips.length === 0) {
    return (
      <div className="px-4 sm:px-6">
        <h2 className="text-xl font-bold text-slate-900 mb-4">📸 Chuyến đi</h2>
        <div className="rounded-[28px] bg-slate-50 border border-slate-200 p-10 text-center shadow-sm">
          <Camera className="mx-auto h-10 w-10 text-slate-300 mb-3" />
          <p className="text-slate-500 text-sm font-medium">Chưa có chuyến đi nào có kỷ niệm</p>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4 px-4 sm:px-6 max-w-5xl mx-auto">
        <h2 className="text-xl font-bold text-slate-900">
          📸 Chuyến đi
        </h2>
        {activeTrip && (
          <button
            onClick={() => setActiveTrip(null)}
            className="text-xs font-semibold text-cyan-600 hover:text-cyan-700 transition-colors"
          >
            Hiện tất cả
          </button>
        )}
      </div>

      <div className="flex gap-4 overflow-x-auto hide-scrollbar scroll-snap-x px-4 sm:px-6 pb-2 max-w-5xl mx-auto">
        {trips.map((trip, index) => (
          <motion.button
            key={trip.id}
            className={`relative flex-shrink-0 w-[280px] sm:w-[320px] h-[360px] sm:h-[380px] rounded-[28px] overflow-hidden text-left scroll-snap-start transition-all shadow-md hover:shadow-lg ${
              activeTrip === trip.id
                ? 'ring-4 ring-cyan-500 shadow-cyan-500/20'
                : 'border border-slate-200/60'
            }`}
            onClick={() =>
              setActiveTrip(activeTrip === trip.id ? null : trip.id)
            }
            initial={{ opacity: 0, x: 40 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.4, delay: index * 0.1 }}
          >
            {/* Background image */}
            {trip.coverImage ? (
              <Image
                src={trip.coverImage}
                alt={trip.name}
                fill
                className="object-cover"
                sizes="320px"
              />
            ) : (
              <div className="absolute inset-0 bg-gradient-to-br from-slate-200 to-slate-300" />
            )}

            {/* Gradient overlay */}
            <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/30 to-transparent" />

            {/* Photo count badge */}
            <div className="absolute top-4 right-4 flex items-center gap-1.5 rounded-full bg-black/50 backdrop-blur-md px-3 py-1 border border-white/10">
              <Camera className="h-3.5 w-3.5 text-white/90" />
              <span className="text-xs font-semibold text-white">{trip.totalPhotos}</span>
            </div>

            {/* Bottom content */}
            <div className="absolute bottom-0 left-0 right-0 p-5">
              <p className="text-white/70 text-xs font-medium mb-1 drop-shadow-sm">
                {trip.destination} • {formatDateShort(trip.startDate)} - {formatDateShort(trip.endDate)}
              </p>
              <h3 className="text-white font-bold text-lg leading-snug mb-3 drop-shadow-sm">
                {trip.name}
              </h3>

              {/* Companion avatars */}
              {trip.companions.length > 0 && (
                <div className="flex items-center">
                  <div className="flex -space-x-2">
                    {trip.companions.slice(0, 3).map((companion) => (
                      <div
                        key={companion.id}
                        className="w-7 h-7 rounded-full border-2 border-white overflow-hidden bg-slate-200 shadow-sm"
                      >
                        {companion.avatarUrl ? (
                          <Image
                            src={companion.avatarUrl}
                            alt={companion.name}
                            width={28}
                            height={28}
                            className="object-cover w-full h-full"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-[10px] text-slate-700 font-bold">
                            {companion.name[0]}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                  {trip.companions.length > 3 && (
                    <span className="ml-2 text-xs font-medium text-white/80">
                      +{trip.companions.length - 3}
                    </span>
                  )}
                </div>
              )}
            </div>
          </motion.button>
        ))}
      </div>
    </div>
  );
}
