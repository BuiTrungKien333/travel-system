"use client";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { z } from "zod";
import { apiRequest } from "@/lib/client";
import { ApiError, errorMessage } from "@/lib/errors";
import { envelope } from "@/lib/contracts";
import { useSession } from "@/features/auth/api";
import { Banner, Button } from "@/components/ui";
import { Bars, ChartCard } from "./charts";
const count = z.number().nonnegative();
const stage = z.object({
  stage: z.string(),
  source: z.string(),
  method: z.string().nullish(),
  model: z.string().nullish(),
  attempts: count,
  retries: count,
  elapsedMillis: count,
  reason: z.string().nullish(),
});
export const telemetrySchema = z
  .object({
    status: z.enum(["AVAILABLE", "UNAVAILABLE", "DISABLED"]),
    asOf: z.string().datetime(),
    days: count,
    page: count,
    size: count,
    retentionDays: count,
    sync: z.object({
      pending: count,
      retried: count,
      oldestPending: z.string().datetime().nullish(),
    }),
    data: z
      .object({
        total: count,
        p50Ms: count.nullish(),
        p95Ms: count.nullish(),
        statuses: z.array(z.object({ name: z.string(), value: count })),
        runs: z.array(
          z.object({
            id: z.string(),
            status: z.string(),
            mode: z.string().nullish(),
            startedAt: z.string().datetime(),
            durationMs: count,
            stages: z.array(stage),
            failureCode: z.string().nullish(),
          }),
        ),
      })
      .nullish(),
  })
  .refine(
    (v) => v.status !== "AVAILABLE" || !!v.data,
    "Available telemetry must contain data",
  );
export function useTelemetry(days: number, page = 0) {
  const session = useSession();
  return useQuery({
    queryKey: ["ai-telemetry", session.data?.id, days, page],
    enabled: !!session.data?.permissions.includes("admin:system:read"),
    staleTime: 10000,
    refetchInterval: 30000,
    queryFn: async ({ signal }) => {
      const parsed = envelope(telemetrySchema).safeParse(
        await apiRequest(`/telemetry?days=${days}&page=${page}`, { signal }),
      );
      if (!parsed.success) throw new ApiError("INVALID_RESPONSE", 502);
      return parsed.data.data;
    },
  });
}
export function TelemetryPanel({
  days = 30,
  detailed = false,
}: {
  days?: number;
  detailed?: boolean;
}) {
  const [page, setPage] = useState(0);
  const q = useTelemetry(days, page);
  const session = useSession();
  if (!session.data?.permissions.includes("admin:system:read")) return null;
  const data = q.isError ? undefined : q.data?.data;
  const seconds = (v: number | null | undefined) =>
    v == null ? "—" : `${(v / 1000).toFixed(1)} s`;
  return (
    <ChartCard
      title={detailed ? "Nhật ký thực thi AI" : "Kết quả tạo nháp AI"}
      note={`Dữ liệu thật từ MongoDB · Lượt tạo nháp chuyến đi trong ${days} ngày. Retry cùng yêu cầu không tạo lượt mới.`}
    >
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <span className="badge bg-canvas text-muted ring-line">
          {q.isPending
            ? "Đang kết nối"
            : q.isError
              ? "Chưa đọc được telemetry"
              : q.data?.status === "AVAILABLE"
                ? "MongoDB kết nối"
                : q.data?.status === "DISABLED"
                  ? "MongoDB chưa bật"
                  : "MongoDB không khả dụng"}
        </span>
        <Button
          variant="secondary"
          busy={q.isFetching}
          onClick={() => void q.refetch()}
        >
          Tải telemetry
        </Button>
      </div>
      {q.isError ? <Banner error>{errorMessage(q.error)}</Banner> : null}
      {q.data?.status === "UNAVAILABLE" ? (
        <Banner error>
          Chưa đọc được MongoDB. Kết quả tạo lịch trình vẫn được lưu ở
          PostgreSQL; outbox chờ gửi lại. Không hiển thị số liệu mẫu thay cho
          lỗi kết nối.
        </Banner>
      ) : q.data?.status === "DISABLED" ? (
        <Banner>
          Chưa bật MongoDB trong cấu hình backend. Các sự kiện mới vẫn được giữ
          trong outbox.
        </Banner>
      ) : null}
      {q.data ? (
        <p className="my-3 text-xs leading-5 text-muted">
          Chờ đồng bộ: {q.data.sync.pending} · Đã phải thử lại:{" "}
          {q.data.sync.retried} · Lưu chi tiết {q.data.retentionDays} ngày
          {q.data.sync.oldestPending
            ? ` · Sự kiện chờ lâu nhất: ${new Date(q.data.sync.oldestPending).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" })}`
            : ""}
        </p>
      ) : null}
      {data ? (
        <>
          <div className="mb-5 grid grid-cols-3 gap-3">
            {[
              ["Lượt đã ghi nhận", data.total],
              ["Trung vị", seconds(data.p50Ms)],
              ["p95 xấp xỉ", seconds(data.p95Ms)],
            ].map(([name, value]) => (
              <div key={name} className="rounded-xl bg-canvas p-3">
                <p className="text-[10px] text-muted">{name}</p>
                <p className="mt-2 text-lg font-semibold">{value}</p>
              </div>
            ))}
          </div>
          {data.total ? (
            <Bars data={data.statuses} />
          ) : (
            <p className="py-12 text-center text-sm text-muted">
              Chưa có lượt thực thi được đồng bộ trong kỳ này. Tạo bản nháp AI
              mới để bắt đầu ghi nhận.
            </p>
          )}
          {detailed ? (
            <div className="mt-5 space-y-3">
              {data.runs.map((run) => (
                <details
                  key={run.id}
                  className="rounded-xl border border-line p-4"
                >
                  <summary className="cursor-pointer text-sm">
                    <strong>{run.status}</strong> ·{" "}
                    {new Date(run.startedAt).toLocaleString("vi-VN", {
                      timeZone: "Asia/Ho_Chi_Minh",
                    })}{" "}
                    · {seconds(run.durationMs)}
                  </summary>
                  <p className="my-3 break-all text-xs text-muted">
                    Run ID: {run.id} · {run.mode}{" "}
                    {run.failureCode ? `· ${run.failureCode}` : ""}
                  </p>
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs">
                      <thead>
                        <tr>
                          {[
                            "Bước",
                            "Nguồn",
                            "Model",
                            "Lượt / retry",
                            "Thời gian",
                            "Mã nguyên nhân",
                          ].map((v) => (
                            <th key={v} className="p-2">
                              {v}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {run.stages.map((s) => (
                          <tr key={s.stage} className="border-t border-line">
                            <td className="p-2">{s.stage}</td>
                            <td className="p-2">{s.source}</td>
                            <td className="p-2">{s.model ?? "—"}</td>
                            <td className="p-2">
                              {s.attempts} / {s.retries}
                            </td>
                            <td className="p-2">{seconds(s.elapsedMillis)}</td>
                            <td className="p-2">{s.reason ?? "—"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </details>
              ))}
              <div className="flex justify-end gap-3">
                <Button
                  variant="secondary"
                  disabled={!page || q.isFetching}
                  onClick={() => setPage(page - 1)}
                >
                  Trang trước
                </Button>
                <span className="self-center text-xs text-muted">
                  {page + 1}
                </span>
                <Button
                  variant="secondary"
                  disabled={(page + 1) * 20 >= data.total || q.isFetching}
                  onClick={() => setPage(page + 1)}
                >
                  Trang sau
                </Button>
              </div>
            </div>
          ) : null}
        </>
      ) : null}
      <p className="mt-4 text-[11px] leading-5 text-muted">
        Chỉ ghi nhận từ khi cài outbox; không sao chép lịch sử cũ. Dữ liệu
        MongoDB đồng bộ bất đồng bộ; khi có lỗi, số liệu chậm hơn do retry. Thời lượng tính toàn
        luồng; p95 dùng ước lượng MongoDB.
      </p>
    </ChartCard>
  );
}
