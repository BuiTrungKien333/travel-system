"use client";
import { useState } from "react";
import Link from "next/link";
import { Users, MapPin, Route, Database, Clock, RefreshCw } from "lucide-react";
import { Banner, Button } from "@/components/ui";
import { useSession } from "@/features/auth/api";
import { errorMessage } from "@/lib/errors";
import { useOverview } from "./hooks";
import { samples, type Point } from "./data";
import { ChartCard, Bars, Donut, Trend } from "./charts";
export function Dashboard() {
  const session = useSession();
  const [days, setDays] = useState(30);
  const [demo, setDemo] = useState(false);
  const allowed = !!session.data?.permissions.includes("admin:system:read");
  const q = useOverview(session.data?.id, days, allowed);
  if (!allowed)
    return <Banner error>Chưa có quyền xem thống kê hệ thống.</Banner>;
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
      title: "Bản nháp AI",
      value: d.drafts,
      note: `Trong ${days} ngày`,
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
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
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
              {c.value.toLocaleString("vi-VN")}
            </p>
            <p className="text-[11px] text-muted">{c.note}</p>
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
          <Bars {...pick(d.topPlaces, samples.topPlaces)} />
        </ChartCard>
        <ChartCard
          title="Top tỉnh / thành"
          note={`Số chuyến mới chọn tỉnh/thành làm điểm đến trong ${days} ngày.`}
          sample={!d.topProvinces.length}
        >
          <Bars {...pick(d.topProvinces, samples.topProvinces)} />
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
          note="Snapshot embedding theo model hiện hành. Ngoài phạm vi gồm hồ sơ chưa duyệt, chưa xác minh hoặc ngoài Đà Nẵng."
          sample={!d.rag.length}
        >
          <Donut {...pick(d.rag, samples.rag)} />
        </ChartCard>
      </div>
      <div className="grid gap-5 xl:grid-cols-2">
        <ChartCard
          title="Kết quả tạo nháp AI"
          note="Minh họa bố cục: chưa có bộ dữ liệu telemetry tổng hợp chuẩn hóa; không dùng để đánh giá provider."
          sample
        >
          <Bars data={samples.ai} />
        </ChartCard>
        <section className="rounded-2xl border border-line bg-white p-6">
          <h2 className="font-semibold">Trạng thái tích hợp</h2>
          <p className="my-3 text-xs leading-5 text-muted">
            Database được xác nhận từ truy vấn thống kê này. Các dịch vụ khác
            chưa có health probe; không gọi API có phí để kiểm tra nền.
          </p>
          <label className="mb-4 flex items-center gap-2 text-xs text-muted">
            <input
              type="checkbox"
              checked={demo}
              onChange={(e) => setDemo(e.target.checked)}
            />
            Xem trạng thái mẫu của provider
          </label>
          {[
            "PostgreSQL",
            "Gemini",
            "Embedding",
            "Mapbox",
            "Thời tiết",
            "Cloudinary",
          ].map((name, i) => (
            <div
              key={name}
              className="flex justify-between border-t border-line py-3 text-sm"
            >
              <span>{name}</span>
              <span className={i === 0 ? "text-teal-700" : "text-muted"}>
                {i === 0
                  ? "Kết nối tại thời điểm chốt"
                  : demo
                    ? `${i === 3 ? "Suy giảm" : "Hoạt động"} · MẪU`
                    : "Chưa đo"}
              </span>
            </div>
          ))}
        </section>
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
