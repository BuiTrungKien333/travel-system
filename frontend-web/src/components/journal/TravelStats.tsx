'use client';

import React from 'react';
import { Map, MapPin, Camera, Compass } from 'lucide-react';
import type { TravelStatsData } from '@/features/journal/types';

interface TravelStatsProps {
  stats: TravelStatsData;
}

export function TravelStats({ stats }: TravelStatsProps) {
  const statItems = [
    {
      id: 'provinces',
      label: 'Tỉnh/thành khám phá',
      value: `${stats.totalProvinces} / 63`,
      subtitle: `${Math.round((stats.totalProvinces / 63) * 100)}% dải đất hình chữ S`,
      icon: Map,
      gradient: 'from-blue-500 to-cyan-500',
      bgLight: 'bg-blue-50',
      textAccent: 'text-blue-600',
      borderAccent: 'border-blue-100',
    },
    {
      id: 'places',
      label: 'Địa điểm đã ghé',
      value: stats.totalPlaces.toString(),
      subtitle: 'Danh lam & thắng cảnh',
      icon: MapPin,
      gradient: 'from-cyan-500 to-teal-500',
      bgLight: 'bg-cyan-50',
      textAccent: 'text-cyan-600',
      borderAccent: 'border-cyan-100',
    },
    {
      id: 'memories',
      label: 'Kỷ niệm & Khoảnh khắc',
      value: stats.totalMemories.toString(),
      subtitle: 'Ảnh & ghi chú cảm xúc',
      icon: Camera,
      gradient: 'from-teal-500 to-emerald-500',
      bgLight: 'bg-teal-50',
      textAccent: 'text-teal-600',
      borderAccent: 'border-teal-100',
    },
    {
      id: 'journeys',
      label: 'Hành trình hoàn thành',
      value: stats.totalJourneys.toString(),
      subtitle: 'Chuyến đi đáng nhớ',
      icon: Compass,
      gradient: 'from-indigo-500 to-sky-500',
      bgLight: 'bg-indigo-50',
      textAccent: 'text-indigo-600',
      borderAccent: 'border-indigo-100',
    },
  ];

  return (
    <section className="py-6">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 md:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
          {statItems.map((item) => {
            const Icon = item.icon;
            return (
              <div
                key={item.id}
                className="group relative overflow-hidden rounded-2xl border border-slate-200/80 bg-white p-4 sm:p-5 shadow-sm transition-all duration-300 hover:shadow-md hover:border-cyan-300"
              >
                <div className="flex items-center justify-between">
                  <div
                    className={`flex h-10 w-10 sm:h-12 sm:w-12 items-center justify-center rounded-2xl bg-gradient-to-tr ${item.gradient} text-white shadow-md shadow-cyan-500/20 group-hover:scale-105 transition-transform`}
                  >
                    <Icon className="h-5 w-5 sm:h-6 sm:w-6" />
                  </div>
                  <span
                    className={`rounded-full px-2 py-0.5 text-[10px] sm:text-xs font-bold ${item.bgLight} ${item.textAccent} border ${item.borderAccent}`}
                  >
                    {item.label}
                  </span>
                </div>

                <div className="mt-3.5">
                  <h3 className="text-2xl sm:text-3xl font-black tracking-tight text-slate-900">
                    {item.value}
                  </h3>
                  <p className="mt-0.5 text-xs text-slate-500 font-medium">
                    {item.subtitle}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
