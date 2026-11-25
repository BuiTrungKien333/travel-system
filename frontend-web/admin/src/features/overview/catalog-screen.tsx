"use client";
import { useState, type FormEvent } from "react";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import { Banner, Button } from "@/components/ui";
import { useSession } from "@/features/auth/api";
import { errorMessage } from "@/lib/errors";
import { useKnowledge, useCatalogAction, useRagSearch, useCatalogProvinces } from "./hooks";
import { labels, type Entry } from "./data";

const retrievalTags = [
  "MUSEUM",
  "HISTORY",
  "CULTURE",
  "ART",
  "LOCAL",
  "MARKET",
  "SHOPPING",
  "BEACH",
  "SCENERY",
  "RELAX",
  "OUTDOOR",
  "PARK",
  "ARCHITECTURE",
  "TEMPLE",
];

export function CatalogScreen({ knowledge = false }: { knowledge?: boolean }) {
  const session = useSession();
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Entry | "index">();
  const [ragQuery, setRagQuery] = useState("");
  const [excludedTags, setExcludedTags] = useState<string[]>([]);
  const [indexSuccessDismissed, setIndexSuccessDismissed] = useState(false);
  const [knowledgeFilters, setKnowledgeFilters] = useState({ provinceCode: "", indexStatus: "" });
  const allowed = !!session.data?.permissions.includes("admin:catalog");
  const canTestRag = !!session.data?.permissions.includes("admin:ai:index");
  const q = useKnowledge(session.data?.id, page, allowed, knowledgeFilters);
  const provinces = useCatalogProvinces(session.data?.id, knowledge && allowed);
  const totalPages = Math.max(1, Math.ceil((q.data?.totalElements ?? 0) / 20));
  const action = useCatalogAction();
  const rag = useRagSearch();
  function searchRag(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!ragQuery.trim() || rag.isPending) return;
    rag.mutate({
      positiveQuery: ragQuery.trim(),
      provinceCode: knowledgeFilters.provinceCode || undefined,
      excludedTags,
      limit: 8,
    });
  }
  if (!allowed) return <Banner error>Chưa có quyền quản lý catalog.</Banner>;
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="eyebrow">
            {knowledge
              ? "AI · RETRIEVAL AUGMENTED GENERATION"
              : "CATALOG · CHẤT LƯỢNG DỮ LIỆU"}
          </p>
          <h1 className="mt-2 text-3xl font-semibold">
            {knowledge ? "Quản lý kho tri thức AI" : "Quản lý địa điểm"}
          </h1>
          <p className="mt-3 text-sm text-muted">
            {knowledge
              ? "Nguồn tri thức, phạm vi xác minh và embedding hiện hành."
              : "Danh sách hồ sơ địa điểm, tỉnh/thành, tags và duyệt công khai."}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => void q.refetch()}>
            Tải lại
          </Button>
          {knowledge && session.data?.permissions.includes("admin:ai:index") ? (
            <Button
              disabled={action.isPending}
              onClick={() => {
                action.reset();
                setIndexSuccessDismissed(false);
                setSelected("index");
              }}
            >
              {action.isPending ? "Đang cập nhật index…" : "Cập nhật index"}
            </Button>
          ) : null}
        </div>
      </div>
      {selected ? (
        <section
          aria-label="Xác nhận thao tác catalog"
          className="rounded-xl border border-amber-200 bg-amber-50 p-5"
        >
          <h2 className="font-semibold">
            {selected === "index"
              ? "Xác nhận cập nhật index AI"
              : `${selected.approved ? "Gỡ công khai" : "Duyệt công khai"}: ${selected.name}`}
          </h2>
          <p className="my-3 text-sm">
            {selected === "index"
              ? "Thao tác gọi dịch vụ embedding và có thể phát sinh chi phí. Các embedding hiện hành được bỏ qua."
              : "Thay đổi được ghi lịch sử; dữ liệu riêng trong các chuyến đi vẫn được giữ."}
          </p>
          <div className="flex gap-3">
            <Button
              busy={action.isPending}
              onClick={() =>
                action.mutate(
                  selected === "index"
                    ? "index"
                    : {
                        id: selected.id,
                        approved: !selected.approved,
                        version: selected.version,
                      },
                  { onSuccess: () => setSelected(undefined) },
                )
              }
            >
              {action.isPending ? "Đang xử lý…" : "Xác nhận"}
            </Button>
            <Button
              variant="secondary"
              disabled={action.isPending}
              onClick={() => setSelected(undefined)}
            >
              Hủy
            </Button>
          </div>
        </section>
      ) : null}
      {action.isPending ? (
        <Banner>
          <p role="status" aria-live="polite" className="font-medium">
            Đang cập nhật kho embedding. Hệ thống xử lý các địa điểm đủ điều
            kiện; số lượng cập nhật, bỏ qua và lỗi sẽ hiển thị khi hoàn tất.
          </p>
        </Banner>
      ) : null}
      {action.isError ? (
        <Banner error>
          <p role="alert">
            Cập nhật index không hoàn tất: {errorMessage(action.error)} Đóng xác
            nhận và tải lại dữ liệu nếu phiên bản đã thay đổi.
          </p>
        </Banner>
      ) : null}
      {action.isSuccess && action.data && !indexSuccessDismissed ? (
        action.data.failed > 0 ? (
          <Banner error>
            <p role="status" aria-live="polite" className="font-medium">
              Index hoàn tất nhưng còn địa điểm lỗi: cập nhật {action.data.updated},
              bỏ qua {action.data.skipped}, lỗi {action.data.failed} · Model {action.data.model}.
            </p>
          </Banner>
        ) : (
          <div className="flex items-start justify-between gap-4 border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
            <p role="status" aria-live="polite" className="font-medium">
              Đã cập nhật index thành công: cập nhật {action.data.updated}, giữ
              nguyên {action.data.skipped}, lỗi {action.data.failed} · Model {action.data.model}.
            </p>
            <button
              type="button"
              aria-label="Đóng thông báo cập nhật index thành công"
              title="Đóng thông báo"
              onClick={() => setIndexSuccessDismissed(true)}
              className="grid size-8 shrink-0 place-items-center text-green-800 hover:bg-green-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-green-800"
            >
              <X size={17} aria-hidden="true" />
            </button>
          </div>
        )
      ) : action.isSuccess ? (
        <Banner>
          <p role="status" aria-live="polite">Đã cập nhật trạng thái công khai.</p>
        </Banner>
      ) : null}
      {q.isPending ? <p role="status">Đang tải catalog…</p> : null}
      {q.isError ? <Banner error>{errorMessage(q.error)}</Banner> : null}
      {knowledge && provinces.isError ? <Banner error>{errorMessage(provinces.error)}</Banner> : null}
      {q.data ? (
        <>
          <Banner>
            {knowledge
              ? `Model: ${q.data.model} · ${q.data.indexAvailable ? "Có kho embedding" : "Chưa bật kho embedding"} · Index toàn quốc cho mọi địa điểm nội bộ đã duyệt, chưa lưu trữ.`
              : "Duyệt sẽ công khai hồ sơ. Hãy kiểm tra nguồn tham khảo, tọa độ và giờ hoạt động trước khi xác nhận."}
          </Banner>
          {knowledge ? (
            <section aria-label="Lọc kho tri thức AI" className="grid gap-3 border-y border-line py-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <label htmlFor="knowledge-province" className="label">Tỉnh/thành</label>
                <select id="knowledge-province" className="input w-full" value={knowledgeFilters.provinceCode} onChange={(event) => { setKnowledgeFilters((current) => ({ ...current, provinceCode: event.target.value })); setPage(0); }}>
                  <option value="">Tất cả tỉnh/thành</option>
                  {(provinces.data ?? []).map((province) => <option key={province.code} value={province.code}>{province.name}</option>)}
                </select>
              </div>
              <div className="space-y-1.5">
                <label htmlFor="knowledge-status" className="label">Trạng thái embedding</label>
                <select id="knowledge-status" className="input w-full" value={knowledgeFilters.indexStatus} onChange={(event) => { setKnowledgeFilters((current) => ({ ...current, indexStatus: event.target.value })); setPage(0); }}>
                  <option value="">Tất cả trạng thái</option>
                  <option value="CURRENT">Hiện hành</option><option value="STALE">Cần cập nhật</option><option value="MISSING">Chưa index</option><option value="OUT_OF_SCOPE">Ngoài phạm vi RAG</option>
                </select>
              </div>
              <p className="text-xs text-muted sm:col-span-2">Mọi địa điểm nội bộ đã duyệt và chưa lưu trữ đều có thể được index, kể cả dữ liệu demo.</p>
            </section>
          ) : null}
          {knowledge && canTestRag ? (
            <section
              className="space-y-4 rounded-2xl border border-line bg-white p-5 sm:p-6"
              aria-labelledby="rag-test-title"
            >
              <div>
                <p className="eyebrow">PLAYGROUND · HYBRID SEARCH</p>
                <h2 id="rag-test-title" className="mt-1 text-xl font-semibold">
                  Thử truy vấn RAG
                </h2>
                <p className="mt-2 max-w-3xl text-sm text-muted">
                  Nhập truy vấn RAG thuần để so sánh tìm kiếm từ khóa với
                  embedding Gemini; bước phân tích ý định không chạy ở đây. Mỗi
                  lần tìm tạo embedding câu hỏi và có thể dùng quota Gemini. Kết
                  quả chỉ gồm địa điểm nội bộ đã duyệt, chưa lưu trữ và không
                  phải dữ liệu demo. Bộ lọc tỉnh/thành phía trên cũng áp dụng
                  cho lần truy vấn này; để trống tỉnh sẽ tìm trên toàn quốc.
                </p>
              </div>
              <form onSubmit={searchRag} className="space-y-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                  <div className="min-w-0 flex-1 space-y-1.5">
                    <label htmlFor="rag-query" className="text-sm font-medium">
                      Nhu cầu / cụm tìm kiếm
                    </label>
                    <input
                      id="rag-query"
                      name="positiveQuery"
                      type="search"
                      required
                      maxLength={1000}
                      value={ragQuery}
                      onChange={(event) => setRagQuery(event.target.value)}
                      placeholder="Ví dụ: nơi tìm hiểu lịch sử và văn hóa Chăm"
                      className="input min-h-12 w-full text-base"
                    />
                  </div>
                  <Button
                    type="submit"
                    busy={rag.isPending}
                    disabled={!ragQuery.trim()}
                  >
                    Tìm địa điểm
                  </Button>
                  {ragQuery || excludedTags.length || rag.data || rag.error ? (
                    <button
                      type="button"
                      aria-label="Đặt lại truy vấn RAG"
                      title="Đặt lại tìm kiếm"
                      disabled={rag.isPending}
                      onClick={() => {
                        setRagQuery("");
                        setExcludedTags([]);
                        rag.reset();
                      }}
                      className="grid size-12 shrink-0 place-items-center rounded-lg border border-line text-muted transition-colors hover:bg-canvas hover:text-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <X size={18} aria-hidden="true" />
                    </button>
                  ) : null}
                </div>
                <details className="rounded-xl border border-line bg-canvas px-4 py-3">
                  <summary className="cursor-pointer text-sm font-medium">
                    Loại trừ tag (tùy chọn)
                  </summary>
                  <fieldset className="mt-3 space-y-2">
                    <legend className="text-sm text-muted">
                      Không đưa các nhóm trải nghiệm này vào kết quả
                    </legend>
                    <div className="flex flex-wrap gap-2">
                      {retrievalTags.map((tag) => (
                        <label
                          key={tag}
                          className="inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-full border border-line bg-white px-3 text-xs"
                        >
                          <input
                            type="checkbox"
                            name={`excluded-${tag}`}
                            value={tag}
                            checked={excludedTags.includes(tag)}
                            disabled={
                              !excludedTags.includes(tag) &&
                              excludedTags.length >= 10
                            }
                            onChange={(event) =>
                              setExcludedTags((current) =>
                                event.target.checked
                                  ? [...current, tag]
                                  : current.filter((item) => item !== tag),
                              )
                            }
                          />
                          {tag}
                        </label>
                      ))}
                    </div>
                  </fieldset>
                </details>
              </form>
              {rag.isPending ? (
                <p role="status" className="text-sm text-muted">
                  Đang tạo embedding câu hỏi và xếp hạng địa điểm…
                </p>
              ) : null}
              {rag.isError ? (
                <Banner error>{errorMessage(rag.error)}</Banner>
              ) : null}
              {rag.data ? (
                <div className="space-y-4" aria-live="polite">
                  <div className="flex flex-wrap items-center gap-2 rounded-xl bg-canvas p-3 text-sm">
                    <span className="badge bg-teal-50 text-teal-700 ring-teal-100">
                      {retrievalModeLabel(rag.data.retrievalMode)}
                    </span>
                    <span className="text-muted">
                      Model: {rag.data.embeddingModel}
                    </span>
                    {rag.data.fallbackReason ? (
                      <span className="text-amber-800">
                        Fallback: {rag.data.fallbackReason}
                      </span>
                    ) : null}
                  </div>
                  {rag.data.matches.length ? (
                    <ol className="grid gap-3 lg:grid-cols-2">
                      {rag.data.matches.map((match, index) => (
                        <li
                          key={match.place.placeId}
                          className="rounded-xl border border-line p-4"
                        >
                          <div className="flex items-start justify-between gap-3">
                            <div>
                              <p className="font-semibold">
                                <span className="mr-2 text-muted">
                                  {index + 1}.
                                </span>
                                {match.place.name}
                              </p>
                              <p className="mt-1 text-xs text-muted">
                                Điểm xếp hạng: {match.rankScore.toFixed(4)}
                              </p>
                            </div>
                            <div className="flex shrink-0 flex-wrap justify-end gap-1">
                              {match.lexicalMatch ? (
                                <span className="badge bg-slate-50 text-slate-700 ring-slate-200">
                                  Từ khóa
                                </span>
                              ) : null}
                              {match.semanticMatch ? (
                                <span className="badge bg-teal-50 text-teal-700 ring-teal-100">
                                  Ngữ nghĩa
                                </span>
                              ) : null}
                            </div>
                          </div>
                          <p className="my-3 text-sm text-muted">
                            {match.place.description}
                          </p>
                          <div className="flex flex-wrap gap-1">
                            {match.place.tags.map((tag) => (
                              <span
                                key={tag}
                                className="badge bg-canvas text-muted ring-line"
                              >
                                {tag}
                              </span>
                            ))}
                          </div>
                        </li>
                      ))}
                    </ol>
                  ) : (
                    <p className="rounded-xl border border-dashed border-line p-6 text-center text-sm text-muted">
                      Không tìm thấy địa điểm phù hợp trong kho tri thức hiện
                      tại.
                    </p>
                  )}
                </div>
              ) : null}
            </section>
          ) : null}
          <section className="overflow-hidden rounded-2xl border border-line bg-white">
            <div className="border-b border-line p-5 text-sm font-semibold">
              {q.data.totalElements.toLocaleString("vi-VN")} hồ sơ
            </div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px] text-left text-sm">
                <thead className="bg-canvas text-xs text-muted">
                  <tr>
                    {[
                      "Địa điểm / nguồn",
                      "Tags",
                      knowledge ? "Embedding" : "Công khai",
                      "Thao tác",
                    ].map((n) => (
                      <th key={n} className="px-5 py-4">
                        {n}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {q.data.content.map((p) => (
                    <tr key={p.id} className="border-t border-line">
                      <td className="max-w-sm px-5 py-5">
                        <p className="font-semibold">{p.name}</p>
                        <p className="my-2 text-xs text-muted">
                          {p.province ?? "Chưa gắn tỉnh/thành"}
                        </p>
                        <p className="break-all text-xs text-muted">
                          {p.sourceUrl}
                        </p>
                        {knowledge ? (
                          <p className="mt-2 text-xs text-muted">
                            {p.verificationScope}
                          </p>
                        ) : null}
                      </td>
                      <td className="max-w-xs px-5 py-5">
                        <div className="flex flex-wrap gap-1">
                          {p.tags.map((t) => (
                            <span
                              key={t}
                              className="badge bg-canvas text-muted ring-line"
                            >
                              {t}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="px-5 py-5">
                        <span className="badge bg-teal-50 text-teal-700 ring-teal-100">
                          {knowledge
                            ? (labels[p.indexStatus] ?? p.indexStatus)
                            : p.approved
                              ? "Đã duyệt"
                              : "Chờ duyệt"}
                        </span>
                      </td>
                      <td className="px-5 py-5">
                        <Button
                          variant="secondary"
                          disabled={action.isPending}
                          onClick={() => {
                            action.reset();
                            setSelected(p);
                          }}
                        >
                          {p.approved ? "Gỡ công khai" : "Duyệt công khai"}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {!q.data.content.length ? (
              <p className="p-10 text-center text-muted">
                Chưa có hồ sơ địa điểm. Không tạo dữ liệu mẫu có thể bị duyệt
                nhầm.
              </p>
            ) : null}
            <div className="flex flex-wrap items-center justify-between gap-4 border-t border-line px-5 py-4 text-xs text-muted sm:px-6">
              <p>
                {q.data.totalElements === 0
                  ? "0 hồ sơ"
                  : `${page * 20 + 1}–${Math.min((page + 1) * 20, q.data.totalElements)} / ${q.data.totalElements.toLocaleString("vi-VN")} hồ sơ`}
              </p>
              <nav
                aria-label={`Phân trang ${knowledge ? "kho tri thức AI" : "địa điểm"}`}
                className="flex items-center gap-1"
              >
              <Button
                variant="secondary"
                aria-label="Trang trước"
                disabled={page === 0 || q.isFetching}
                onClick={() => setPage(page - 1)}
              >
                  <ChevronLeft size={15} />
              </Button>
                {paginationPages(page, totalPages).map((pageNumber, index) =>
                  pageNumber === "ellipsis" ? (
                    <span key={`ellipsis-${index}`} className="px-1" aria-hidden="true">
                      …
                    </span>
                  ) : (
                    <Button
                      key={pageNumber}
                      variant={pageNumber === page ? "primary" : "secondary"}
                      aria-label={`Trang ${pageNumber + 1}`}
                      aria-current={pageNumber === page ? "page" : undefined}
                      disabled={q.isFetching}
                      onClick={() => setPage(pageNumber)}
                    >
                      {pageNumber + 1}
                    </Button>
                  ),
                )}
                <Button
                  variant="secondary"
                  aria-label="Trang sau"
                  disabled={page + 1 >= totalPages || q.isFetching}
                  onClick={() => setPage(page + 1)}
                >
                  <ChevronRight size={15} />
                </Button>
              </nav>
            </div>
          </section>
        </>
      ) : null}

    </div>
  );
}

function paginationPages(current: number, total: number) {
  const visible = [...new Set([0, total - 1, current - 1, current, current + 1])]
    .filter((page) => page >= 0 && page < total)
    .sort((a, b) => a - b);
  return visible.flatMap((page, index) => [
    ...(index > 0 && page - visible[index - 1] > 1 ? (["ellipsis"] as const) : []),
    page,
  ]);
}

function retrievalModeLabel(mode: string) {
  if (mode === "HYBRID") return "Kết hợp từ khóa + ngữ nghĩa";
  if (mode === "LEXICAL_ONLY_NO_CURRENT_EMBEDDINGS")
    return "Chỉ từ khóa · chưa có embedding hiện hành";
  if (mode === "LEXICAL_FALLBACK_EMBEDDING_UNAVAILABLE")
    return "Dự phòng từ khóa · embedding không khả dụng";
  return mode;
}
