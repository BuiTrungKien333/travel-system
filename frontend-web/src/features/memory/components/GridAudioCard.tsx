'use client';

import { useState } from 'react';
import { Play, Pause } from 'lucide-react';
import { motion } from 'framer-motion';
import type { MemoryGridItem } from '../types';

interface GridAudioCardProps {
  item: MemoryGridItem;
}

export function GridAudioCard({ item }: GridAudioCardProps) {
  const [isPlaying, setIsPlaying] = useState(false);

  // Generate static waveform bars
  const bars = Array.from({ length: 24 }, (_, i) => {
    const height = 12 + Math.sin(i * 0.8) * 10 + Math.random() * 8;
    return Math.min(32, Math.max(8, height));
  });

  return (
    <motion.div
      className="relative w-full overflow-hidden rounded-[20px] bg-slate-50 border border-slate-200/90 shadow-sm p-4 aspect-[4/3] flex flex-col justify-between"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      {/* Caption with subtle light-gray pill */}
      {item.caption && (
        <div className="inline-block rounded-lg bg-slate-200/70 px-2.5 py-1 text-xs text-slate-700 font-medium leading-relaxed line-clamp-2">
          {item.caption}
        </div>
      )}

      {/* Waveform + Play button */}
      <div className="flex items-center gap-3 mt-auto">
        <button
          onClick={() => setIsPlaying(!isPlaying)}
          className="flex-shrink-0 rounded-full bg-cyan-600 p-2.5 text-white shadow-sm hover:bg-cyan-700 transition-colors"
          aria-label={isPlaying ? 'Tạm dừng' : 'Phát'}
        >
          {isPlaying ? (
            <Pause className="h-4 w-4" />
          ) : (
            <Play className="h-4 w-4 ml-0.5" />
          )}
        </button>

        {/* Waveform visualization */}
        <div className="flex items-center gap-[2px] flex-1 h-8">
          {bars.map((h, i) => (
            <div
              key={i}
              className={`w-[3px] rounded-full transition-colors duration-300 ${
                isPlaying && i < 10 ? 'bg-cyan-600' : 'bg-slate-300'
              }`}
              style={{ height: `${h}px` }}
            />
          ))}
        </div>
      </div>
    </motion.div>
  );
}
