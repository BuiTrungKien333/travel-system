"use client";
import { useState } from "react";
import Image from "next/image";
import type { PlacePhoto } from "../types";

export function PlaceThumbnail({
  name,
  photo,
  className = "",
}: {
  name: string;
  photo?: PlacePhoto | null;
  className?: string;
}) {
  const [failedUrl, setFailedUrl] = useState<string>();
  const available = photo?.url && failedUrl !== photo.url;
  return (
    <div
      className={`relative shrink-0 overflow-hidden rounded-lg border bg-muted ${className}`}
    >
      {available ? (
        <Image
          src={photo.url}
          alt={name}
          fill
          unoptimized
          sizes="96px"
          className="object-cover"
          onError={() => setFailedUrl(photo.url)}
        />
      ) : (
        <div
          className="flex h-full flex-col items-center justify-center bg-gradient-to-br from-cyan-50 to-emerald-100 text-cyan-800"
          aria-label={`Minh họa địa điểm ${name}`}
        >
          <svg
            viewBox="0 0 80 44"
            className="w-16"
            role="img"
            aria-label="Phong cảnh minh họa"
          >
            <circle cx="59" cy="10" r="6" fill="#fbbf24" />
            <path d="M0 40 24 10 43 32 55 19 80 40Z" fill="#67b8a6" />
            <path d="m0 44 28-20 24 20Z" fill="#208e88" />
          </svg>
          <span className="text-[9px]">
            {photo?.url ? "Ảnh tạm không tải được" : "Ảnh minh họa"}
          </span>
        </div>
      )}
    </div>
  );
}

export function PhotoAttribution({ photo }: { photo?: PlacePhoto | null }) {
  return (
    <>
      {photo?.attributions?.map((source, index) => (
        <p
          key={index}
          className="break-words text-[10px] text-muted-foreground"
        >
          Ảnh:{" "}
          {source.url ? (
            <a
              href={source.url}
              target="_blank"
              rel="noopener noreferrer"
              className="underline"
              onClick={(event) => event.stopPropagation()}
            >
              {source.label}
            </a>
          ) : (
            source.label
          )}
        </p>
      ))}
    </>
  );
}
