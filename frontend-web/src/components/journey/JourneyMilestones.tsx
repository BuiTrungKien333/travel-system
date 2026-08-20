"use client";

import React from "react";
import { CheckCircle2, Target, Lock, Award } from "lucide-react";
import { JOURNEY_MILESTONES_DATA } from "@/constants";

interface JourneyMilestonesProps {
  exploredCount: number;
}

export function JourneyMilestones({ exploredCount = 0 }: JourneyMilestonesProps) {
  const currentBadge = [...JOURNEY_MILESTONES_DATA].reverse().find(m => exploredCount >= m.target);
  const currentBadgeLabel = currentBadge ? currentBadge.label : "Người mới bắt đầu";

  return (
    <div className="rounded-3xl border border-cyan-100 bg-white/90 backdrop-blur-sm p-6 sm:p-8 shadow-sm shadow-cyan-900/5">
      <div className="flex items-center gap-2 mb-6">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-100 text-cyan-700">
          <Award className="h-5 w-5" />
        </div>
        <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900">
          Cột mốc hành trình
        </h2>
      </div>
      
      <div className="flex overflow-x-auto pb-4 gap-4 hide-scrollbar snap-x">
        {JOURNEY_MILESTONES_DATA.map((milestone, index) => {
          const isAchieved = exploredCount >= milestone.target;
          const isNext = !isAchieved && (index === 0 || exploredCount >= JOURNEY_MILESTONES_DATA[index - 1].target);
          
          if (isAchieved) {
            return (
              <div key={milestone.target} className="snap-start min-w-[150px] rounded-2xl p-4 flex flex-col items-center justify-center border border-cyan-200 bg-gradient-to-b from-cyan-50/80 to-sky-50/50 shrink-0 shadow-sm">
                <CheckCircle2 className="w-8 h-8 text-cyan-600 mb-2" />
                <span className="text-xl font-extrabold text-slate-900">{milestone.target}</span>
                <span className="text-xs font-bold text-cyan-800 text-center mt-1">{milestone.label}</span>
                <span className="text-[10px] text-cyan-600 font-semibold mt-1">Đã đạt ✓</span>
              </div>
            );
          }
          
          if (isNext) {
            const remaining = milestone.target - exploredCount;
            return (
              <div key={milestone.target} className="snap-start min-w-[150px] rounded-2xl p-4 flex flex-col items-center justify-center border-2 border-dashed border-cyan-400 bg-white shrink-0 shadow-sm">
                <Target className="w-8 h-8 text-cyan-500 mb-2 animate-bounce" />
                <span className="text-xl font-extrabold text-slate-700">{milestone.target}</span>
                <span className="text-xs font-semibold text-cyan-700 text-center mt-1">Còn {remaining} tỉnh nữa!</span>
                <span className="text-[11px] text-slate-500 text-center mt-0.5">{milestone.label}</span>
              </div>
            );
          }
          
          return (
            <div key={milestone.target} className="snap-start min-w-[150px] rounded-2xl p-4 flex flex-col items-center justify-center border border-slate-200 bg-slate-50/80 shrink-0 opacity-70">
              <Lock className="w-8 h-8 text-slate-400 mb-2" />
              <span className="text-xl font-extrabold text-slate-500">{milestone.target}</span>
              <span className="text-xs font-semibold text-slate-500 text-center mt-1">{milestone.label}</span>
              <span className="text-[10px] text-slate-400 mt-1">Chưa mở khóa</span>
            </div>
          );
        })}
      </div>

      <div className="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4 p-4 rounded-2xl bg-cyan-50/70 border border-cyan-200/80">
        <div className="flex items-center gap-3">
          <div className="text-2xl">🏅</div>
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-cyan-800">Danh hiệu hiện tại</span>
            <p className="text-sm font-extrabold text-slate-900">{currentBadgeLabel}</p>
          </div>
        </div>
        <p className="text-xs text-slate-600">
          Bạn đã đặt chân tới <span className="font-bold text-cyan-700">{exploredCount}/63</span> tỉnh thành khắp Việt Nam!
        </p>
      </div>
    </div>
  );
}
