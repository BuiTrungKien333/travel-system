'use client';

export function TripCardSkeleton() {
  return (
    <div className="flex-shrink-0 w-[280px] sm:w-[320px] h-[360px] sm:h-[380px] rounded-[28px] bg-slate-200 animate-pulse" />
  );
}

export function PhotoGridSkeleton() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
      <div className="flex flex-col gap-3">
        <div className="aspect-[3/4] rounded-[20px] bg-slate-200 animate-pulse" />
        <div className="aspect-square rounded-[20px] bg-slate-200 animate-pulse" />
      </div>
      <div className="flex flex-col gap-3">
        <div className="aspect-square rounded-[20px] bg-slate-200 animate-pulse" />
        <div className="aspect-[3/4] rounded-[20px] bg-slate-200 animate-pulse" />
      </div>
      <div className="hidden md:flex flex-col gap-3">
        <div className="aspect-[4/3] rounded-[20px] bg-slate-200 animate-pulse" />
        <div className="aspect-[3/4] rounded-[20px] bg-slate-200 animate-pulse" />
      </div>
    </div>
  );
}

export function LocationSectionSkeleton() {
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <div className="w-8 h-8 rounded-full bg-slate-200 animate-pulse" />
        <div className="space-y-1.5">
          <div className="w-44 h-4 rounded bg-slate-200 animate-pulse" />
          <div className="w-28 h-3 rounded bg-slate-200 animate-pulse" />
        </div>
      </div>
      <PhotoGridSkeleton />
    </div>
  );
}
