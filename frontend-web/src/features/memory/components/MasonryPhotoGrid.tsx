'use client';

import { useMemo } from 'react';
import type { MemoryGridItem } from '../types';
import { GridPhotoCard } from './GridPhotoCard';
import { GridAudioCard } from './GridAudioCard';
import { GridNoteCard } from './GridNoteCard';
import { useMemoryUIStore } from '../store/useMemoryUIStore';

interface MasonryPhotoGridProps {
  items: MemoryGridItem[];
}

export function MasonryPhotoGrid({ items }: MasonryPhotoGridProps) {
  const openPhotoViewer = useMemoryUIStore((s) => s.openPhotoViewer);

  // Distribute items into 3 columns for desktop masonry effect
  const [col1, col2, col3] = useMemo(() => {
    const c1: MemoryGridItem[] = [];
    const c2: MemoryGridItem[] = [];
    const c3: MemoryGridItem[] = [];
    let h1 = 0;
    let h2 = 0;
    let h3 = 0;

    for (const item of items) {
      const weight =
        item.aspectRatio === 'portrait' ? 1.33 :
        item.aspectRatio === 'landscape' ? 0.75 : 1;

      if (h1 <= h2 && h1 <= h3) {
        c1.push(item);
        h1 += weight;
      } else if (h2 <= h1 && h2 <= h3) {
        c2.push(item);
        h2 += weight;
      } else {
        c3.push(item);
        h3 += weight;
      }
    }
    return [c1, c2, c3];
  }, [items]);

  const handlePhotoClick = (item: MemoryGridItem) => {
    const photoItems = items.filter((i) => i.type === 'photo');
    const index = photoItems.findIndex((i) => i.id === item.id);
    if (index !== -1) {
      openPhotoViewer(item, items, index);
    }
  };

  const renderItem = (item: MemoryGridItem) => {
    switch (item.type) {
      case 'photo':
        return (
          <GridPhotoCard
            key={item.id}
            item={item}
            onClick={() => handlePhotoClick(item)}
          />
        );
      case 'audio':
        return <GridAudioCard key={item.id} item={item} />;
      case 'note':
        return <GridNoteCard key={item.id} item={item} />;
      default:
        return null;
    }
  };

  return (
    <>
      {/* Mobile: 1 single column */}
      <div className="flex flex-col gap-3 md:hidden">
        {items.map(renderItem)}
      </div>

      {/* Web / Tablet / Desktop: 3 masonry columns */}
      <div className="hidden md:grid md:grid-cols-3 gap-3">
        <div className="flex flex-col gap-3">{col1.map(renderItem)}</div>
        <div className="flex flex-col gap-3">{col2.map(renderItem)}</div>
        <div className="flex flex-col gap-3">{col3.map(renderItem)}</div>
      </div>
    </>
  );
}
