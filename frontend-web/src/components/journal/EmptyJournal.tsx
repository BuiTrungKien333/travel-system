'use client';

import React from 'react';
import Link from 'next/link';
import { Compass, SearchX, Camera, Plus, RotateCcw } from 'lucide-react';
import { ROUTES } from '@/constants/routes';

interface EmptyJournalProps {
  type?: 'empty' | 'no_results' | 'no_memories';
  onResetFilters?: () => void;
}

export function EmptyJournal({
  type = 'empty',
  onResetFilters,
}: EmptyJournalProps) {
  if (type === 'no_results') {
    return (
      <div className="flex flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white px-6 py-16 text-center shadow-sm">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-50 text-amber-500 ring-8 ring-amber-50/50 mb-4">
          <SearchX className="h-8 w-8" />
        </div>
        <h3 className="text-lg font-bold text-slate-900 sm:text-xl">
          Không tìm thấy kết quả phù hợp
        </h3>
        <p className="mt-1.5 max-w-sm text-xs sm:text-sm text-slate-500 leading-relaxed">
          Hãy thử tìm kiếm với từ khóa khác hoặc điều chỉnh lại bộ lọc tỉnh thành, hành trình.
        </p>

        {onResetFilters && (
          <button
            type="button"
            onClick={onResetFilters}
            className="mt-5 inline-flex items-center gap-2 rounded-xl bg-slate-900 px-4 py-2.5 text-xs font-bold text-white shadow-sm hover:bg-cyan-600 active:scale-95 transition"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>Đặt lại toàn bộ bộ lọc</span>
          </button>
        )}
      </div>
    );
  }

  if (type === 'no_memories') {
    return (
      <div className="flex flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white px-6 py-14 text-center">
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-teal-50 text-teal-600 mb-3">
          <Camera className="h-7 w-7" />
        </div>
        <h4 className="text-base font-bold text-slate-900">
          Chưa có khoảnh khắc nào
        </h4>
        <p className="mt-1 text-xs text-slate-500 max-w-xs">
          Bắt đầu lưu giữ những bức ảnh và kỷ niệm đáng nhớ từ các chuyến đi của bạn.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white px-6 py-20 text-center shadow-sm">
      <div className="flex h-20 w-20 items-center justify-center rounded-3xl bg-cyan-50 text-cyan-600 ring-8 ring-cyan-50/50 mb-5">
        <Compass className="h-10 w-10 animate-spin-slow" />
      </div>

      <h3 className="text-xl font-black text-slate-900 sm:text-2xl">
        Hành trình của bạn bắt đầu từ đây
      </h3>
      <p className="mt-2 max-w-md text-xs sm:text-sm text-slate-500 leading-relaxed">
        Tạo chuyến đi đầu tiên và bắt đầu ghim những vùng đất bạn đã đi qua trên bản đồ dải đất hình chữ S.
      </p>

      <Link
        href={ROUTES.tripNew}
        className="mt-6 inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-cyan-500 to-teal-500 px-6 py-3 text-sm font-bold text-white shadow-lg shadow-cyan-500/25 hover:from-cyan-600 hover:to-teal-600 active:scale-95 transition"
      >
        <Plus className="h-4 w-4 stroke-[2.5]" />
        <span>Tạo chuyến đi mới</span>
      </Link>
    </div>
  );
}
