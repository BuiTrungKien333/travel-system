"use client";

import {
  Sparkles,
  Plane,
  Hotel,
  Users,
  WifiOff,
  FileDown,
  Check,
  QrCode,
  Compass,
} from "lucide-react";

export function DetailedFeaturesShowcase() {
  return (
    <section id="showcase" className="py-20 lg:py-28 bg-slate-50/60 overflow-hidden">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="mx-auto max-w-3xl text-center">
          <div className="inline-flex items-center gap-2 rounded-full border border-cyan-200 bg-cyan-50 px-3.5 py-1 text-xs font-semibold text-cyan-800">
            <Sparkles className="h-3.5 w-3.5 text-cyan-600" />
            <span>Trải nghiệm tính năng chuyên sâu</span>
          </div>

          <h2 className="mt-4 text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl">
            Thiết kế riêng cho những chuyến đi{" "}
            <span className="text-cyan-600">thảnh thơi nhất</span>
          </h2>

          <p className="mt-4 text-base text-slate-600 sm:text-lg">
            Mỗi chi tiết trong Tripzy đều được tối ưu để bạn dành ít thời gian
            lo lắng về lịch trình hơn, và tận hưởng trọn vẹn từng khoảnh khắc.
          </p>
        </div>

        {/* ─── Z-Pattern Item 1: AI Route Optimization (Image Left, Text Right) ─── */}
        <div className="mt-20 grid grid-cols-1 lg:grid-cols-12 items-center gap-12 lg:gap-16">
          {/* Mockup UI */}
          <div className="lg:col-span-6 order-2 lg:order-1">
            <div className="relative rounded-2xl border border-slate-200 bg-white p-6 shadow-xl shadow-slate-900/5">
              {/* Badge */}
              <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <div className="h-3 w-3 rounded-full bg-emerald-500 animate-ping" />
                  <span className="text-xs font-bold text-slate-800">
                    Thuật toán Held-Karp AI
                  </span>
                </div>
                <span className="rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 px-2.5 py-0.5 text-xs font-bold">
                  Tiết kiệm 45 phút lái xe
                </span>
              </div>

              {/* Comparison visual */}
              <div className="mt-4 space-y-3">
                <div className="rounded-xl bg-rose-50/60 border border-rose-100 p-3 text-xs">
                  <div className="flex items-center justify-between text-rose-800 font-semibold mb-1">
                    <span>❌ Thứ tự tự chọn (Chưa tối ưu)</span>
                    <span>18.4 km • 1h 20p</span>
                  </div>
                  <p className="text-slate-500 text-[11px]">
                    Khách sạn → Bãi biển Mỹ Khê → Chợ Hàn → Cầu Rồng → Bán đảo Sơn Trà (Bị đi ngược đường)
                  </p>
                </div>

                <div className="rounded-xl bg-emerald-50/80 border border-emerald-200 p-3.5 text-xs shadow-sm">
                  <div className="flex items-center justify-between text-emerald-800 font-bold mb-1.5">
                    <span className="flex items-center gap-1.5">
                      <Sparkles className="h-3.5 w-3.5 text-emerald-600" />
                      ✅ Lộ trình AI đề xuất
                    </span>
                    <span className="bg-emerald-600 text-white px-2 py-0.5 rounded text-[11px]">
                      10.2 km • 35 phút
                    </span>
                  </div>
                  <div className="flex flex-col gap-2 mt-2">
                    {[
                      { step: 1, name: "Khách sạn Chicland", time: "08:00" },
                      { step: 2, name: "Bãi biển Mỹ Khê (Tắm biển)", time: "08:15" },
                      { step: 3, name: "Bán đảo Sơn Trà (Chùa Linh Ứng)", time: "10:30" },
                      { step: 4, name: "Chợ Hàn (Ăn trưa đặc sản)", time: "12:30" },
                      { step: 5, name: "Cầu Rồng (Ngắm hoàng hôn)", time: "17:30" },
                    ].map((item) => (
                      <div key={item.step} className="flex items-center gap-2 text-slate-700 font-medium">
                        <span className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-600 text-white text-[10px] font-bold">
                          {item.step}
                        </span>
                        <span className="flex-1 truncate">{item.name}</span>
                        <span className="text-[11px] text-slate-400 font-normal">{item.time}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Text Content */}
          <div className="lg:col-span-6 order-1 lg:order-2">
            <div className="inline-flex items-center gap-2 rounded-lg bg-orange-100/80 px-3 py-1 text-xs font-bold text-orange-700">
              <Sparkles className="h-3.5 w-3.5" />
              TỐI ƯU TUYẾN ĐƯỜNG
            </div>
            <h3 className="mt-3 text-2xl sm:text-3xl font-extrabold text-slate-900 leading-tight">
              Tự động sắp xếp lịch trình thông minh, không còn đi đường vòng
            </h3>
            <p className="mt-4 text-base text-slate-600 leading-relaxed">
              Chỉ cần thêm các điểm bạn muốn ghé thăm, AI sẽ tự động phân tích ma
              trận khoảng cách Google Maps và sắp xếp thứ tự di chuyển tối ưu nhất
              cho bạn.
            </p>
            <ul className="mt-6 space-y-3">
              {[
                "Thuật toán Held-Karp giải bài toán tối ưu tuyến đường chính xác",
                "Tính toán thời gian di chuyển theo ô tô, xe máy hoặc đi bộ",
                "Cảnh báo nếu lịch trình quá dày đặc hoặc giờ mở cửa không phù hợp",
              ].map((item, i) => (
                <li key={i} className="flex items-start gap-2.5 text-sm text-slate-700">
                  <div className="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-orange-500 text-white">
                    <Check className="h-2.5 w-2.5 stroke-[3]" />
                  </div>
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* ─── Z-Pattern Item 2: Reservations & Flights (Text Left, Image Right) ─── */}
        <div className="mt-28 grid grid-cols-1 lg:grid-cols-12 items-center gap-12 lg:gap-16">
          {/* Text Content */}
          <div className="lg:col-span-6">
            <div className="inline-flex items-center gap-2 rounded-lg bg-sky-100/80 px-3 py-1 text-xs font-bold text-sky-700">
              <Plane className="h-3.5 w-3.5" />
              ĐẶT CHỖ & VÉ MÁY BAY
            </div>
            <h3 className="mt-3 text-2xl sm:text-3xl font-extrabold text-slate-900 leading-tight">
              Quản lý tập trung đặt phòng, chuyến bay & vé tham quan
            </h3>
            <p className="mt-4 text-base text-slate-600 leading-relaxed">
              Tạm biệt việc lục tung hòm thư điện tử để tìm mã vé máy bay hay địa
              chỉ khách sạn. Mọi thông tin đặt chỗ được tự động đồng bộ và hiển thị
              ngay trong dòng thời gian của chuyến đi.
            </p>
            <ul className="mt-6 space-y-3">
              {[
                "Đồng bộ vé máy bay Vietnam Airlines, Vietjet, Bamboo Airways",
                "Lưu trữ mã đặt phòng Agoda, Booking.com, Airbnb kèm địa chỉ GPS",
                "Đính kèm mã QR vé tham quan, thẻ lên máy bay để mở nhanh khi cần",
              ].map((item, i) => (
                <li key={i} className="flex items-start gap-2.5 text-sm text-slate-700">
                  <div className="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-sky-600 text-white">
                    <Check className="h-2.5 w-2.5 stroke-[3]" />
                  </div>
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Mockup UI */}
          <div className="lg:col-span-6">
            <div className="relative space-y-4">
              {/* Flight Boarding Pass Card */}
              <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-lg">
                <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                  <div className="flex items-center gap-2">
                    <div className="h-7 w-7 rounded-lg bg-sky-600 text-white flex items-center justify-center font-bold text-xs">
                      VN
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-slate-900">
                        Vietnam Airlines • VN128
                      </h4>
                      <span className="text-[10px] text-slate-400">
                        Hạng Phổ thông • Ghế 14A, 14B
                      </span>
                    </div>
                  </div>
                  <span className="rounded-full bg-emerald-50 text-emerald-700 px-2 py-0.5 text-[11px] font-bold">
                    Đã xác nhận
                  </span>
                </div>

                <div className="mt-3 grid grid-cols-3 items-center text-center">
                  <div>
                    <div className="text-xl font-black text-slate-900">SGN</div>
                    <div className="text-[11px] text-slate-500">TP. Hồ Chí Minh</div>
                    <div className="text-xs font-bold text-orange-600 mt-0.5">07:00</div>
                  </div>
                  <div className="flex flex-col items-center">
                    <span className="text-[10px] text-slate-400">1h 20p</span>
                    <div className="w-full flex items-center justify-center my-1">
                      <div className="h-[1px] w-full bg-slate-300 relative">
                        <Plane className="h-3 w-3 text-sky-600 absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 rotate-90" />
                      </div>
                    </div>
                    <span className="text-[10px] text-emerald-600 font-semibold">Bay thẳng</span>
                  </div>
                  <div>
                    <div className="text-xl font-black text-slate-900">DAD</div>
                    <div className="text-[11px] text-slate-500">Đà Nẵng</div>
                    <div className="text-xs font-bold text-orange-600 mt-0.5">08:20</div>
                  </div>
                </div>
              </div>

              {/* Hotel Reservation Card */}
              <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-lg">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3">
                    <img
                      src="https://images.unsplash.com/photo-1566073771259-6a8506099945?w=150&auto=format&fit=crop&q=80"
                      alt="Hotel"
                      className="h-16 w-16 rounded-xl object-cover"
                    />
                    <div>
                      <div className="flex items-center gap-1 text-amber-500 text-xs font-bold">
                        <Hotel className="h-3.5 w-3.5 text-orange-500" />
                        <span>Resort 5 Sao</span>
                      </div>
                      <h4 className="font-bold text-sm text-slate-900 mt-0.5">
                        Pullman Danang Beach Resort
                      </h4>
                      <p className="text-xs text-slate-500">
                        101 Võ Nguyên Giáp, Ngũ Hành Sơn, Đà Nẵng
                      </p>
                      <div className="mt-1 flex items-center gap-3 text-[11px] text-slate-600">
                        <span>Check-in: <b>14:00 (24/10)</b></span>
                        <span>Mã đặt: <b className="text-orange-600">PLM-98231</b></span>
                      </div>
                    </div>
                  </div>
                  <div className="shrink-0 flex flex-col items-center">
                    <QrCode className="h-12 w-12 text-slate-800" />
                    <span className="text-[9px] text-slate-400 mt-1">Mã QR phòng</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* ─── Z-Pattern Item 3: Realtime Collaboration (Image Left, Text Right) ─── */}
        <div className="mt-28 grid grid-cols-1 lg:grid-cols-12 items-center gap-12 lg:gap-16">
          {/* Mockup UI */}
          <div className="lg:col-span-6 order-2 lg:order-1">
            <div className="relative rounded-2xl border border-slate-200 bg-white p-6 shadow-xl">
              {/* Header */}
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4 text-emerald-600" />
                  <span className="text-xs font-bold text-slate-800">
                    Đang chỉnh sửa cùng nhau (3 người online)
                  </span>
                </div>
                <span className="flex items-center gap-1 text-xs text-emerald-600 font-semibold">
                  <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
                  Thời gian thực
                </span>
              </div>

              {/* Workspace with multi-user cursors */}
              <div className="mt-4 relative bg-slate-50 rounded-xl p-4 border border-slate-200/80 min-h-[220px]">
                {/* Place block being edited */}
                <div className="rounded-lg bg-white p-3 shadow-sm border border-slate-200">
                  <div className="flex items-center justify-between">
                    <h5 className="text-xs font-bold text-slate-800">
                      Bà Nà Hills & Cầu Vàng (Ngày 2)
                    </h5>
                    <span className="text-[11px] text-orange-600 font-semibold">
                      08:00 - 15:00
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-500 mt-1">
                    Đi cáp treo sớm để tránh đông, ăn buffet trưa trên đỉnh núi.
                  </p>

                  {/* Reactions / Votes */}
                  <div className="mt-3 flex items-center gap-2">
                    <button className="flex items-center gap-1 rounded-full bg-orange-50 border border-orange-200 px-2.5 py-0.5 text-[11px] font-bold text-orange-700">
                      ❤️ 4 người thích
                    </button>
                    <button className="flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-[11px] text-slate-600">
                      💬 3 bình luận
                    </button>
                  </div>
                </div>

                {/* Simulated Cursors */}
                {/* Cursor 1: Linh */}
                <div
                  className="absolute z-10 transition-all pointer-events-none"
                  style={{ top: "35px", right: "40px" }}
                >
                  <div className="flex items-center gap-1 bg-emerald-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow-md">
                    <span>Linh: đang thêm vé cáp treo</span>
                  </div>
                </div>

                {/* Cursor 2: Tuấn */}
                <div
                  className="absolute z-10 transition-all pointer-events-none"
                  style={{ bottom: "25px", left: "30px" }}
                >
                  <div className="flex items-center gap-1 bg-indigo-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow-md">
                    <span>Tuấn: vừa vote nhà hàng buffet 👍</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Text Content */}
          <div className="lg:col-span-6 order-1 lg:order-2">
            <div className="inline-flex items-center gap-2 rounded-lg bg-emerald-100/80 px-3 py-1 text-xs font-bold text-emerald-700">
              <Users className="h-3.5 w-3.5" />
              CỘNG TÁC THỜI GIAN THỰC
            </div>
            <h3 className="mt-3 text-2xl sm:text-3xl font-extrabold text-slate-900 leading-tight">
              Lên kế hoạch nhóm dễ dàng như cùng soạn thảo tài liệu
            </h3>
            <p className="mt-4 text-base text-slate-600 leading-relaxed">
              Không còn tình trạng một người phải gánh toàn bộ lịch trình. Mọi
              thành viên trong nhóm có thể cùng thêm địa điểm yêu thích, thả tim
              bình chọn và chia sẻ ý kiến tức thì.
            </p>
            <ul className="mt-6 space-y-3">
              {[
                "Chia sẻ qua link đơn giản, không bắt buộc mọi người phải tải app",
                "Phân quyền chỉnh sửa hoặc chỉ xem linh hoạt",
                "Xem vị trí chuột và thao tác trực tiếp của bạn bè theo thời gian thực",
              ].map((item, i) => (
                <li key={i} className="flex items-start gap-2.5 text-sm text-slate-700">
                  <div className="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-emerald-600 text-white">
                    <Check className="h-2.5 w-2.5 stroke-[3]" />
                  </div>
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* ─── Z-Pattern Item 4: Offline Mode & Export (Text Left, Image Right) ─── */}
        <div className="mt-28 grid grid-cols-1 lg:grid-cols-12 items-center gap-12 lg:gap-16">
          {/* Text Content */}
          <div className="lg:col-span-6">
            <div className="inline-flex items-center gap-2 rounded-lg bg-amber-100/80 px-3 py-1 text-xs font-bold text-amber-800">
              <WifiOff className="h-3.5 w-3.5" />
              NGOẠI TUYẾN & XUẤT FILE
            </div>
            <h3 className="mt-3 text-2xl sm:text-3xl font-extrabold text-slate-900 leading-tight">
              Luôn sẵn sàng trên mọi nẻo đường, ngay cả khi không có sóng 4G
            </h3>
            <p className="mt-4 text-base text-slate-600 leading-relaxed">
              Khi đi du lịch vùng cao hay hải đảo mất kết nối, Tripzy vẫn giúp bạn
              truy cập đầy đủ lịch trình, ghi chú và địa chỉ. Đồng thời bạn có thể
              xuất bản PDF để in hoặc đồng bộ vào Google Maps cá nhân.
            </p>
            <ul className="mt-6 space-y-3">
              {[
                "Tự động lưu trữ offline toàn bộ lịch trình, số điện thoại & địa chỉ",
                "Xuất file PDF lịch trình A4 chuẩn đẹp, chuyên nghiệp chỉ với 1 click",
                "Đồng bộ danh sách địa điểm trực tiếp vào tài khoản Google Maps",
              ].map((item, i) => (
                <li key={i} className="flex items-start gap-2.5 text-sm text-slate-700">
                  <div className="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-amber-600 text-white">
                    <Check className="h-2.5 w-2.5 stroke-[3]" />
                  </div>
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Mockup UI */}
          <div className="lg:col-span-6">
            <div className="relative rounded-2xl border border-slate-200 bg-white p-6 shadow-xl">
              {/* Offline Status */}
              <div className="rounded-xl bg-amber-50 border border-amber-200 p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="h-8 w-8 rounded-lg bg-amber-500 text-white flex items-center justify-center">
                      <WifiOff className="h-4 w-4" />
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-amber-950">
                        Chế độ Ngoại tuyến đang hoạt động
                      </h4>
                      <p className="text-[11px] text-amber-800">
                        Đã tải sẵn: 16 địa điểm • 3 vé máy bay • Bản đồ offline
                      </p>
                    </div>
                  </div>
                  <span className="h-2 w-2 rounded-full bg-amber-500 animate-ping" />
                </div>
              </div>

              {/* Export Buttons Actions */}
              <div className="mt-4 grid grid-cols-2 gap-3">
                <div className="rounded-xl border border-slate-200 p-3.5 bg-slate-50/50 hover:bg-slate-100/60 transition cursor-pointer">
                  <div className="flex items-center gap-2 text-slate-800 font-bold text-xs">
                    <FileDown className="h-4 w-4 text-orange-600" />
                    <span>Xuất file PDF (A4)</span>
                  </div>
                  <p className="text-[11px] text-slate-500 mt-1">
                    Đầy đủ mốc giờ, chi phí & bản đồ thu nhỏ
                  </p>
                </div>

                <div className="rounded-xl border border-slate-200 p-3.5 bg-slate-50/50 hover:bg-slate-100/60 transition cursor-pointer">
                  <div className="flex items-center gap-2 text-slate-800 font-bold text-xs">
                    <Compass className="h-4 w-4 text-sky-600" />
                    <span>Mở trên Google Maps</span>
                  </div>
                  <p className="text-[11px] text-slate-500 mt-1">
                    Đồng bộ các điểm ghim vào tài khoản
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* ─── Z-Pattern Item 5: 63 Provinces & Memories (Image Left, Text Right) ─── */}
        <div className="mt-28 grid grid-cols-1 lg:grid-cols-12 items-center gap-12 lg:gap-16">
          {/* Mockup UI */}
          <div className="lg:col-span-6 order-2 lg:order-1">
            <div className="relative rounded-2xl border border-slate-200 bg-white p-6 shadow-xl">
              {/* Vietnam Journey Progress */}
              <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <div className="h-7 w-7 rounded-lg bg-orange-600 text-white flex items-center justify-center font-bold text-xs">
                    VN
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-slate-900">
                      Bản đồ khám phá Việt Nam của bạn
                    </h4>
                    <span className="text-[10px] text-slate-400">
                      Đã đặt chân đến 28 / 63 tỉnh thành (44.4%)
                    </span>
                  </div>
                </div>
                <span className="rounded-full bg-orange-50 text-orange-700 px-2.5 py-0.5 text-xs font-bold border border-orange-100">
                  Hạng: Nhà Thám Hiểm 🌟
                </span>
              </div>

              {/* Progress bar */}
              <div className="mt-4">
                <div className="h-2 w-full rounded-full bg-slate-100 overflow-hidden">
                  <div className="h-full rounded-full bg-gradient-to-r from-orange-500 to-amber-500 w-[44.4%]" />
                </div>
              </div>

              {/* Memories Polaroid Preview */}
              <div className="mt-4 grid grid-cols-3 gap-2.5">
                {[
                  {
                    title: "Bình minh Sa Pa",
                    img: "https://images.unsplash.com/photo-1528127269322-539801943592?w=200&auto=format&fit=crop&q=80",
                    date: "Tháng 9, 2025",
                  },
                  {
                    title: "Biển Mỹ Khê",
                    img: "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=200&auto=format&fit=crop&q=80",
                    date: "Tháng 10, 2025",
                  },
                  {
                    title: "Hoàng hôn Phú Quốc",
                    img: "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=200&auto=format&fit=crop&q=80",
                    date: "Tháng 12, 2025",
                  },
                ].map((item, i) => (
                  <div key={i} className="rounded-xl border border-slate-200 bg-slate-50 p-1.5 shadow-sm">
                    <img
                      src={item.img}
                      alt={item.title}
                      className="h-20 w-full rounded-lg object-cover"
                    />
                    <div className="mt-1 px-1">
                      <p className="text-[10px] font-bold text-slate-800 truncate">
                        {item.title}
                      </p>
                      <p className="text-[9px] text-slate-400">{item.date}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Text Content */}
          <div className="lg:col-span-6 order-1 lg:order-2">
            <div className="inline-flex items-center gap-2 rounded-lg bg-rose-100/80 px-3 py-1 text-xs font-bold text-rose-700">
              <Compass className="h-3.5 w-3.5" />
              HÀNH TRÌNH & KỶ NIỆM
            </div>
            <h3 className="mt-3 text-2xl sm:text-3xl font-extrabold text-slate-900 leading-tight">
              Tô màu bản đồ 63 tỉnh thành & Lưu giữ từng mốc kỷ niệm
            </h3>
            <p className="mt-4 text-base text-slate-600 leading-relaxed">
              Mỗi chuyến đi là một trang nhật ký rực rỡ. Tripzy giúp bạn theo dõi
              hành trình khám phá trọn vẹn dải đất hình chữ S, lưu trữ album ảnh
              và những khoảnh khắc đẹp nhất theo dòng thời gian.
            </p>
            <ul className="mt-6 space-y-3">
              {[
                "Bản đồ nhiệt 63 tỉnh thành tự động sáng lên khi bạn check-in",
                "Album ảnh gắn thẻ định vị GPS và cảm xúc sau chuyến đi",
                "Huy hiệu thành tựu du lịch thú vị để chia sẻ cùng bạn bè",
              ].map((item, i) => (
                <li key={i} className="flex items-start gap-2.5 text-sm text-slate-700">
                  <div className="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-rose-600 text-white">
                    <Check className="h-2.5 w-2.5 stroke-[3]" />
                  </div>
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </section>
  );
}
