"use client";
import { useState } from "react";
import { Banner, Button } from "@/components/ui";
import { useSession } from "@/features/auth/api";
import { errorMessage } from "@/lib/errors";
import { useKnowledge, useCatalogAction } from "./hooks";
import { labels, type Entry } from "./data";
export function CatalogScreen({ knowledge = false }: { knowledge?: boolean }) {
  const session = useSession();
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Entry | "index">();
  const allowed = !!session.data?.permissions.includes("admin:catalog");
  const q = useKnowledge(session.data?.id, page, allowed);
  const action = useCatalogAction();
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
                setSelected("index");
              }}
            >
              Cập nhật index
            </Button>
          ) : null}
        </div>
      </div>
      {q.isPending ? <p role="status">Đang tải catalog…</p> : null}
      {q.isError ? <Banner error>{errorMessage(q.error)}</Banner> : null}
      {q.data ? (
        <>
          <Banner>
            {knowledge
              ? `Model: ${q.data.model} · ${q.data.indexAvailable ? "Có kho embedding" : "Chưa bật kho embedding"} · Chỉ index hồ sơ đã duyệt trong phạm vi RAG.`
              : "Duyệt công khai không đồng nghĩa đã xác minh đầy đủ tọa độ/giờ mở cửa. Hồ sơ demo chưa xác minh không thể được duyệt."}
          </Banner>
          <section className="overflow-hidden rounded-2xl border border-line bg-white">
            <div className="border-b border-line p-5 text-sm font-semibold">
              {q.data.totalElements} hồ sơ · Trang {page + 1}
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
                          disabled={
                            action.isPending ||
                            (!p.approved &&
                              p.verificationScope ===
                                "DEV_DEMO_ONLY_NOT_FACT_VERIFIED")
                          }
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
            <div className="flex justify-end gap-3 border-t border-line p-4">
              <Button
                variant="secondary"
                disabled={!page || q.isFetching}
                onClick={() => setPage(page - 1)}
              >
                Trang trước
              </Button>
              <Button
                variant="secondary"
                disabled={
                  (page + 1) * 20 >= q.data.totalElements || q.isFetching
                }
                onClick={() => setPage(page + 1)}
              >
                Trang sau
              </Button>
            </div>
          </section>
        </>
      ) : null}
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
              Xác nhận
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
      {action.isError ? (
        <Banner error>
          {errorMessage(action.error)} Đóng xác nhận và tải lại dữ liệu nếu
          phiên bản đã thay đổi.
        </Banner>
      ) : null}
      {action.isSuccess ? (
        <Banner>
          {action.data
            ? `Index hoàn tất: cập nhật ${action.data.updated}, giữ nguyên ${action.data.skipped}, lỗi ${action.data.failed}.`
            : "Đã cập nhật trạng thái công khai."}
        </Banner>
      ) : null}
    </div>
  );
}
