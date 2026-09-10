'use client';

import { useEffect, useCallback } from 'react';
import Image from 'next/image';
import { motion, AnimatePresence } from 'framer-motion';
import { X, ChevronLeft, ChevronRight, MapPin } from 'lucide-react';
import { useMemoryUIStore } from '../store/useMemoryUIStore';
import { formatDateShort } from '@/lib/utils';

export function Lightbox() {
  const { lightboxMemory, lightboxMediaIndex, closeLightbox, setLightboxMediaIndex } = useMemoryUIStore();

  const handleNext = useCallback((e?: React.MouseEvent) => {
    e?.stopPropagation();
    if (!lightboxMemory || !lightboxMemory.media.length) return;
    if (lightboxMediaIndex < lightboxMemory.media.length - 1) {
      setLightboxMediaIndex(lightboxMediaIndex + 1);
    } else {
      setLightboxMediaIndex(0); // loop
    }
  }, [lightboxMemory, lightboxMediaIndex, setLightboxMediaIndex]);

  const handlePrev = useCallback((e?: React.MouseEvent) => {
    e?.stopPropagation();
    if (!lightboxMemory || !lightboxMemory.media.length) return;
    if (lightboxMediaIndex > 0) {
      setLightboxMediaIndex(lightboxMediaIndex - 1);
    } else {
      setLightboxMediaIndex(lightboxMemory.media.length - 1); // loop
    }
  }, [lightboxMemory, lightboxMediaIndex, setLightboxMediaIndex]);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!lightboxMemory) return;
      if (e.key === 'Escape') closeLightbox();
      if (e.key === 'ArrowRight') handleNext();
      if (e.key === 'ArrowLeft') handlePrev();
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [lightboxMemory, closeLightbox, handleNext, handlePrev]);

  if (!lightboxMemory) return null;

  const currentMedia = lightboxMemory.media[lightboxMediaIndex];

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-[100] flex items-center justify-center bg-black/95 backdrop-blur-sm"
        onClick={closeLightbox}
      >
        {/* Close Button */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            closeLightbox();
          }}
          className="absolute top-6 right-6 z-[110] rounded-full bg-white/10 p-2.5 text-white hover:bg-white/20 transition-colors"
        >
          <X className="h-6 w-6" />
        </button>

        {/* Media Container */}
        <div className="relative w-full max-w-5xl h-[85vh] flex items-center justify-center p-4">
          {currentMedia && (
            <motion.div
              key={currentMedia.id}
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.2 }}
              className="relative w-full h-full"
            >
              <Image
                src={currentMedia.url}
                alt={lightboxMemory.note || 'Memory photo'}
                fill
                className="object-contain"
                sizes="100vw"
                priority
              />
            </motion.div>
          )}

          {/* Navigation Controls */}
          {lightboxMemory.media.length > 1 && (
            <>
              <button
                onClick={handlePrev}
                className="absolute left-4 md:left-12 top-1/2 -translate-y-1/2 rounded-full bg-white/10 p-3 text-white hover:bg-white/20 transition-colors"
              >
                <ChevronLeft className="h-8 w-8" />
              </button>
              <button
                onClick={handleNext}
                className="absolute right-4 md:right-12 top-1/2 -translate-y-1/2 rounded-full bg-white/10 p-3 text-white hover:bg-white/20 transition-colors"
              >
                <ChevronRight className="h-8 w-8" />
              </button>
            </>
          )}
        </div>

        {/* Info Overlay */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/90 via-black/60 to-transparent p-6 pb-8 text-white pointer-events-none"
        >
          <div className="max-w-5xl mx-auto space-y-2 pointer-events-auto" onClick={(e) => e.stopPropagation()}>
            {lightboxMemory.placeName && (
              <div className="flex items-center gap-2 text-sm text-white/80 font-medium">
                <MapPin className="h-4 w-4 text-primary" />
                <span>{lightboxMemory.placeName}</span>
              </div>
            )}
            <p className="text-xs text-white/60">
              {formatDateShort(lightboxMemory.createdAt)}
            </p>
            {lightboxMemory.note && (
              <p className="text-base md:text-lg mt-3 max-w-2xl text-white/95 leading-relaxed">
                {lightboxMemory.note}
              </p>
            )}
            
            {/* Pagination Indicators */}
            {lightboxMemory.media.length > 1 && (
              <div className="flex items-center justify-center gap-1.5 mt-6 mb-2">
                {lightboxMemory.media.map((_, idx) => (
                  <button
                    key={idx}
                    onClick={(e) => {
                      e.stopPropagation();
                      setLightboxMediaIndex(idx);
                    }}
                    className={`h-1.5 rounded-full transition-all ${
                      idx === lightboxMediaIndex ? 'bg-primary w-6' : 'bg-white/40 w-1.5 hover:bg-white/60'
                    }`}
                  />
                ))}
              </div>
            )}
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
}
