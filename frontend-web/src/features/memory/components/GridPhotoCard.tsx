'use client';

import Image from 'next/image';
import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';
import type { MemoryGridItem } from '../types';

interface GridPhotoCardProps {
  item: MemoryGridItem;
  onClick: () => void;
}

const ASPECT_MAP = {
  square: 'aspect-square',
  portrait: 'aspect-[3/4]',
  landscape: 'aspect-[4/3]',
} as const;

export function GridPhotoCard({ item, onClick }: GridPhotoCardProps) {
  return (
    <motion.button
      className={cn(
        'relative w-full overflow-hidden rounded-[20px] bg-slate-100 border border-slate-200/80 shadow-sm text-left',
        ASPECT_MAP[item.aspectRatio]
      )}
      onClick={onClick}
      aria-label={item.caption || 'Xem ảnh'}
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      {item.imageUrl && (
        <Image
          src={item.imageUrl}
          alt={item.caption || 'Kỷ niệm'}
          fill
          className="object-cover"
          sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
        />
      )}

      {/* Caption overlay with subtle light gray background */}
      {item.caption && (
        <div className="absolute bottom-2.5 left-2.5 right-2.5 pointer-events-none">
          <div className="inline-block max-w-full rounded-xl bg-slate-100/90 backdrop-blur-md px-3 py-1.5 border border-slate-200/80 shadow-sm">
            <p className="text-xs text-slate-800 font-medium truncate">
              {item.caption}
            </p>
          </div>
        </div>
      )}
    </motion.button>
  );
}
