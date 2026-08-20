"use client";

import React, { useState } from "react";
import { Province } from "@/features/journey/types";
import { MapPin, CheckCircle2, Compass } from "lucide-react";
import { VIETNAM_MAP_REGIONS } from "@/constants";

interface VietnamMapSectionProps {
  exploredProvinces: Province[];
}

export function VietnamMapSection({ exploredProvinces = [] }: VietnamMapSectionProps) {
  const [selectedProvince, setSelectedProvince] = useState<string | null>(null);

  const exploredNames = exploredProvinces.map((p) => p.name);
  const totalExplored = exploredNames.length;

  return (
    <div className="rounded-3xl border border-cyan-100 bg-white/90 backdrop-blur-md p-6 sm:p-8 shadow-sm shadow-cyan-900/5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-100 text-cyan-700">
              <Compass className="h-5 w-5" />
            </div>
            <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900">
              Bản đồ khám phá 63 tỉnh thành Việt Nam
            </h2>
          </div>
          <p className="text-slate-500 text-xs sm:text-sm mt-1.5 ml-11">
            Hiển thị đầy đủ tên tỉnh thành. Nhấn vào từng tỉnh để xem trạng thái đã khám phá.
          </p>
        </div>

        {/* Legend badge */}
        <div className="flex items-center gap-3 text-xs bg-slate-50 border border-slate-200/80 px-3.5 py-2 rounded-2xl shrink-0 self-start sm:self-auto">
          <div className="flex items-center gap-1.5">
            <span className="h-3 w-3 rounded-full bg-cyan-500 inline-block shadow-sm" />
            <span className="font-semibold text-slate-700">Đã đi ({totalExplored})</span>
          </div>
          <span className="text-slate-300">•</span>
          <div className="flex items-center gap-1.5">
            <span className="h-3 w-3 rounded-full bg-slate-200 inline-block" />
            <span className="text-slate-500">Chưa đi ({63 - totalExplored})</span>
          </div>
        </div>
      </div>

      {/* Regions Container */}
      <div className="space-y-8">
        {VIETNAM_MAP_REGIONS.map((region) => (
          <div key={region.name} className="space-y-3">
            <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
              <h3 className="text-sm font-bold text-slate-900">
                {region.name}
              </h3>
              <span className="text-[11px] font-semibold text-cyan-700 bg-cyan-50 px-2 py-0.5 rounded-full border border-cyan-100">
                {region.badge || `${region.provinces.length} tỉnh/thành`}
              </span>
            </div>

            {/* Province Badges Grid */}
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
                    className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold transition-all ${
                      isExplored
                        ? isSelected
                          ? "bg-cyan-600 text-white shadow-md ring-2 ring-cyan-400"
                          : "bg-cyan-500 text-white shadow-sm hover:bg-cyan-600"
                        : isSelected
                        ? "bg-slate-200 text-slate-800 ring-2 ring-slate-400 font-bold"
                        : "bg-slate-100 text-slate-600 hover:bg-slate-200 hover:text-slate-900"
                    }`}
                    title={`${provName}: ${isExplored ? "Đã khám phá" : "Chưa khám phá"}`}
                  >
                    <MapPin className={`h-3 w-3 shrink-0 ${isExplored ? "text-cyan-100" : "text-slate-400"}`} />
                    <span>{provName}</span>
                    {isExplored && (
                      <CheckCircle2 className="h-3 w-3 shrink-0 text-white" />
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {/* Selected Province Info Footer */}
      {selectedProvince && (
        <div className="mt-8 rounded-2xl bg-cyan-50/70 border border-cyan-200/80 p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 animate-fade-in">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-cyan-500 text-white shadow-sm">
              <MapPin className="h-5 w-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-slate-900">
                {selectedProvince}
              </h4>
              <p className="text-xs text-slate-600 mt-0.5">
                {exploredNames.some((n) => n.toLowerCase() === selectedProvince.toLowerCase())
                  ? "🎉 Bạn đã từng đặt chân đến đây và lưu lại kỷ niệm đẹp!"
                  : "✨ Điểm đến hấp dẫn đang chờ bạn lên lịch trình khám phá!"}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setSelectedProvince(null)}
            className="text-xs font-semibold text-cyan-700 hover:text-cyan-800 hover:underline self-end sm:self-auto"
          >
            Đóng
          </button>
        </div>
      )}
    </div>
  );
}
