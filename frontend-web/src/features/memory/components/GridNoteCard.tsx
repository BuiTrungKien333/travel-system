'use client';

import { motion } from 'framer-motion';
import type { MemoryGridItem } from '../types';

interface GridNoteCardProps {
  item: MemoryGridItem;
}

export function GridNoteCard({ item }: GridNoteCardProps) {
  return (
    <motion.div
      className="relative w-full overflow-hidden rounded-[20px] bg-gradient-to-br from-amber-50 to-orange-50/80 border border-amber-200/70 p-5 aspect-square flex flex-col justify-between shadow-sm"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <span className="text-xl">📝</span>
      <p className="text-sm text-amber-950 font-medium leading-relaxed italic my-auto text-center">
        {item.noteText}
      </p>
      <div className="h-2" />
    </motion.div>
  );
}
