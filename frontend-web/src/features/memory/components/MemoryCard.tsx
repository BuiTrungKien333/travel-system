'use client';

import Image from 'next/image';
import { MapPin, Image as ImageIcon } from 'lucide-react';
import { formatDateShort } from '@/lib/utils';
import type { Memory } from '../types';

interface MemoryCardProps {
  memory: Memory;
}

export function MemoryCard({ memory }: MemoryCardProps) {
  return (
    <button
      className="group block w-full text-left rounded-xl overflow-hidden border bg-card shadow-sm hover:shadow-md transition-shadow"
      onClick={() => {
        // Lightbox trigger will be wired with detail fetch
      }}
      aria-label={`Kỷ niệm tại ${memory.placeName ?? 'chuyến đi'}`}
    >
      {/* Cover image */}
      <div className="relative aspect-square bg-muted overflow-hidden">
        {memory.coverMediaUrl ? (
          <Image
            src={memory.coverMediaUrl}
            alt={`Kỷ niệm tại ${memory.placeName ?? 'chuyến đi'}`}
            fill
            className="object-cover"
            sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw"
          />
        ) : (
          <div className="flex h-full items-center justify-center">
            <ImageIcon className="h-8 w-8 text-muted-foreground/30" />
          </div>
        )}
        {memory.mediaCount > 1 && (
          <span className="absolute bottom-2 right-2 rounded bg-black/60 px-1.5 py-0.5 text-[10px] text-white">
            +{memory.mediaCount - 1}
          </span>
        )}
      </div>

      {/* Info */}
      <div className="p-2.5">
        {memory.placeName && (
          <p className="flex items-center gap-1 text-xs text-muted-foreground truncate">
            <MapPin className="h-3 w-3 flex-shrink-0" />
            {memory.placeName}
          </p>
        )}
        <p className="text-[11px] text-muted-foreground/70 mt-0.5">
          {formatDateShort(memory.createdAt)}
        </p>
        {memory.note && (
          <p className="text-xs mt-1 line-clamp-2 text-foreground/80">{memory.note}</p>
        )}
      </div>
    </button>
  );
}
