'use client';

import React from 'react';
import Link from 'next/link';
import {
  Search,
  ArrowUpDown,
  X,
  MapPin,
  Compass,
  Plus,
} from 'lucide-react';
import { ROUTES } from '@/constants/routes';
import { useJournalUIStore } from '@/features/journal/store/useJournalUIStore';
import type { Journey, JournalSortOption } from '@/features/journal/types';

interface JournalToolbarProps {
  journeys: Journey[];
  allProvinces: string[];
  totalResultsCount: number;
}

export function JournalToolbar({
  journeys,
  allProvinces,
  totalResultsCount,
}: JournalToolbarProps) {
  const searchQuery = useJournalUIStore((s) => s.searchQuery);
  const setSearchQuery = useJournalUIStore((s) => s.setSearchQuery);
  const selectedProvince = useJournalUIStore((s) => s.selectedProvince);
  const setSelectedProvince = useJournalUIStore((s) => s.setSelectedProvince);
  const selectedJourneyId = useJournalUIStore((s) => s.selectedJourneyId);
  const setSelectedJourneyId = useJournalUIStore((s) => s.setSelectedJourneyId);
  const sortBy = useJournalUIStore((s) => s.sortBy);
  const setSortBy = useJournalUIStore((s) => s.setSortBy);
  const resetFilters = useJournalUIStore((s) => s.resetFilters);

  const hasActiveFilters = Boolean(
    searchQuery || selectedProvince || selectedJourneyId || sortBy !== 'newest'
  );

  return (
    <div className="sticky top-16 z-30 bg-white/95 backdrop-blur-md border-b border-slate-200 shadow-sm py-3 transition-all duration-200">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3">
          {/* Left: Search Bar */}
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm kiếm hành trình, địa điểm, kỷ niệm, tỉnh thành..."
              className="w-full rounded-xl border border-slate-200 bg-slate-50/80 pl-10 pr-9 py-2 text-xs sm:text-sm font-medium text-slate-900 placeholder:text-slate-400 focus:border-cyan-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-cyan-500/20 transition"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            )}
          </div>

          {/* Center & Right: Filter Chips & Selectors */}
          <div className="flex flex-wrap items-center gap-2 sm:gap-2.5">
            {/* Province Filter */}
            <div className="relative inline-flex items-center">
              <MapPin className="absolute left-2.5 h-3.5 w-3.5 text-slate-400 pointer-events-none" />
              <select
                value={selectedProvince || ''}
                onChange={(e) => setSelectedProvince(e.target.value || null)}
                className="rounded-xl border border-slate-200 bg-white pl-8 pr-7 py-2 text-xs font-semibold text-slate-700 hover:border-cyan-400 focus:border-cyan-500 focus:outline-none transition cursor-pointer appearance-none shadow-sm"
              >
                <option value="">Tất cả tỉnh thành</option>
                {allProvinces.map((prov) => (
                  <option key={prov} value={prov}>
                    {prov}
                  </option>
                ))}
              </select>
            </div>

            {/* Journey Filter */}
            <div className="relative inline-flex items-center">
              <Compass className="absolute left-2.5 h-3.5 w-3.5 text-slate-400 pointer-events-none" />
              <select
                value={selectedJourneyId || ''}
                onChange={(e) => setSelectedJourneyId(e.target.value || null)}
                className="rounded-xl border border-slate-200 bg-white pl-8 pr-7 py-2 text-xs font-semibold text-slate-700 hover:border-cyan-400 focus:border-cyan-500 focus:outline-none transition cursor-pointer appearance-none shadow-sm max-w-[180px] truncate"
              >
                <option value="">Tất cả chuyến đi</option>
                {journeys.map((j) => (
                  <option key={j.id} value={j.id}>
                    {j.title}
                  </option>
                ))}
              </select>
            </div>

            {/* Sort Selector */}
            <div className="relative inline-flex items-center">
              <ArrowUpDown className="absolute left-2.5 h-3.5 w-3.5 text-slate-400 pointer-events-none" />
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as JournalSortOption)}
                className="rounded-xl border border-slate-200 bg-white pl-8 pr-7 py-2 text-xs font-semibold text-slate-700 hover:border-cyan-400 focus:border-cyan-500 focus:outline-none transition cursor-pointer appearance-none shadow-sm"
              >
                <option value="newest">Mới nhất</option>
                <option value="oldest">Cũ nhất</option>
                <option value="most_visited">Nhiều điểm đến nhất</option>
                <option value="most_memories">Nhiều kỷ niệm nhất</option>
              </select>
            </div>

            {/* Reset Filters */}
            {hasActiveFilters && (
              <button
                type="button"
                onClick={resetFilters}
                className="inline-flex items-center gap-1 rounded-xl bg-slate-100 px-3 py-2 text-xs font-bold text-slate-600 hover:bg-red-50 hover:text-red-600 transition"
              >
                <X className="h-3.5 w-3.5" />
                <span>Đặt lại ({totalResultsCount})</span>
              </button>
            )}

            {/* CTA Button on Desktop */}
            <Link
              href={ROUTES.tripNew}
              className="hidden xl:inline-flex items-center gap-1.5 rounded-xl bg-cyan-500 px-3.5 py-2 text-xs font-bold text-white shadow-sm hover:bg-cyan-600 transition ml-auto"
            >
              <Plus className="h-3.5 w-3.5 stroke-[2.5]" />
              <span>Tạo chuyến đi</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
