"use client";
import { useState } from "react";
import Link from "next/link";
import { Users, MapPin, Route, Database, Clock, RefreshCw, CheckCircle2 } from "lucide-react";
import { Banner, Button } from "@/components/ui";
import { useSession } from "@/features/auth/api";
import { errorMessage } from "@/lib/errors";
import { useIntegrationHealth, useOverview, useTriggerIntegrationHealthCheck } from "./hooks";
import { samples, type Point } from "./data";
import { TelemetryPanel } from "./telemetry";
import {
  ChartCard,
  Bars,
  TopPlacesBars,
  TopProvincesBars,
  Donut,
  Trend,
} from "./charts";
export function Dashboard() {
  const session = useSession();
  const [days, setDays] = useState(30);
  const allowed = !!session.data?.permissions.includes("admin:system:read");
  const q = useOverview(session.data?.id, days, allowed);
  const integrationHealth = useIntegrationHealth(session.data?.id, allowed);
  const triggerHealthCheck = useTriggerIntegrationHealthCheck(session.data?.id);
  if (!allowed)
    return <Banner error>Chưa có quyền xem thống kê hệ thống.</Banner>;
  const buildTopProvinces = (real: Point[], fallback: Point[]) => {
    if (!real.length) return fallback.slice(0, 5);
    const existing = new Set(real.map((p) => p.name.toLowerCase()));
    const top = [...real].sort((a, b) => b.value - a.value).slice(0, 5);
    if (top.length >= 5) return top;
    const anchor = Math.max(top[top.length - 1]?.value ?? 1, 1);
    const ratios = [0.8, 0.6, 0.45, 0.3];
    const fillers = fallback
      .filter((p) => !existing.has(p.name.toLowerCase()))
      .slice(0, 5 - top.length)
      .map((p, i) => ({
        name: p.name,
        value: Math.max(1, Math.round(anchor * (ratios[i] ?? 0.3))),
      }));
    return [...top, ...fillers];
  };
  const pick = (real: Point[], fallback: Point[]) => ({
    data: real.length ? real : fallback,
    sample: !real.length,
  });
  if (q.isPending)
    return (
      <div role="status" className="animate-pulse rounded-2xl bg-white p-12">
        Đang tổng hợp số liệu…
      </div>
    );
  if (q.isError)
    return (
      <div className="space-y-4">
        <Banner error>{errorMessage(q.error)}</Banner>
        <Button onClick={() => void q.refetch()}>Thử lại</Button>
      </div>
    );
  const d = q.data;
  const cards = [
    {
      title: "Tổng chuyến đi",
      value: d.trips,
      note: "Hiện tại · chưa bị xóa",
      icon: Route,
    },
    {
      title: "Tổng địa điểm",
      value: d.places,
      note: "Hồ sơ nội bộ đang hoạt động",
      icon: MapPin,
    },
    {
      title: "Tổng lượt check-in",
      value: d.checkIns,
      note: "Điểm dừng đã ghé thăm",
      icon: CheckCircle2,
    },
    {
      title: "Tổng tài khoản",
      value: d.users,
      note: "Hiện tại · chưa bị xóa",
      icon: Users,
    },
    {
      title: "Đăng ký mới",
      value: d.newUsers,
      note: `Trong ${days} ngày`,
      icon: Users,
    },
    {
      title: "Chuyến đi mới",
      value: d.newTrips,
      note: `Trong ${days} ngày`,
      icon: Route,
    },
    {
      title: "Yêu cầu tạo nháp AI",
      value: d.drafts,
      note: `Trong ${days} ngày · gồm cả yêu cầu chưa hoàn tất`,
      icon: Database,
    },
    {
      title: "Chờ duyệt",
      value: d.pendingPlaces,
      note: "Hồ sơ địa điểm hiện tại",
      icon: MapPin,
    },
  ];
  return (
    <div className="space-y-7">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="eyebrow">WEGO · TỔNG QUAN VẬN HÀNH</p>
          <h1 className="mt-2 text-3xl font-semibold">Dashboard</h1>
          <p className="mt-2 text-sm text-muted">
            Mức sử dụng, chất lượng dữ liệu và khả năng phục vụ AI.
          </p>
        </div>
        <div className="flex gap-2">
          <label className="sr-only" htmlFor="days">
            Khoảng thời gian
          </label>
          <select
            id="days"
            className="input min-h-10 py-2"
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
          >
            {[7, 30, 90].map((n) => (
              <option key={n} value={n}>
                {n} ngày qua
              </option>
            ))}
          </select>
          <Button
            variant="secondary"
            busy={q.isFetching}
            onClick={() => void q.refetch()}
            aria-label="Tải lại thống kê"
          >
            <RefreshCw size={16} />
          </Button>
        </div>
      </div>
      <p className="flex items-center gap-2 text-xs text-muted">
        <Clock size={13} />
        Chốt số liệu:{" "}
        {new Date(d.asOf).toLocaleString("vi-VN", {
          timeZone: "Asia/Ho_Chi_Minh",
        })}{" "}
        · Múi giờ Việt Nam
      </p>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map((c) => (
          <div
            key={c.title}
            className="rounded-2xl border border-line bg-white p-5 shadow-panel"
          >
            <div className="flex items-center justify-between text-xs text-muted">
              {c.title}
              <c.icon size={17} />
            </div>
            <p className="my-3 text-3xl font-semibold tracking-tight">
              {(c.value ?? 0).toLocaleString("vi-VN")}
            </p>
            <p className="text-xs text-muted">{c.note}</p>
          </div>
        ))}
      </div>
      <div className="grid gap-5 xl:grid-cols-[1.5fr_1fr]">
        <ChartCard
          title="Tăng trưởng sử dụng"
          note="Tài khoản và chuyến đi mới theo ngày; số 0 là dữ liệu thật."
        >
          <Trend data={d.trend} />
        </ChartCard>
        <ChartCard
          title="Trạng thái chuyến đi"
          note="Trạng thái đã lưu, toàn bộ chuyến chưa xóa tại thời điểm thống kê."
          sample={!d.tripStatuses.length}
        >
          <Donut {...pick(d.tripStatuses, samples.tripStatuses)} />
        </ChartCard>
      </div>
      <div className="grid gap-5 xl:grid-cols-2">
        <ChartCard
          title="Top địa điểm"
          note={`Top 10 theo số chuyến được tạo trong ${days} ngày có thêm địa điểm. Mỗi chuyến chỉ đếm một lần.`}
          sample={!d.topPlaces.length}
        >
          <TopPlacesBars {...pick(d.topPlaces, samples.topPlaces)} />
        </ChartCard>
        <ChartCard
          title="Top 5 tỉnh / thành"
          note={`Số chuyến mới chọn tỉnh/thành làm điểm đến trong ${days} ngày.`}
          sample={d.topProvinces.length < 5}
        >
          <TopProvincesBars data={buildTopProvinces(d.topProvinces, samples.topProvinces)} />
        </ChartCard>
      </div>
      <div className="grid gap-5 xl:grid-cols-2">
        <ChartCard
          title="Phân bổ kho địa điểm theo tag"
          note="Toàn bộ hồ sơ INTERNAL. Một địa điểm có nhiều tag nên các nhóm có thể chồng lấp."
          sample={!d.tags.length}
        >
          <Bars {...pick(d.tags, samples.tags)} />
        </ChartCard>
        <ChartCard
          title="Kho tri thức AI RAG"
          note="Snapshot embedding theo model hiện hành. Hồ sơ lưu trữ không nằm trong thống kê; hồ sơ chưa được duyệt được tính là ngoài phạm vi."
          sample={!d.rag.length}
        >
          <Donut {...pick(d.rag, samples.rag)} />
        </ChartCard>
      </div>
      <div className="grid gap-5 xl:grid-cols-2">
        <TelemetryPanel days={days} />
        <IntegrationHealthPanel query={integrationHealth} trigger={triggerHealthCheck} />
      </div>
      <Banner>
        Biểu đồ không có bản ghi sẽ hiển thị ví dụ với nhãn “Dữ liệu mẫu”. Các
        thẻ chỉ số vẫn luôn là dữ liệu thật, kể cả số 0. Database dev có thể
        chứa dữ liệu seed/demo.
      </Banner>
      <div className="grid gap-4 sm:grid-cols-2">
        <Link
          href="/places"
          className="rounded-xl border border-line bg-white p-5 text-sm font-semibold text-teal-700"
        >
          Xử lý {d.pendingPlaces} hồ sơ chờ duyệt →
        </Link>
        <Link
          href="/knowledge"
          className="rounded-xl border border-line bg-white p-5 text-sm font-semibold text-teal-700"
        >
          Kiểm tra kho tri thức và cập nhật index →
        </Link>
      </div>
    </div>
  );
}

function IntegrationHealthPanel({
  query,
  trigger,
}: {
  query: ReturnType<typeof useIntegrationHealth>;
  trigger: ReturnType<typeof useTriggerIntegrationHealthCheck>;
}) {
  const statusLabels: Record<string, string> = {
    UP: "Hoạt động",
    DOWN: "Không kết nối được",
    NOT_CONFIGURED: "Chưa cấu hình",
    MOCK: "Đang dùng mô phỏng",
    NOT_CHECKED: "Đang chờ lần kiểm tra đầu",
  };
  const detailLabels: Record<string, string> = {
    INVALID_RESPONSE: "Nhà cung cấp trả phản hồi không hợp lệ",
    TIMEOUT_OR_NETWORK: "Hết thời gian chờ hoặc lỗi mạng",
    CHECK_FAILED: "Lần kiểm tra chưa hoàn tất",
    MOCK_PROVIDER_ACTIVE: "Đây không phải dữ liệu thời tiết thật",
    MODEL_METHOD_UNSUPPORTED: "Model không hỗ trợ chức năng hệ thống cần dùng",
    TIMEOUT: "Dịch vụ phản hồi quá thời gian chờ",
    DNS_ERROR: "Không phân giải được địa chỉ dịch vụ",
    CONNECTION_REFUSED: "Kết nối tới dịch vụ bị từ chối",
    NETWORK_ERROR: "Không thể kết nối tới dịch vụ",
    MONGODB_TIMEOUT: "MongoDB không phản hồi trong thời gian chờ",
    INVALID_CONFIGURATION: "Cấu hình tích hợp chưa hợp lệ",
    PROVIDER_CLIENT_ERROR: "Client gọi dịch vụ gặp lỗi",
    HTTP_401: "Thông tin xác thực bị từ chối",
    HTTP_403: "Tài khoản thiếu quyền truy cập dịch vụ",
    HTTP_429: "Nhà cung cấp đang giới hạn yêu cầu",
    HTTP_500: "Nhà cung cấp đang gặp lỗi",
  };
  return (
    <section className="min-w-0 rounded-2xl border border-line bg-white p-6 shadow-panel">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-semibold">Trạng thái tích hợp</h2>
          <p className="mt-2 text-xs leading-5 text-muted">
            Kiểm tra nền mỗi {Math.round((query.data?.intervalMillis ?? 1800000) / 60000)} phút; dashboard chỉ đọc kết quả đã lưu.
          </p>
        </div>
        <Button
          variant="secondary"
          busy={query.isFetching || trigger.isPending || !!query.data?.checking}
          disabled={!query.data}
          onClick={() => trigger.mutate()}
        >
          <RefreshCw size={16} aria-hidden="true" />
          Kiểm tra ngay
        </Button>
      </div>
      {query.isError ? <Banner error>Chưa tải được trạng thái health check. Hãy thử tải lại.</Banner> : null}
      {trigger.isError ? <Banner error>{errorMessage(trigger.error)}</Banner> : null}
      {query.data?.checking ? <p role="status" className="mt-3 text-sm font-medium text-teal-700">Đang kiểm tra các dịch vụ tích hợp… Kết quả sẽ tự cập nhật.</p> : null}
      {trigger.data && !trigger.data.started ? <p className="mt-3 text-xs text-muted">Một lần kiểm tra khác đang chạy; dashboard sẽ cập nhật khi hoàn tất.</p> : null}
      {query.isPending ? <p role="status" className="py-8 text-sm text-muted">Đang tải kết quả kiểm tra…</p> : null}
      {query.data ? (
        <ul className="mt-4 divide-y divide-line">
          {query.data.integrations.map((integration) => {
            const tone = integration.stale && integration.status === "UP"
              ? "bg-amber-50 text-amber-800 ring-amber-200"
              : integration.status === "UP"
              ? "bg-teal-50 text-teal-700 ring-teal-100"
              : integration.status === "DOWN"
                ? "bg-red-50 text-red-700 ring-red-100"
                : integration.status === "MOCK"
                  ? "bg-amber-50 text-amber-800 ring-amber-200"
                  : "bg-slate-50 text-slate-600 ring-slate-200";
            return (
              <li key={integration.key} className="flex flex-wrap items-start justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-ink">{integration.name}</p>
                  <p className="mt-1 text-xs text-muted">
                    {integration.checkedAt
                      ? `Kiểm tra lúc ${new Date(integration.checkedAt).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" })} · ${integration.durationMillis} ms`
                      : "Chưa có kết quả kiểm tra"}
                    {integration.stale ? " · Dữ liệu đã cũ" : ""}
                  </p>
                  {integration.lastSuccessAt && integration.status !== "UP" ? (
                    <p className="mt-1 text-xs text-muted">
                      Thành công gần nhất: {new Date(integration.lastSuccessAt).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" })}
                    </p>
                  ) : null}
                  {integration.detailCode ? (
                    <p className="mt-1 text-xs text-muted">
                      {detailLabels[integration.detailCode]
                        ?? (integration.detailCode.startsWith("HTTP_")
                          ? `Nhà cung cấp phản hồi HTTP ${integration.detailCode.slice(5)}`
                          : integration.detailCode.startsWith("UNEXPECTED_")
                            ? `Lỗi nội bộ khi kiểm tra (${integration.detailCode.slice(11)})`
                            : integration.detailCode === "CHECK_FAILED"
                              ? "Lỗi chưa phân loại từ lần kiểm tra cũ; hãy chạy kiểm tra lại."
                              : `Mã lỗi: ${integration.detailCode}`)}
                    </p>
                  ) : null}
                </div>
                <span className={`badge shrink-0 ${tone}`}>
                  {integration.stale && integration.status !== "NOT_CHECKED" ? "Cũ · " : ""}{statusLabels[integration.status] ?? integration.status}
                </span>
              </li>
            );
          })}
        </ul>
      ) : null}
    </section>
  );
}
