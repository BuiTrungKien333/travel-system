'use client';

import React from 'react';
import Link from 'next/link';
import { Plus, BookOpen } from 'lucide-react';
import { ROUTES } from '@/constants/routes';

interface JournalHeroProps {
  totalJourneys: number;
  totalPlaces: number;
}

export function JournalHero({ totalJourneys, totalPlaces }: JournalHeroProps) {
  return (
    <section className="relative overflow-hidden bg-gradient-to-b from-cyan-50/60 via-white to-white pt-8 pb-6 border-b border-slate-100">
      {/* Subtle Background Elements */}
      <div className="absolute top-0 right-1/4 h-64 w-64 rounded-full bg-cyan-200/20 blur-3xl -z-10 pointer-events-none" />
      <div className="absolute top-12 left-1/3 h-48 w-48 rounded-full bg-teal-200/20 blur-3xl -z-10 pointer-events-none" />

      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-6">
          {/* Left: Branding & Titles */}
          <div className="max-w-2xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-cyan-200/80 bg-cyan-50/80 px-3 py-1 text-xs font-bold text-cyan-800 shadow-sm backdrop-blur-sm mb-3">
              <BookOpen className="h-3.5 w-3.5 text-cyan-600" />
              <span>Nhật ký khám phá cá nhân</span>
              <span className="h-1 w-1 rounded-full bg-cyan-400" />
              <span className="text-cyan-600 font-semibold">{totalJourneys} chuyến đi</span>
              <span className="h-1 w-1 rounded-full bg-cyan-400" />
              <span className="text-cyan-600 font-semibold">{totalPlaces} địa điểm</span>
            </div>

            <h1 className="text-3xl font-black tracking-tight text-slate-900 sm:text-4xl lg:text-5xl">
              Nhật ký hành trình{' '}
              <span className="bg-gradient-to-r from-cyan-600 to-teal-600 bg-clip-text text-transparent">
                (My Journal)
              </span>
            </h1>

            <p className="mt-2 text-sm text-slate-600 sm:text-base leading-relaxed">
              Mỗi vùng đất là một câu chuyện sống động. Khám phá lại những địa danh bạn từng đặt chân đến, tuyến đường đã đi và những khoảnh khắc kỷ niệm đã lưu dấu.
            </p>
          </div>

          {/* Right: Quick CTA */}
          <div className="flex items-center gap-3 shrink-0">
            <Link
              href={ROUTES.tripNew}
              className="inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-cyan-500 via-teal-500 to-cyan-600 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-cyan-500/25 hover:from-cyan-600 hover:to-teal-600 active:scale-[0.99] transition-all"
            >
              <Plus className="h-4 w-4 stroke-[2.5]" />
              <span>+ Tạo chuyến đi mới</span>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
