"use client";

import React, { useEffect, useState } from "react";
import { Luggage, MapPin, Heart, Camera } from "lucide-react";
import type { JourneyStats } from "@/features/journey/types";
import { VIETNAM_TOTAL_PROVINCES } from "@/lib/constants";

interface HeroJourneyBannerProps {
  stats: JourneyStats;
}

export function HeroJourneyBanner({ stats }: HeroJourneyBannerProps) {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const total = VIETNAM_TOTAL_PROVINCES || 63;
  const explored = stats.exploredProvinces.length;
  const percentage = Math.min(100, Math.max(0, (explored / total) * 100));

  const radius = 76;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = mounted
    ? circumference - (percentage / 100) * circumference
    : circumference;

  return (
    <div className="w-full bg-gradient-to-b from-sky-100/70 via-cyan-50/40 to-white py-12 px-4">
      <div className="max-w-4xl mx-auto flex flex-col items-center">
        <h1 className="text-4xl md:text-5xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-cyan-600 via-teal-600 to-sky-600 mb-4 text-center">
          Hành trình của bạn
        </h1>
        <p className="text-lg text-slate-600 text-center mb-10">
          Mỗi bước chân là một câu chuyện, mỗi kỷ niệm là một kho báu
        </p>

        {/* Circular Progress Ring */}
        <div className="relative flex items-center justify-center mb-12">
          <svg
            className="transform -rotate-90 w-40 h-40 drop-shadow-sm"
            viewBox="0 0 160 160"
          >
            <circle
              cx="80"
              cy="80"
              r={radius}
              stroke="currentColor"
              strokeWidth="8"
              fill="transparent"
              className="text-cyan-100"
            />
            <circle
              cx="80"
              cy="80"
              r={radius}
              stroke="currentColor"
              strokeWidth="8"
              fill="transparent"
              strokeDasharray={circumference}
              strokeDashoffset={strokeDashoffset}
              strokeLinecap="round"
              className="text-cyan-500 transition-all duration-1000 ease-out"
            />
          </svg>
          <div className="absolute flex flex-col items-center justify-center">
            <span className="text-4xl font-extrabold text-slate-900">
              {explored}
            </span>
            <span className="text-xs text-slate-500 font-semibold">
              /{total} tỉnh thành
            </span>
          </div>
        </div>

        {/* 4 Stat Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 w-full">
          <StatCard
            icon={<Luggage className="w-6 h-6 text-cyan-600" />}
            value={stats.totalTrips}
            label="Chuyến đi"
          />
          <StatCard
            icon={<MapPin className="w-6 h-6 text-cyan-600" />}
            value={stats.totalPlaces}
            label="Địa điểm"
          />
          <StatCard
            icon={<Heart className="w-6 h-6 text-rose-500" />}
            value={stats.totalMemories}
            label="Kỷ niệm"
          />
          <StatCard
            icon={<Camera className="w-6 h-6 text-cyan-600" />}
            value={stats.totalPhotos}
            label="Ảnh chụp"
          />
        </div>
      </div>
    </div>
  );
}

function StatCard({
  icon,
  value,
  label,
}: {
  icon: React.ReactNode;
  value: number;
  label: string;
}) {
  return (
    <div className="bg-white/90 backdrop-blur-sm rounded-2xl shadow-sm p-6 flex flex-col items-center justify-center border border-cyan-100 hover:border-cyan-200 transition-colors">
      <div className="w-12 h-12 bg-cyan-50 rounded-2xl flex items-center justify-center mb-3">
        {icon}
      </div>
      <span className="text-2xl font-bold text-slate-900">
        {value.toLocaleString("vi-VN")}
      </span>
      <span className="text-xs text-slate-500 font-medium mt-1">{label}</span>
    </div>
  );
}
