"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { Archive, Check, ChevronLeft, ChevronRight, Pencil, Plus, RotateCcw, Search } from "lucide-react";
import { Banner, Button } from "@/components/ui";
import { useSession } from "@/features/auth/api";
import { errorMessage } from "@/lib/errors";
import { labels, type CatalogPlace, type PlaceRequest } from "./data";
import { useAdminCatalog, useCatalogManagementAction, useCatalogProvinces, type CatalogFilters } from "./hooks";
import { PlaceDialog, type PlaceEditor } from "./place-dialog";

type PendingAction = { type: "archive" | "restore" | "review"; place: CatalogPlace };
const initialFilters: CatalogFilters = { provinceCode: "", status: "ALL", tag: "", q: "" };

export function PlacesManagementScreen() {
  const session = useSession();
  const owner = session.data?.id;
  const allowed = !!session.data?.permissions.includes("admin:catalog");
  const [filters, setFilters] = useState<CatalogFilters>(initialFilters);
  const [searchDraft, setSearchDraft] = useState("");
  const [page, setPage] = useState(0);
  const [editor, setEditor] = useState<PlaceEditor>();
  const [pending, setPending] = useState<PendingAction>();
  const [notice, setNotice] = useState("");
  const catalog = useAdminCatalog(owner, page, filters, allowed);
  const provinces = useCatalogProvinces(owner, allowed);
  const action = useCatalogManagementAction();
  const confirmDialog = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = confirmDialog.current;
    if (pending && dialog && !dialog.open) dialog.showModal();
    if (!pending && dialog?.open) dialog.close();
  }, [pending]);

  if (!allowed) return <Banner error>Chưa có quyền quản lý địa điểm.</Banner>;
  const data = catalog.data;
  const totalPages = Math.max(1, Math.ceil((data?.totalElements ?? 0) / (data?.size || 20)));

  function patchFilters(patch: Partial<CatalogFilters>) {
    setFilters((current) => ({ ...current, ...patch }));
    setPage(0);
  }
  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    patchFilters({ q: searchDraft.trim() });
  }
  async function savePlace(input: PlaceRequest) {
    if (!editor) return;
    setNotice("");
    try {
      await action.mutateAsync(editor.kind === "create"
        ? { type: "create", input }
        : { type: "update", id: editor.place.id, version: editor.place.version, input });
    } catch {
      return;
    }
    setEditor(undefined);
    setNotice(editor.kind === "create" ? "Đã tạo hồ sơ chờ duyệt." : "Đã lưu thay đổi và chuyển hồ sơ về chờ duyệt.");
    setPage(0);
    if (editor.kind === "create") patchFilters({ status: "PENDING" });
  }
  async function performPending() {
    if (!pending) return;
    const selected = pending;
    setNotice("");
    try {
      if (selected.type === "archive") await action.mutateAsync({ type: "archive", id: selected.place.id, version: selected.place.version });
      else if (selected.type === "restore") await action.mutateAsync({ type: "restore", id: selected.place.id, version: selected.place.version });
      else await action.mutateAsync({ type: "review", id: selected.place.id, version: selected.place.version, approved: true });
    } catch {
      return;
    }
    setPending(undefined);
    setNotice(selected.type === "archive" ? "Đã lưu trữ địa điểm." : selected.type === "restore" ? "Đã khôi phục hồ sơ ở trạng thái chờ duyệt." : "Đã duyệt và công khai địa điểm.");
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="eyebrow">CATALOG · QUẢN TRỊ DỮ LIỆU ĐỊA ĐIỂM</p>
          <h1 className="mt-2 text-3xl font-semibold">Quản lý địa điểm</h1>
          <p className="mt-2 max-w-3xl text-sm text-muted">Tìm và lọc hồ sơ trên toàn bộ tỉnh/thành; địa điểm mới hoặc vừa chỉnh sửa cần được duyệt trước khi công khai.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="secondary" disabled={catalog.isFetching} onClick={() => void catalog.refetch()}>Tải lại</Button>
          <Button type="button" onClick={() => { action.reset(); setEditor({ kind: "create" }); }}><Plus size={16} aria-hidden="true" /> Thêm địa điểm</Button>
        </div>
      </header>

      {notice ? <Banner>{notice}</Banner> : null}
      {catalog.isError ? <Banner error>{errorMessage(catalog.error)}</Banner> : null}
      {provinces.isError ? <Banner error>{errorMessage(provinces.error)}</Banner> : null}

      <section aria-label="Bộ lọc địa điểm" className="space-y-4 border-y border-line py-4">
        <form onSubmit={submitSearch} className="flex flex-col gap-3 sm:flex-row">
          <label className="sr-only" htmlFor="places-search">Tìm theo tên, địa chỉ hoặc mô tả</label>
          <div className="relative min-w-0 flex-1">
            <Search aria-hidden="true" size={17} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
            <input id="places-search" type="search" maxLength={200} value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} placeholder="Tìm tên, địa chỉ hoặc mô tả" className="input w-full pl-10" />
          </div>
          <Button type="submit" variant="secondary">Tìm kiếm</Button>
        </form>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-[minmax(12rem,1fr)_minmax(10rem,0.7fr)_auto]">
          <div className="space-y-1.5"><label htmlFor="places-province" className="label">Tỉnh/thành</label><select id="places-province" className="input w-full" value={filters.provinceCode} onChange={(event) => patchFilters({ provinceCode: event.target.value })}><option value="">Tất cả tỉnh/thành</option>{(provinces.data ?? []).map((province) => <option key={province.code} value={province.code}>{province.name}</option>)}</select></div>
          <div className="space-y-1.5"><label htmlFor="places-status" className="label">Trạng thái</label><select id="places-status" className="input w-full" value={filters.status} onChange={(event) => patchFilters({ status: event.target.value as CatalogFilters["status"] })}><option value="ALL">Tất cả trạng thái</option><option value="PENDING">Chờ duyệt</option><option value="APPROVED">Đã duyệt</option><option value="ARCHIVED">Đã lưu trữ</option></select></div>
          <div className="space-y-1.5"><label htmlFor="places-tag" className="label">Tag</label><input id="places-tag" maxLength={30} value={filters.tag} onChange={(event) => patchFilters({ tag: event.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, "") })} placeholder="Ví dụ: BEACH" className="input w-full" /></div>
          <div className="sm:col-span-2 lg:col-span-3"><button type="button" className="text-sm font-medium text-teal-700 underline-offset-4 hover:underline" onClick={() => { setSearchDraft(""); setFilters(initialFilters); setPage(0); }}>Xóa bộ lọc</button></div>
        </div>
      </section>

      <section aria-labelledby="places-list-title" className="space-y-4">
        <div className="flex flex-wrap items-baseline justify-between gap-2"><h2 id="places-list-title" className="text-lg font-semibold">Danh sách hồ sơ</h2>{data ? <p className="text-sm text-muted">{data.totalElements.toLocaleString("vi-VN")} địa điểm · Trang {page + 1}/{totalPages}</p> : null}</div>
        {catalog.isPending ? <p role="status" className="py-8 text-center text-sm text-muted">Đang tải địa điểm…</p> : null}
        {data && data.content.length === 0 ? <div className="border-y border-line py-12 text-center"><p className="font-medium">Không tìm thấy địa điểm</p><p className="mt-1 text-sm text-muted">Thử xóa bớt bộ lọc hoặc thêm hồ sơ mới.</p></div> : null}
        {data && data.content.length > 0 ? <div role="region" aria-labelledby="places-list-title" tabIndex={0} className="overflow-x-auto border-y border-line focus-visible:outline focus-visible:outline-2 focus-visible:outline-teal-700"><table className="w-full min-w-[980px] border-collapse text-left text-sm"><caption className="sr-only">Danh sách địa điểm, trạng thái duyệt và thao tác quản trị</caption><thead className="bg-canvas text-xs uppercase tracking-wide text-muted"><tr><th scope="col" className="px-4 py-3">Địa điểm</th><th scope="col" className="px-4 py-3">Tỉnh/thành</th><th scope="col" className="px-4 py-3">Tags</th><th scope="col" className="px-4 py-3">Kiểm chứng</th><th scope="col" className="px-4 py-3">Trạng thái</th><th scope="col" className="px-4 py-3">Thao tác</th></tr></thead><tbody className="divide-y divide-line">{data.content.map((place) => <tr key={place.id} className="align-top"><td className="max-w-sm px-4 py-4"><p className="font-semibold">{place.name}</p><p className="mt-1 line-clamp-2 text-xs text-muted">{place.address || "Chưa có địa chỉ"}</p><a href={place.sourceUrl} target="_blank" rel="noreferrer" className="mt-1 inline-block max-w-full truncate text-xs text-teal-700 underline">Nguồn tham khảo ↗</a></td><td className="px-4 py-4">{place.province ?? "Chưa gán"}</td><td className="max-w-52 px-4 py-4"><div className="flex flex-wrap gap-1">{place.tags.map((tag) => <span key={tag} className="badge bg-canvas text-muted ring-line">{tag}</span>)}</div></td><td className="px-4 py-4 text-xs"><p>Tọa độ: {qualityLabel(place.coordinatesQuality)}</p><p className="mt-1">Giờ: {qualityLabel(place.hoursQuality)}</p><p className="mt-1">{place.reviewedOn}</p></td><td className="px-4 py-4"><span className={`badge ${place.archivedAt ? "bg-slate-100 text-slate-700 ring-slate-200" : place.approved ? "bg-teal-50 text-teal-800 ring-teal-100" : "bg-amber-50 text-amber-800 ring-amber-100"}`}>{place.archivedAt ? "Đã lưu trữ" : place.approved ? "Đã duyệt" : "Chờ duyệt"}</span></td><td className="px-4 py-4"><div className="flex min-w-36 flex-wrap gap-1.5">{!place.archivedAt ? <><button type="button" className="action-button" aria-label={`Sửa ${place.name}`} onClick={() => { action.reset(); setEditor({ kind: "edit", place }); }}><Pencil size={15} aria-hidden="true" /></button>{!place.approved ? <button type="button" className="action-button text-teal-700" aria-label={`Duyệt ${place.name}`} onClick={() => { action.reset(); setPending({ type: "review", place }); }}><Check size={16} aria-hidden="true" /></button> : null}<button type="button" className="action-button text-red-700" aria-label={`Lưu trữ ${place.name}`} onClick={() => { action.reset(); setPending({ type: "archive", place }); }}><Archive size={15} aria-hidden="true" /></button></> : <button type="button" className="action-button text-teal-700" aria-label={`Khôi phục ${place.name}`} onClick={() => { action.reset(); setPending({ type: "restore", place }); }}><RotateCcw size={15} aria-hidden="true" /></button>}</div></td></tr>)}</tbody></table></div> : null}
        {data && totalPages > 1 ? <nav aria-label="Phân trang địa điểm" className="flex flex-wrap items-center justify-between gap-3"><p className="text-sm text-muted">Hiển thị {page * data.size + 1}–{Math.min((page + 1) * data.size, data.totalElements)} / {data.totalElements}</p><div className="flex items-center gap-1"><button type="button" className="page-button" aria-label="Trang trước" disabled={page === 0} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={16} aria-hidden="true" /></button>{Array.from({ length: totalPages }, (_, index) => index).filter((index) => index === 0 || index === totalPages - 1 || Math.abs(index - page) <= 1).map((index, position, visible) => <span key={index} className="contents">{position > 0 && visible[position - 1] !== index - 1 ? <span aria-hidden="true" className="px-1 text-muted">…</span> : null}<button type="button" aria-current={page === index ? "page" : undefined} aria-label={`Trang ${index + 1}`} className={`page-button ${page === index ? "page-button-active" : ""}`} onClick={() => setPage(index)}>{index + 1}</button></span>)}<button type="button" className="page-button" aria-label="Trang tiếp" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}><ChevronRight size={16} aria-hidden="true" /></button></div></nav> : null}
      </section>

      {editor ? <PlaceDialog editor={editor} provinces={provinces.data ?? []} busy={action.isPending} error={action.error} onClose={() => { if (!action.isPending) setEditor(undefined); }} onSave={savePlace} /> : null}
      <dialog ref={confirmDialog} aria-labelledby="catalog-confirm-title" onCancel={(event) => { if (action.isPending) event.preventDefault(); else setPending(undefined); }} onClose={() => { if (pending && !action.isPending) setPending(undefined); }} className="m-auto w-[calc(100%-2rem)] max-w-lg border border-line bg-white p-6 text-ink shadow-panel backdrop:bg-ink/40">
        {pending ? <><p className="eyebrow">XÁC NHẬN THAO TÁC</p><h2 id="catalog-confirm-title" className="mt-2 text-xl font-semibold">{pending.type === "archive" ? "Lưu trữ địa điểm?" : pending.type === "restore" ? "Khôi phục hồ sơ?" : "Duyệt địa điểm?"}</h2><p className="mt-3 text-sm text-muted">{pending.type === "archive" ? <><strong>{pending.place.name}</strong> sẽ bị ẩn khỏi khám phá và kho RAG. Tham chiếu trong chuyến đi hiện có vẫn được giữ lại.</> : pending.type === "restore" ? <><strong>{pending.place.name}</strong> sẽ trở lại danh sách ở trạng thái chờ duyệt.</> : <>Công khai <strong>{pending.place.name}</strong> cho người dùng và đưa hồ sơ đủ điều kiện vào kho RAG.</>}</p>{action.error ? <div className="mt-4"><Banner error>{errorMessage(action.error)}</Banner></div> : null}<div className="mt-6 flex justify-end gap-2"><Button type="button" variant="secondary" disabled={action.isPending} onClick={() => setPending(undefined)}>Hủy</Button><Button type="button" variant={pending.type === "archive" ? "danger" : "primary"} busy={action.isPending} onClick={() => void performPending()}>{pending.type === "archive" ? "Lưu trữ" : pending.type === "restore" ? "Khôi phục" : "Duyệt"}</Button></div></> : null}
      </dialog>
    </div>
  );
}

function qualityLabel(value: string) {
  const dictionary: Record<string, string> = { VERIFIED: "Đã xác minh", APPROXIMATE: "Ước lượng", APPROXIMATE_DEMO: "Tọa độ demo", UNKNOWN: "Chưa rõ", ESTIMATED: "Ước lượng" };
  return dictionary[value] ?? labels[value] ?? value;
}
