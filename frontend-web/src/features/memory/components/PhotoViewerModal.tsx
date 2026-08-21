'use client';

import { useEffect, useCallback } from 'react';
import Image from 'next/image';
import { X, ChevronLeft, ChevronRight, Share2, Download, FolderPlus } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useMemoryUIStore } from '../store/useMemoryUIStore';
import { formatDateShort } from '@/lib/utils';

export function PhotoViewerModal() {
  const open = useMemoryUIStore((s) => s.photoViewerOpen);
  const item = useMemoryUIStore((s) => s.photoViewerItem);
  const items = useMemoryUIStore((s) => s.photoViewerLocationItems);
  const index = useMemoryUIStore((s) => s.photoViewerIndex);
  const close = useMemoryUIStore((s) => s.closePhotoViewer);
  const navigate = useMemoryUIStore((s) => s.navigatePhotoViewer);

  const photoItems = items.filter((i) => i.type === 'photo');
  const totalPhotos = photoItems.length;

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (!open) return;
      switch (e.key) {
        case 'Escape':
          close();
          break;
        case 'ArrowLeft':
          navigate('prev');
          break;
        case 'ArrowRight':
          navigate('next');
          break;
      }
    },
    [open, close, navigate]
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  // Lock body scroll when open
  useEffect(() => {
    if (open) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [open]);

  return (
    <AnimatePresence>
      {open && item && (
        <motion.div
          className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-black/95 backdrop-blur-sm"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.2 }}
        >
          {/* Close button */}
          <button
            onClick={close}
            className="absolute top-4 left-4 z-10 rounded-full bg-white/10 p-2.5 text-white/70 transition-colors"
            aria-label="Đóng"
          >
            <X className="h-5 w-5" />
          </button>

          {/* Main photo area */}
          <div className="relative flex-1 flex items-center justify-center w-full max-w-lg px-6 py-16">
            {/* Navigation arrows */}
            {totalPhotos > 1 && (
              <>
                <button
                  onClick={() => navigate('prev')}
                  className="absolute left-2 sm:left-4 z-10 rounded-full bg-white/10 p-2 text-white/60"
                  aria-label="Ảnh trước"
                >
                  <ChevronLeft className="h-5 w-5" />
                </button>
                <button
                  onClick={() => navigate('next')}
                  className="absolute right-2 sm:right-4 z-10 rounded-full bg-white/10 p-2 text-white/60"
                  aria-label="Ảnh sau"
                >
                  <ChevronRight className="h-5 w-5" />
                </button>
              </>
            )}

            {/* Photo card with Locket polaroid style */}
            <motion.div
              key={item.id}
              className="relative w-full aspect-[3/4] max-h-[70vh] rounded-[28px] overflow-hidden shadow-2xl"
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0, y: 100 }}
              transition={{ type: 'spring', stiffness: 300, damping: 25 }}
            >
              {item.imageUrl && (
                <Image
                  src={item.imageUrl}
                  alt={item.caption || 'Kỷ niệm'}
                  fill
                  className="object-cover"
                  sizes="(max-width: 512px) 100vw, 512px"
                  priority
                />
              )}
            </motion.div>
          </div>

          {/* Bottom info */}
          <div className="pb-6 px-4 text-center space-y-3 w-full max-w-lg">
            {/* Caption */}
            {item.caption && (
              <div className="inline-block rounded-xl bg-white/10 backdrop-blur-md px-4 py-2 border border-white/10 shadow-sm">
                <p className="text-white/95 text-sm font-medium">
                  📍 {item.caption}
                </p>
              </div>
            )}

            {/* Timestamp */}
            <p className="text-white/40 text-xs">
              {formatDateShort(item.createdAt)}
            </p>

            {/* Dot indicators */}
            {totalPhotos > 1 && (
              <div className="flex items-center justify-center gap-1.5">
                {photoItems.map((_, i) => (
                  <div
                    key={i}
                    className={`rounded-full transition-all duration-200 ${
                      i === index
                        ? 'w-6 h-1.5 bg-white'
                        : 'w-1.5 h-1.5 bg-white/30'
                    }`}
                  />
                ))}
              </div>
            )}

            {/* Action buttons */}
            <div className="flex items-center justify-center gap-8 pt-2">
              <button className="flex flex-col items-center gap-1 text-white/50">
                <Share2 className="h-5 w-5" />
                <span className="text-[10px]">Chia sẻ</span>
              </button>
              <button className="flex flex-col items-center gap-1 text-white/50">
                <Download className="h-5 w-5" />
                <span className="text-[10px]">Lưu</span>
              </button>
              <button className="flex flex-col items-center gap-1 text-white/50">
                <FolderPlus className="h-5 w-5" />
                <span className="text-[10px]">Album</span>
              </button>
            </div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
