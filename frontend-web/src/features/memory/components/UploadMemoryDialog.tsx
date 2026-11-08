'use client';

import { useEffect, useRef, useState } from 'react';
import Image from 'next/image';
import { useDropzone } from 'react-dropzone';
import { useQuery } from '@tanstack/react-query';
import { UploadCloud, X } from 'lucide-react';
import { toast } from 'sonner';
import apiClient from '@/lib/api';
import { DEFAULT_UPLOAD_LIMITS, selectionError, type UploadLimits } from '../api/contract';
import { MemorySaveError, useCreateMemoryMutation } from '../api/useCreateMemoryMutation';
import { getMemory } from '../api/useMemoryActions';
import type { CreateMemoryRequest, MemoryDetail } from '../types';

interface Props { tripId?: string; memory?: MemoryDetail; onClose: () => void; onSaved?: (memory: MemoryDetail) => void; }
interface Preview { id: string; file: File; url: string; }

export function UploadMemoryDialog({ tripId, memory, onClose, onSaved }: Props) {
  const dialog = useRef<HTMLDialogElement>(null);
  const abort = useRef<AbortController | null>(null);
  const previews = useRef<Preview[]>([]);
  const creation = useRef<CreateMemoryRequest | null>(null);
  const [files, setFiles] = useState<Preview[]>([]);
  const [note, setNote] = useState(memory?.note ?? '');
  const [placeId, setPlaceId] = useState('');
  const [persisted, setPersisted] = useState(memory);
  const [error, setError] = useState('');
  const [progress, setProgress] = useState(0);
  const save = useCreateMemoryMutation();
  const limitQuery = useQuery({ queryKey: ['media', 'limits'], queryFn: async () => (await apiClient.get<UploadLimits>('/media/limits')).data, staleTime: 60_000 });
  const limits = limitQuery.data ?? DEFAULT_UPLOAD_LIMITS;
  const places = useQuery({
    queryKey: ['trips', tripId, 'places'], enabled: !!tripId && !memory,
    queryFn: async () => (await apiClient.get<Array<{ placeId: string; place: { name: string } }>>(`/trips/${tripId}/places`)).data,
  });

  useEffect(() => {
    dialog.current?.showModal();
    return () => {
      abort.current?.abort();
      previews.current.forEach(p => URL.revokeObjectURL(p.url));
    };
  }, []);

  const remaining = Math.max(0, limits.maxFilesPerMemory - (persisted?.mediaCount ?? 0));
  const dropzone = useDropzone({
    accept: { 'image/jpeg': ['.jpg', '.jpeg'], 'image/png': ['.png'], 'image/gif': ['.gif'], 'image/webp': ['.webp'] },
    maxSize: limits.maxFileBytes, multiple: true, disabled: save.isPending,
    onDrop: (accepted, rejected) => {
      if (rejected.length) { setError('Có ảnh không hợp lệ hoặc vượt giới hạn dung lượng. Vui lòng chọn lại.'); return; }
      const unique = accepted.filter(file => !files.some(p => p.file.name === file.name && p.file.size === file.size && p.file.lastModified === file.lastModified));
      const issue = selectionError([...files.map(p => p.file), ...unique], limits, remaining);
      if (issue) { setError(issue); return; }
      const next = [...files, ...unique.map(file => ({ id: crypto.randomUUID(), file, url: URL.createObjectURL(file) }))];
      previews.current = next; setFiles(next); setError('');
    },
  });

  function remove(id: string) {
    const item = files.find(p => p.id === id);
    if (item) URL.revokeObjectURL(item.url);
    const next = files.filter(p => p.id !== id);
    previews.current = next; setFiles(next);
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const issue = selectionError(files.map(p => p.file), limits, remaining);
    if (issue) { setError(issue); return; }
    if (!files.length && !note.trim() && !persisted) { setError('Hãy chọn ảnh hoặc viết một ghi chú.'); return; }
    creation.current ??= { clientRequestId: crypto.randomUUID(), tripId, placeId: placeId || undefined, note: note.trim() || undefined };
    const controller = new AbortController(); abort.current = controller;
    setError(''); setProgress(0);
    try {
      const saved = await save.mutateAsync({ creation: creation.current, note, files: files.map(p => p.file), existing: persisted,
        signal: controller.signal, onProgress: setProgress });
      toast.success('Đã lưu kỷ niệm'); onSaved?.(saved); onClose();
    } catch (e) {
      if (e instanceof MemorySaveError && e.memory) setPersisted(e.memory);
      setError(e instanceof Error ? e.message : 'Không thể lưu kỷ niệm.');
    }
  }

  return (
    <dialog ref={dialog} aria-labelledby="upload-memory-title" onCancel={event => { event.preventDefault(); if (!save.isPending) onClose(); }}
      className="m-auto w-[calc(100%_-_2rem)] max-w-xl max-h-[90dvh] overflow-y-auto rounded-2xl border border-border bg-background p-5 text-foreground shadow-xl backdrop:bg-black/60 sm:p-6">
      <div className="flex items-center justify-between gap-3">
        <h2 id="upload-memory-title" className="text-lg font-semibold">{memory ? 'Thêm ảnh vào kỷ niệm' : 'Lưu kỷ niệm mới'}</h2>
        <button type="button" onClick={onClose} disabled={save.isPending} aria-label="Đóng" className="rounded-lg p-2 hover:bg-muted disabled:opacity-40"><X size={20} /></button>
      </div>
      <form onSubmit={submit} className="mt-4 space-y-4">
        <fieldset disabled={save.isPending} className="space-y-4 disabled:opacity-70">
          <div {...dropzone.getRootProps()} className={`cursor-pointer rounded-xl border-2 border-dashed p-6 text-center ${dropzone.isDragActive ? 'border-primary bg-primary/5' : 'border-border'}`}>
            <input {...dropzone.getInputProps()} aria-label="Chọn ảnh kỷ niệm" />
            <UploadCloud className="mx-auto mb-2 h-7 w-7 text-primary" />
            <p className="text-sm font-medium">Kéo ảnh vào đây hoặc bấm để chọn</p>
            <p className="mt-1 text-xs text-muted-foreground">JPEG, PNG, GIF, WebP · tối đa {Math.floor(limits.maxFileBytes / 1024 / 1024)} MB/ảnh</p>
            <p className="mt-1 text-xs text-muted-foreground">Tối đa {Math.min(limits.maxFilesPerRequest, remaining)} ảnh/lượt · {limits.maxFilesPerMemory} ảnh/kỷ niệm</p>
          </div>
          {files.length > 0 && <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
            {files.map(p => <div key={p.id} className="relative aspect-square overflow-hidden rounded-lg bg-muted">
              <Image unoptimized src={p.url} alt={p.file.name} fill sizes="120px" className="object-cover" />
              <button type="button" onClick={() => remove(p.id)} aria-label={`Bỏ ảnh ${p.file.name}`} className="absolute right-1 top-1 rounded-full bg-black/70 p-1 text-white"><X size={14} /></button>
            </div>)}
          </div>}
          {!!tripId && !memory && <label className="block space-y-1 text-sm">
            <span>Địa điểm (không bắt buộc)</span>
            <select value={placeId} onChange={e => setPlaceId(e.target.value)} disabled={!!persisted || !!creation.current || places.isError}
              className="w-full rounded-lg border border-border bg-background p-2">
              <option value="">Không gắn địa điểm</option>
              {places.data?.map(place => <option key={place.placeId} value={place.placeId}>{place.place.name}</option>)}
            </select>
            {places.isError && <span className="text-xs text-destructive">Không tải được địa điểm; bạn vẫn có thể lưu kỷ niệm không gắn địa điểm.</span>}
          </label>}
          <label className="block space-y-1 text-sm">
            <span>Ghi chú</span>
            <textarea value={note} onChange={e => setNote(e.target.value)} maxLength={2000} rows={3}
              placeholder="Bạn muốn ghi nhớ điều gì?" className="w-full resize-y rounded-lg border border-border bg-background p-3" />
            <span className="block text-right text-xs text-muted-foreground">{note.length}/2000</span>
          </label>
        </fieldset>
        {error && <div role="alert" className="rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
          <p>{error}</p>
          {persisted && <p className="mt-1 text-xs">Kỷ niệm đã được tạo. Thử lại sẽ tiếp tục trên bản này và không thêm ảnh trùng.</p>}
          {persisted && <button type="button" disabled={save.isPending} className="mt-2 underline" onClick={async () => {
            try { setPersisted(await getMemory(persisted.id)); setError(''); }
            catch (e) { setError(e instanceof Error ? e.message : 'Không thể tải lại.'); }
          }}>Kiểm tra bản đã lưu</button>}
        </div>}
        {limitQuery.isError && <p className="text-xs text-destructive">Chưa tải được giới hạn từ máy chủ; đang dùng giới hạn mặc định.</p>}
        {save.isPending && <div role="status" aria-live="polite" className="space-y-2 text-sm">
          <progress className="h-2 w-full" value={progress} max={100} aria-label="Tiến trình tải ảnh" />
          <p>{!files.length ? 'Đang lưu kỷ niệm…' : progress >= 99 ? 'Đã gửi ảnh, đang kiểm tra và lưu…' : `Đang gửi ảnh… ${progress}%`}</p>
        </div>}
        <div className="flex items-center justify-between gap-3">
          <span className="text-xs text-muted-foreground">{files.length} ảnh · {(files.reduce((sum, p) => sum + p.file.size, 0) / 1024 / 1024).toFixed(1)} MB</span>
          {save.isPending ? <button type="button" onClick={() => abort.current?.abort()} className="rounded-lg border border-border px-4 py-2 text-sm">Dừng tải lên</button>
            : <button type="submit" className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground">{persisted ? 'Lưu và tiếp tục' : 'Lưu kỷ niệm'}</button>}
        </div>
      </form>
    </dialog>
  );
}
