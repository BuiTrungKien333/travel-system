"use client";

import Image from 'next/image';
import Link from 'next/link';
import { Compass, MapPin, Camera } from 'lucide-react';
import type { TripOverview } from '@/features/trip/types';
import { TRIP_STATUS_LABELS, TRIP_STATUS_COLORS } from '@/lib/constants';
import { formatDateRange } from '@/lib/utils';

interface TripMemoryCardsProps {
  trips: TripOverview[];
  isLoading?: boolean;
}

export function TripMemoryCards({ trips, isLoading }: TripMemoryCardsProps) {
  if (isLoading) {
    return (
      <div className="space-y-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">📸 Kỷ niệm theo chuyến đi</h2>
          <p className="text-sm text-slate-500">Nhấn vào chuyến đi để xem lại những khoảnh khắc đẹp</p>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-sm animate-pulse">
              <div className="aspect-[16/10] bg-slate-200" />
              <div className="p-4 space-y-3">
                <div className="h-4 bg-slate-200 rounded w-3/4" />
                <div className="h-3 bg-slate-200 rounded w-1/2" />
                <div className="h-3 bg-slate-200 rounded w-2/3" />
                <div className="flex gap-2">
                  <div className="h-5 bg-slate-200 rounded-full w-16" />
                  <div className="h-5 bg-slate-200 rounded-full w-16" />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-bold text-slate-900">📸 Kỷ niệm theo chuyến đi</h2>
        <p className="text-sm text-slate-500">Nhấn vào chuyến đi để xem lại những khoảnh khắc đẹp</p>
      </div>

      {trips.length === 0 ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
          <Camera className="mx-auto h-12 w-12 text-slate-300 mb-4" />
          <p className="text-slate-600 mb-4">Bạn chưa có chuyến đi nào có kỷ niệm. Hãy tạo chuyến đi mới!</p>
          <Link 
            href="/trips/new" 
            className="inline-flex items-center justify-center rounded-xl bg-cyan-500 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-cyan-600 transition-colors"
          >
            Tạo chuyến đi mới
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {trips.map((trip) => (
            <Link 
              key={trip.id} 
              href={`/trips/${trip.id}/memories`}
              className="group rounded-2xl border border-cyan-100/80 bg-white overflow-hidden shadow-sm hover:shadow-md hover:border-cyan-200 transition-all flex flex-col"
            >
              {/* No zoom scale on hover */}
              <div className="relative aspect-[16/10] bg-slate-100 flex items-center justify-center overflow-hidden">
                {trip.coverImage ? (
                  <Image 
                    src={trip.coverImage} 
                    alt={trip.name || 'Trip cover'} 
                    fill 
                    className="object-cover" 
                  />
                ) : (
                  <Compass className="h-10 w-10 text-slate-300" />
                )}
                <div className={`absolute top-3 right-3 px-2.5 py-1 rounded-full text-xs font-semibold ${TRIP_STATUS_COLORS?.[trip.status] || 'bg-slate-100 text-slate-700'}`}>
                  {TRIP_STATUS_LABELS?.[trip.status] || trip.status}
                </div>
              </div>
              <div className="p-4 flex-1 flex flex-col">
                <h3 className="font-bold text-sm text-slate-900 line-clamp-1 mb-1 group-hover:text-cyan-600 transition-colors">{trip.name}</h3>
                <div className="flex items-center text-xs text-slate-500 mb-2">
                  <MapPin className="h-3 w-3 mr-1 flex-shrink-0 text-cyan-600" />
                  <span className="truncate">{trip.destination}</span>
                </div>
                <div className="text-xs text-slate-500 mb-3">
                  {formatDateRange(trip.startDate, trip.endDate)}
                </div>
                <div className="mt-auto flex items-center gap-2">
                  <span className="inline-flex items-center gap-1 rounded-full bg-cyan-50 border border-cyan-100 px-2.5 py-0.5 text-[11px] font-semibold text-cyan-700">
                    📷 {trip.memoryCount || 0} kỷ niệm
                  </span>
                  <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-[11px] font-medium text-slate-600">
                    📍 {trip.placeCount || 0} địa điểm
                  </span>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
