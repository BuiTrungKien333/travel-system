"use client";

import React from "react";
import type { JourneyStats } from "@/features/journey/types";
import { VIETNAM_TOTAL_PROVINCES } from "@/lib/constants";

interface HeroJourneyBannerProps {
  stats: JourneyStats;
}

export function HeroJourneyBanner({ stats }: HeroJourneyBannerProps) {
  const total = VIETNAM_TOTAL_PROVINCES || 63;
  const explored = stats.exploredProvinces.length;

  return (
    <div className="w-full bg-background pt-10 pb-12 px-4 sm:px-6 lg:px-8 border-b border-border/60">
      <div className="max-w-4xl mx-auto text-center space-y-6">

        <div>
          <p className="text-xs uppercase tracking-widest text-primary font-semibold">
            Bản đồ bước chân
          </p>
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-foreground tracking-tight mt-2">
            Hành trình khám phá Việt Nam
          </h1>
          <p className="text-sm sm:text-base text-muted-foreground mt-2 max-w-xl mx-auto">
            Ghi dấu từng tỉnh thành bạn đã ghé thăm qua những bức ảnh và kỷ niệm thực tế.
          </p>
        </div>

        {/* Large Typographic Explorer Milestone */}
        <div className="py-6 inline-flex flex-col items-center">
          <div className="flex items-baseline gap-2">
            <span className="text-6xl sm:text-7xl font-bold font-serif tracking-tight text-foreground">
              {explored}
            </span>
            <span className="text-2xl sm:text-3xl font-normal text-muted-foreground">
              / {total}
            </span>
          </div>
          <p className="text-xs uppercase tracking-wider font-medium text-muted-foreground mt-2">
            Tỉnh / Thành phố đã chinh phục
          </p>
        </div>

        {/* Quiet Milestone Strip with Hairline Dividers */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-6 pt-6 border-t border-border/60 max-w-2xl mx-auto text-center">
          <div className="space-y-1">
            <p className="text-2xl font-bold text-foreground font-mono">{stats.totalTrips}</p>
            <p className="text-xs text-muted-foreground">Chuyến đi</p>
          </div>
          <div className="space-y-1 sm:border-l sm:border-border/60">
            <p className="text-2xl font-bold text-foreground font-mono">{stats.totalPlaces}</p>
            <p className="text-xs text-muted-foreground">Địa điểm ghé thăm</p>
          </div>
          <div className="space-y-1 sm:border-l sm:border-border/60">
            <p className="text-2xl font-bold text-foreground font-mono">{stats.totalMemories}</p>
            <p className="text-xs text-muted-foreground">Kỷ niệm đã lưu</p>
          </div>
          <div className="space-y-1 sm:border-l sm:border-border/60">
            <p className="text-2xl font-bold text-foreground font-mono">{stats.totalPhotos}</p>
            <p className="text-xs text-muted-foreground">Ảnh chụp</p>
          </div>
        </div>

      </div>
    </div>
  );
}
