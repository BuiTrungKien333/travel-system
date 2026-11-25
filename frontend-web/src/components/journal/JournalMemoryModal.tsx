"use client";

import React, { useEffect, useState } from "react";
import {
  X,
  MapPin,
  Calendar,
  Compass,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { format } from "date-fns";
import { vi } from "date-fns/locale";
import { useJournalUIStore } from "@/features/journal/store/useJournalUIStore";
import type { Memory } from "@/features/journal/types";

interface JournalMemoryModalProps {
  allMemories: Memory[];
}

export function JournalMemoryModal({ allMemories }: JournalMemoryModalProps) {
  const activeMemory = useJournalUIStore((s) => s.activeMemoryForLightbox);
  const [photoIndex, setPhotoIndex] = useState(0);
  const photos = activeMemory?.images?.length
    ? activeMemory.images
    : activeMemory?.image
      ? [activeMemory.image]
      : [];
  useEffect(() => setPhotoIndex(0), [activeMemory?.id]);
  const setActiveMemory = useJournalUIStore(
    (s) => s.setActiveMemoryForLightbox,
  );

  // Keyboard navigation
  useEffect(() => {
    if (!activeMemory) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setActiveMemory(null);
      } else if (e.key === "ArrowLeft") {
        setPhotoIndex((index) => Math.max(0, index - 1));
      } else if (e.key === "ArrowRight") {
        setPhotoIndex((index) =>
          Math.min(Math.max(0, photos.length - 1), index + 1),
        );
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [activeMemory, photos.length, setActiveMemory]);

  if (!activeMemory) return null;

  const currentIndex = allMemories.findIndex((m) => m.id === activeMemory.id);
  const hasPrev = photoIndex > 0;
  const hasNext = photoIndex < photos.length - 1;

  const formattedDate = activeMemory.createdAt
    ? format(new Date(activeMemory.createdAt), "dd MMMM yyyy, HH:mm", {
        locale: vi,
      })
    : "";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 p-4 sm:p-6 backdrop-blur-md animate-in fade-in duration-200">
      {/* Backdrop Click Close */}
      <div className="absolute inset-0" onClick={() => setActiveMemory(null)} />

      {/* Close button */}
      <button
        type="button"
        onClick={() => setActiveMemory(null)}
        className="absolute right-4 top-4 z-50 flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-white backdrop-blur-md hover:bg-white/20 transition"
        aria-label="Đóng xem kỷ niệm"
      >
        <X className="h-5 w-5" />
      </button>

      {/* Prev / Next buttons */}
      {hasPrev && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            setPhotoIndex(photoIndex - 1);
          }}
          className="absolute left-4 top-1/2 -translate-y-1/2 z-50 flex h-11 w-11 items-center justify-center rounded-full bg-white/10 text-white backdrop-blur-md hover:bg-white/20 transition"
          aria-label="Ảnh trước"
        >
          <ChevronLeft className="h-6 w-6" />
        </button>
      )}

      {hasNext && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            setPhotoIndex(photoIndex + 1);
          }}
          className="absolute right-4 top-1/2 -translate-y-1/2 z-50 flex h-11 w-11 items-center justify-center rounded-full bg-white/10 text-white backdrop-blur-md hover:bg-white/20 transition"
          aria-label="Ảnh kế tiếp"
        >
          <ChevronRight className="h-6 w-6" />
        </button>
      )}

      {/* Modal Card */}
      <div
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label={activeMemory.title}
        className="relative z-10 flex max-h-[90vh] w-full max-w-4xl flex-col overflow-y-auto rounded-3xl bg-slate-900 shadow-2xl border border-white/10 lg:flex-row animate-in zoom-in-95 duration-200"
      >
        {/* Large Photo Section */}
        <div className="relative flex flex-1 items-center justify-center bg-black min-h-[300px] lg:min-h-[500px]">
          {photos.length ? (
            <img
              src={photos[Math.min(photoIndex, photos.length - 1)]}
              alt={activeMemory.title}
              className="max-h-[75vh] w-full object-contain"
            />
          ) : (
            <div className="flex h-full min-h-32 w-full items-center justify-center bg-slate-100 text-sm text-slate-500">
              Chưa có ảnh
            </div>
          )}
        </div>

        {/* Info Sidebar Section */}
        <div className="flex w-full flex-col justify-between bg-slate-900 p-6 text-white lg:w-80 lg:border-l lg:border-white/10">
          <div>
            <div className="flex items-center gap-2">
              <span className="rounded-full bg-cyan-500/20 px-2.5 py-0.5 text-xs font-bold text-cyan-400 border border-cyan-500/30">
                {activeMemory.province || "Việt Nam"}
              </span>
              <span className="text-xs text-slate-400">
                {photos.length
                  ? `Ảnh ${photoIndex + 1} / ${photos.length}`
                  : "Kỷ niệm ghi chú"}
              </span>
            </div>

            <h3 className="mt-3 text-lg font-extrabold text-white leading-snug">
              {activeMemory.title}
            </h3>

            {activeMemory.note && (
              <p className="mt-3 rounded-2xl bg-white/5 p-3.5 text-xs text-slate-300 leading-relaxed border border-white/5">
                {activeMemory.note}
              </p>
            )}

            <div className="mt-4 space-y-2 text-xs text-slate-400 border-t border-white/10 pt-4">
              <div className="flex items-center gap-2">
                <MapPin className="h-4 w-4 text-cyan-400 shrink-0" />
                <span className="text-slate-200">{activeMemory.location}</span>
              </div>

              {formattedDate && (
                <div className="flex items-center gap-2">
                  <Calendar className="h-4 w-4 text-cyan-400 shrink-0" />
                  <span>{formattedDate}</span>
                </div>
              )}

              {activeMemory.journeyTitle && (
                <div className="flex items-center gap-2">
                  <Compass className="h-4 w-4 text-cyan-400 shrink-0" />
                  <span className="text-slate-300 font-medium">
                    {activeMemory.journeyTitle}
                  </span>
                </div>
              )}
            </div>
          </div>

          <div className="mt-6 pt-4 border-t border-white/10 flex flex-wrap gap-3 justify-end">
            {currentIndex > 0 && (
              <button
                type="button"
                className="text-xs"
                onClick={() => setActiveMemory(allMemories[currentIndex - 1])}
              >
                Kỷ niệm trước
              </button>
            )}
            {currentIndex >= 0 && currentIndex < allMemories.length - 1 && (
              <button
                type="button"
                className="text-xs"
                onClick={() => setActiveMemory(allMemories[currentIndex + 1])}
              >
                Kỷ niệm kế tiếp
              </button>
            )}
            <button
              type="button"
              onClick={() => setActiveMemory(null)}
              className="rounded-xl bg-white/10 px-4 py-2 text-xs font-bold text-white hover:bg-white/20 transition"
            >
              Đóng
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
