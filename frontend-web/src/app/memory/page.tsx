'use client';

import { Plus } from 'lucide-react';
import Link from 'next/link';
import { useMemoryQuery } from '@/features/memory/api/useMemoryQuery';
import { useMemoryUIStore } from '@/features/memory/store/useMemoryUIStore';
import { MemoryGallery } from '@/features/memory/components/MemoryGallery';
import { Lightbox } from '@/features/memory/components/Lightbox';
import { UploadMemoryDialog } from '@/features/memory/components/UploadMemoryDialog';
import { useTripsQuery } from '@/features/trip/api/useTripsQuery';
import { useAuthStore } from '@/features/auth/store/useAuthStore';

export default function MemoryPage() {
  const authenticated = useAuthStore(s => s.isAuthenticated);
  const activeTrip = useMemoryUIStore(s => s.activeTrip);
  const setActiveTrip = useMemoryUIStore(s => s.setActiveTrip);
  const isUploadOpen = useMemoryUIStore(s => s.isUploadOpen);
  const setIsUploadOpen = useMemoryUIStore(s => s.setIsUploadOpen);
  const query = useMemoryQuery(activeTrip ?? undefined);
  const trips = useTripsQuery();
  const memories = query.data?.pages.flatMap(page => page.content) ?? [];

  if (!authenticated) return <main className="mx-auto max-w-lg space-y-4 px-6 py-20 text-center">
    <h1 className="text-2xl font-semibold">Kho kỷ niệm của bạn</h1>
    <p className="text-muted-foreground">Đăng nhập để xem và lưu những khoảnh khắc trong hành trình.</p>
    <Link href="/auth/login" className="inline-block rounded-lg bg-primary px-5 py-2 text-primary-foreground">Đăng nhập</Link>
  </main>;

  return <main className="mx-auto w-full max-w-6xl space-y-6 px-4 py-8 sm:px-8">
    <div className="flex flex-wrap items-start justify-between gap-4 border-b border-border pb-5">
      <div><p className="text-sm font-medium text-primary">Nhật ký hành trình</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight sm:text-3xl">Kho kỷ niệm</h1>
        <p className="mt-2 text-sm text-muted-foreground">Những khoảnh khắc, hình ảnh và cảm nhận bạn đã lưu.</p>
      </div>
      <button onClick={() => setIsUploadOpen(true)} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"><Plus size={17} />Thêm kỷ niệm</button>
    </div>
    <div className="flex flex-wrap items-center justify-between gap-3">
      <label className="flex items-center gap-2 text-sm">Chuyến đi
        <select value={activeTrip ?? ''} onChange={e => setActiveTrip(e.target.value || null)} className="max-w-[60vw] rounded-lg border border-border bg-background p-2">
          <option value="">Tất cả kỷ niệm</option>
          {trips.data?.map(trip => <option key={trip.id} value={trip.id}>{trip.name}</option>)}
        </select>
      </label>
      {activeTrip && <Link href={`/trips/${activeTrip}/memories`} className="text-sm text-primary underline">Mở chuyến đi</Link>}
    </div>
    {query.data && <p className="text-sm text-muted-foreground">{query.data.pages[0].totalElements} kỷ niệm</p>}
    {query.isError && <div role="alert" className="rounded-lg border border-destructive/30 p-4 text-sm text-destructive">
      <p>{query.error.message}</p><button onClick={() => query.refetch()} className="mt-2 underline">Thử lại</button>
    </div>}
    {(!query.isError || memories.length > 0) && <MemoryGallery memories={memories} isLoading={query.isPending} />}
    {query.hasNextPage && <button disabled={query.isFetchingNextPage} onClick={() => query.fetchNextPage()} className="mx-auto block rounded-lg border border-border px-5 py-2 text-sm">
      {query.isFetchingNextPage ? 'Đang tải…' : 'Tải thêm kỷ niệm'}
    </button>}
    <Lightbox />
    {isUploadOpen && <UploadMemoryDialog tripId={activeTrip ?? undefined} onClose={() => setIsUploadOpen(false)} />}
  </main>;
}
