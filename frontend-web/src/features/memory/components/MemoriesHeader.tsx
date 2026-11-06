'use client';

import { Search, Map } from 'lucide-react';
import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';
import { useMemoryUIStore } from '../store/useMemoryUIStore';

const TABS = [
  { id: 'trips' as const, label: 'Tất cả' },
  { id: 'map' as const, label: 'Bản đồ' },
] as const;

export function MemoriesHeader() {
  const memoryViewMode = useMemoryUIStore((s) => s.memoryViewMode);
  const setMemoryViewMode = useMemoryUIStore((s) => s.setMemoryViewMode);

  return (
    <header className="sticky top-0 z-30 backdrop-blur-xl bg-white/80 border-b border-slate-200/80 transition-colors">
      <div className="px-4 sm:px-6 pt-5 pb-3.5 max-w-5xl mx-auto">
        {/* Top row */}
        <div className="flex items-center justify-between mb-3.5">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">Kỷ niệm</h1>
            <p className="text-xs sm:text-sm text-slate-500 font-medium mt-0.5">Kho lưu trữ hình ảnh & khoảnh khắc đáng nhớ</p>
          </div>
          <button
            className="rounded-full bg-slate-100 hover:bg-slate-200 p-2.5 text-slate-600 transition-colors shadow-sm"
            aria-label="Tìm kiếm kỷ niệm"
          >
            <Search className="h-5 w-5" />
          </button>
        </div>

        {/* Tab toggle */}
        <div className="inline-flex items-center gap-1 rounded-full bg-slate-100 p-1 border border-slate-200/60">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setMemoryViewMode(tab.id)}
              className={cn(
                'relative rounded-full px-5 py-1.5 text-xs sm:text-sm font-semibold transition-colors',
                memoryViewMode === tab.id
                  ? 'text-slate-900'
                  : 'text-slate-500 hover:text-slate-800'
              )}
            >
              {memoryViewMode === tab.id && (
                <motion.span
                  layoutId="memory-tab-indicator"
                  className="absolute inset-0 rounded-full bg-white shadow-sm border border-slate-200/50"
                  transition={{ type: 'spring', stiffness: 400, damping: 30 }}
                />
              )}
              <span className="relative z-10 flex items-center gap-1.5">
                {tab.id === 'map' && <Map className="h-3.5 w-3.5 text-cyan-600" />}
                {tab.label}
              </span>
            </button>
          ))}
        </div>
      </div>
    </header>
  );
}
