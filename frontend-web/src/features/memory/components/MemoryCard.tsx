"use client";

import Image from "next/image";
import { MapPin, Image as ImageIcon } from "lucide-react";
import { formatDateShort } from "@/lib/utils";
import type { Memory, MemoryDetail } from "../types";
import { useMemoryUIStore } from "../store/useMemoryUIStore";

interface MemoryCardProps {
  memory: Memory;
}

export function MemoryCard({ memory }: MemoryCardProps) {
  const openLightbox = useMemoryUIStore((s) => s.openLightbox);

  return (
    <button
      className="group block w-full text-left rounded-xl overflow-hidden border border-border/70 bg-card hover:border-border transition-colors duration-150"
      onClick={() => {
        openLightbox(
          {
            ...memory,
            media: (memory as MemoryDetail).media ?? [],
          },
          0,
        );
      }}
      aria-label={`Kỷ niệm tại ${memory.placeName ?? "chuyến đi"}`}
    >
      {/* Cover image */}
      <div className="relative aspect-[4/3] bg-muted overflow-hidden">
        {memory.coverMediaUrl ? (
          <Image
            src={memory.coverMediaUrl}
            alt={memory.placeName ?? "Kỷ niệm chuyến đi"}
            fill
            className="object-cover transition-transform duration-300 ease-out group-hover:scale-[1.02]"
            sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw"
          />
        ) : (
          <div className="flex h-full items-center justify-center bg-muted text-muted-foreground/30">
            <ImageIcon className="h-8 w-8" />
          </div>
        )}

        {memory.mediaCount > 1 && (
          <span className="absolute top-2.5 right-2.5 rounded-md bg-black/60 px-2 py-0.5 text-[10px] font-medium text-white backdrop-blur-xs">
            +{memory.mediaCount - 1}
          </span>
        )}
      </div>

      {/* Info Section */}
      <div className="p-3 space-y-1 bg-card">
        <div className="flex items-center justify-between gap-2 text-[11px] text-muted-foreground">
          <span className="font-medium text-primary/80">
            {formatDateShort(memory.createdAt)}
          </span>
          {memory.placeName && (
            <span className="truncate flex items-center gap-1">
              <MapPin className="h-3 w-3 shrink-0 opacity-70" />
              <span className="truncate">{memory.placeName}</span>
            </span>
          )}
        </div>

        {memory.note ? (
          <p className="text-xs text-foreground line-clamp-2 leading-relaxed pt-0.5">
            {memory.note}
          </p>
        ) : (
          <p className="text-xs text-muted-foreground/50 italic pt-0.5">
            Không có ghi chú
          </p>
        )}
      </div>
    </button>
  );
}
