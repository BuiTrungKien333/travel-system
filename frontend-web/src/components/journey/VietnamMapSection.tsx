"use client";

import React, { useState } from "react";
import type { Province } from "@/features/journey/types";
import { MapPin, Check } from "lucide-react";
import { VIETNAM_MAP_REGIONS } from "@/constants";
import { cn } from "@/lib/utils";

interface VietnamMapSectionProps {
  exploredProvinces: Province[];
}

export function VietnamMapSection({ exploredProvinces = [] }: VietnamMapSectionProps) {
  const [selectedProvince, setSelectedProvince] = useState<string | null>(null);

  const exploredNames = exploredProvinces.map((p) => p.name);
  const totalExplored = exploredNames.length;

  return (
    <div className="rounded-xl border border-border bg-card p-6 sm:p-8 shadow-xs">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-border/60">
        <div>
          <h2 className="text-xl font-bold text-foreground">
            Bản đồ các vùng miền
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Nhấn vào tỉnh/thành để xem trạng thái đã khám phá
          </p>
        </div>

        {/* Legend */}
        <div className="flex items-center gap-4 text-xs text-muted-foreground">
          <div className="flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-primary inline-block" />
            <span className="font-medium text-foreground">Đã khám phá ({totalExplored})</span>
          </div>
          <span>·</span>
          <div className="flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-border inline-block" />
            <span>Chưa đến ({63 - totalExplored})</span>
          </div>
        </div>
      </div>

      {/* Regions Container */}
      <div className="mt-6 space-y-8">
        {VIETNAM_MAP_REGIONS.map((region) => (
          <div key={region.name} className="space-y-3">
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-semibold text-foreground">
                {region.name}
              </h3>
              <span className="text-[11px] text-muted-foreground">
                ({region.provinces.length} tỉnh thành)
              </span>
            </div>

            {/* Province Buttons Grid */}
            <div className="flex flex-wrap gap-2">
              {region.provinces.map((provName) => {
                const isExplored = exploredNames.some(
                  (name) => name.toLowerCase() === provName.toLowerCase()
                );
                const isSelected = selectedProvince === provName;

                return (
                  <button
                    key={provName}
                    type="button"
                    onClick={() => setSelectedProvince(provName)}
                    className={cn(
                      "inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium border transition-colors",
                      isExplored
                        ? isSelected
                          ? "bg-primary text-primary-foreground border-primary font-semibold shadow-xs"
                          : "bg-primary/10 text-primary border-primary/20 hover:bg-primary/20"
                        : isSelected
                        ? "bg-muted text-foreground border-foreground/30 font-semibold"
                        : "bg-background text-muted-foreground border-border hover:bg-muted/40 hover:text-foreground"
                    )}
                  >
                    <span>{provName}</span>
                    {isExplored && (
                      <Check className={cn("h-3 w-3 shrink-0", isSelected ? "text-primary-foreground" : "text-primary")} />
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {/* Selected Province Info */}
      {selectedProvince && (
        <div className="mt-8 rounded-lg bg-muted/20 border border-border p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <MapPin className="h-4 w-4" />
            </div>
            <div>
              <h4 className="text-sm font-semibold text-foreground">
                {selectedProvince}
              </h4>
              <p className="text-xs text-muted-foreground">
                {exploredNames.some((n) => n.toLowerCase() === selectedProvince.toLowerCase())
                  ? "✓ Đã có ảnh và kỷ niệm ghi lại tại địa phương này"
                  : "Chưa có kỷ niệm nào được lưu tại đây"}
              </p>
            </div>
          </div>
          
          <button
            onClick={() => setSelectedProvince(null)}
            className="text-xs font-medium text-muted-foreground hover:text-foreground self-end sm:self-auto"
          >
            Đóng
          </button>
        </div>
      )}

    </div>
  );
}
