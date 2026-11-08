'use client';

import { useEffect, useRef, useState } from 'react';
import Image from 'next/image';
import { X, ChevronLeft, ChevronRight, ImagePlus, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { useMemoryUIStore } from '../store/useMemoryUIStore';
import { useDeleteMemoryMutation } from '../api/useDeleteMemoryMutation';
import { getMemory, useDeleteMediaMutation, useUpdateMemoryMutation } from '../api/useMemoryActions';
import { UploadMemoryDialog } from './UploadMemoryDialog';
import type { MemoryDetail } from '../types';

function localDate(value?: string) {
  const date = value ? new Date(value) : new Date();
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
}

export function Lightbox() {
  const memory = useMemoryUIStore(s => s.lightboxMemory);
  return memory ? <MemoryViewer key={memory.id} memory={memory} /> : null;
}

function MemoryViewer({ memory }: { memory: MemoryDetail }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const { closeLightbox, openLightbox, lightboxMediaIndex: index, setLightboxMediaIndex } = useMemoryUIStore();
  const [note, setNote] = useState(memory.note ?? '');
  const [date, setDate] = useState(localDate(memory.experiencedAt ?? memory.createdAt));
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [adding, setAdding] = useState(false);
  const [failedUrl, setFailedUrl] = useState('');
  const update = useUpdateMemoryMutation();
  const removePhoto = useDeleteMediaMutation();
  const removeMemory = useDeleteMemoryMutation();
  const busy = update.isPending || removePhoto.isPending || removeMemory.isPending;
  const current = memory.media[index];

  useEffect(() => {
    dialog.current?.showModal();
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = previous; };
  }, []);

  function navigate(delta: number) {
    if (memory.media.length) setLightboxMediaIndex((index + delta + memory.media.length) % memory.media.length);
  }
  async function reload() {
    try {
      const fresh = await getMemory(memory.id);
      openLightbox(fresh, Math.min(index, Math.max(0, fresh.media.length - 1)));
      setNote(fresh.note ?? ''); setError('');
    } catch (e) { setError(e instanceof Error ? e.message : 'Không thể tải lại.'); }
  }
  async function save(event: React.FormEvent) {
    event.preventDefault(); setError(''); setMessage('');
    try {
      const changedDate = date !== localDate(memory.experiencedAt ?? memory.createdAt);
      const saved = await update.mutateAsync({ id: memory.id, version: memory.version!, note: note.trim() || null,
        ...(changedDate ? { experiencedAt: new Date(date).toISOString() } : {}) });
      openLightbox(saved, index); setMessage('Đã lưu thay đổi.');
    } catch (e) { setError(e instanceof Error ? e.message : 'Không thể lưu.'); }
  }

  return <>
    <dialog ref={dialog} aria-label="Chi tiết kỷ niệm" onCancel={event => { event.preventDefault(); if (!busy) closeLightbox(); }}
      onKeyDown={event => {
        const tag = (event.target as HTMLElement).tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA') return;
        if (event.key === 'ArrowLeft') { event.preventDefault(); navigate(-1); }
        if (event.key === 'ArrowRight') { event.preventDefault(); navigate(1); }
      }} className="m-auto h-[90dvh] w-[95vw] max-w-6xl overflow-auto rounded-2xl bg-background p-0 text-foreground shadow-xl backdrop:bg-black/80">
      <div className="flex min-h-full flex-col md:flex-row">
        <div className="relative min-h-[40vh] flex-1 bg-black md:min-h-[85dvh]">
          {current && failedUrl !== current.url ? <Image src={current.url} alt={memory.note || 'Ảnh kỷ niệm'} fill sizes="(max-width: 768px) 95vw, 65vw" className="object-contain" priority onError={() => setFailedUrl(current.url)} />
            : current ? <div className="flex min-h-[40vh] h-full flex-col items-center justify-center gap-3 p-8 text-sm text-white/70"><p>Chưa tải được ảnh.</p><button onClick={() => setFailedUrl('')} className="underline">Thử tải ảnh lại</button></div>
            : <p className="flex h-full min-h-[40vh] items-center justify-center p-8 text-sm text-white/70">Kỷ niệm này chưa có ảnh.</p>}
          {memory.media.length > 1 && <>
            <button onClick={() => navigate(-1)} aria-label="Ảnh trước" className="absolute left-2 top-1/2 rounded-full bg-black/60 p-2 text-white"><ChevronLeft /></button>
            <button onClick={() => navigate(1)} aria-label="Ảnh kế tiếp" className="absolute right-2 top-1/2 rounded-full bg-black/60 p-2 text-white"><ChevronRight /></button>
            <span className="absolute bottom-3 left-1/2 -translate-x-1/2 rounded-full bg-black/70 px-3 py-1 text-xs text-white">{index + 1}/{memory.media.length}</span>
          </>}
        </div>
        <div className="w-full space-y-4 p-5 md:w-80 md:shrink-0">
          <div className="flex items-center justify-between gap-2">
            <h2 className="font-semibold">Kỷ niệm của bạn</h2>
            <button onClick={closeLightbox} disabled={busy} aria-label="Đóng chi tiết" className="rounded-lg p-2 hover:bg-muted disabled:opacity-40"><X size={20} /></button>
          </div>
          {memory.version !== undefined && <form onSubmit={save} className="space-y-3">
            <label className="block space-y-1 text-sm"><span>Ghi chú</span>
              <textarea value={note} onChange={e => setNote(e.target.value)} maxLength={2000} rows={4} disabled={busy} className="w-full rounded-lg border border-border bg-background p-2" />
              <span className="block text-right text-xs text-muted-foreground">{note.length}/2000</span>
            </label>
            <label className="block space-y-1 text-sm"><span>Thời gian trải nghiệm</span>
              <input type="datetime-local" required max={localDate()} value={date} onChange={e => setDate(e.target.value)} disabled={busy} className="w-full rounded-lg border border-border bg-background p-2" />
            </label>
            <button disabled={busy} className="w-full rounded-lg bg-primary p-2 text-sm font-semibold text-primary-foreground disabled:opacity-50">{update.isPending ? 'Đang lưu…' : 'Lưu thay đổi'}</button>
          </form>}
          {error && <div role="alert" className="rounded-lg bg-destructive/10 p-3 text-sm text-destructive"><p>{error}</p>
            <button type="button" onClick={reload} disabled={busy} className="mt-2 underline">Tải lại bản mới nhất</button>
          </div>}
          {message && <p role="status" className="text-sm text-primary">{message}</p>}
          {memory.version !== undefined && <div className="space-y-2 border-t border-border pt-4">
            <button disabled={busy} onClick={() => setAdding(true)} className="flex w-full items-center gap-2 rounded-lg border border-border p-2 text-sm"><ImagePlus size={16} />Thêm ảnh</button>
            {current && <>
              {index > 0 && <button disabled={busy} onClick={async () => {
                try {
                  const order = [current.id, ...memory.media.filter(m => m.id !== current.id).map(m => m.id)];
                  const saved = await update.mutateAsync({ id: memory.id, version: memory.version!, mediaOrder: order });
                  openLightbox(saved, 0); setMessage('Đã đổi ảnh bìa.'); setError('');
                } catch (e) { setError(e instanceof Error ? e.message : 'Không thể đổi ảnh bìa.'); }
              }} className="w-full rounded-lg border border-border p-2 text-sm">Đặt ảnh này làm ảnh bìa</button>}
              <button disabled={busy} onClick={async () => {
                if (!window.confirm('Xóa ảnh này khỏi kỷ niệm? Ảnh sẽ được xóa khỏi kho lưu trữ.')) return;
                try { await removePhoto.mutateAsync(current.id); await reload(); setMessage('Đã xóa ảnh.'); }
                catch (e) { setError(e instanceof Error ? e.message : 'Không thể xóa ảnh.'); }
              }} className="w-full rounded-lg border border-border p-2 text-sm text-destructive">Xóa ảnh đang xem</button>
            </>}
            <button disabled={busy} onClick={async () => {
              if (!window.confirm('Xóa kỷ niệm và tất cả ảnh của nó? Bạn không thể hoàn tác thao tác này.')) return;
              try { await removeMemory.mutateAsync(memory.id); closeLightbox(); toast.success('Đã xóa kỷ niệm'); }
              catch (e) { setError(e instanceof Error ? e.message : 'Không thể xóa kỷ niệm.'); }
            }} className="flex w-full items-center gap-2 rounded-lg p-2 text-sm text-destructive"><Trash2 size={16} />Xóa kỷ niệm</button>
          </div>}
        </div>
      </div>
    </dialog>
    {adding && <UploadMemoryDialog tripId={memory.tripId || undefined} memory={memory} onClose={() => setAdding(false)} onSaved={saved => openLightbox(saved, index)} />}
  </>;
}
