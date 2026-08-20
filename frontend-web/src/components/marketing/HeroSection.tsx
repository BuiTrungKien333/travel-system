"use client";

import { useState } from "react";
import Link from "next/link";
import {
  Sparkles,
  ArrowRight,
  Clock,
  Car,
  Star,
  CheckCircle2,
  Share2,
  Navigation,
  Layers,
  Plus,
  Play,
  Check,
} from "lucide-react";
import { ROUTES } from "@/lib/constants";

export function HeroSection() {
  const [activeDay, setActiveDay] = useState(1);
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | null>(2);

  const places = [
    {
      id: 1,
      num: 1,
      name: "Bún chả cá Hòn 113",
      category: "Ăn sáng • Đặc sản",
      time: "08:00 - 09:15",
      rating: 4.8,
      reviews: 1420,
      image: "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=300&auto=format&fit=crop&q=80",
      note: "Thưởng thức bún chả cá gia truyền ngon nức tiếng Đà Nẵng",
      durationToNext: "10 phút",
      distToNext: "3.2 km",
    },
    {
      id: 2,
      num: 2,
      name: "Bãi biển Mỹ Khê",
      category: "Bãi biển • Check-in",
      time: "09:30 - 11:45",
      rating: 4.9,
      reviews: 5890,
      image: "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=300&auto=format&fit=crop&q=80",
      note: "Tắm biển, lướt sóng và uống dừa tươi bên rặng dừa xanh",
      durationToNext: "12 phút",
      distToNext: "4.5 km",
    },
    {
      id: 3,
      num: 3,
      name: "Cơm Niêu Nhà Đỏ 2",
      category: "Ăn trưa • Ẩm thực",
      time: "12:00 - 13:30",
      rating: 4.7,
      reviews: 2130,
      image: "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=300&auto=format&fit=crop&q=80",
      note: "Cá bống kho tộ và canh cua rau đay cực ngon",
      durationToNext: "8 phút",
      distToNext: "2.1 km",
    },
    {
      id: 4,
      num: 4,
      name: "Cầu Rồng & Chợ đêm Sơn Trà",
      category: "Tham quan • Giải trí",
      time: "19:00 - 21:30",
      rating: 4.9,
      reviews: 8400,
      image: "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=300&auto=format&fit=crop&q=80",
      note: "Xem rồng phun lửa & phun nước lúc 21:00 cuối tuần",
      durationToNext: null,
      distToNext: null,
    },
  ];

  return (
    <section className="relative overflow-hidden bg-gradient-to-b from-sky-50/60 via-cyan-50/20 to-white pt-10 pb-20 lg:pt-16 lg:pb-28">
      {/* Background ambient glowing orbs */}
      <div className="absolute top-10 left-1/2 -translate-x-1/2 w-[800px] h-[350px] bg-gradient-to-tr from-cyan-200/30 via-teal-200/20 to-sky-200/30 blur-3xl rounded-full pointer-events-none -z-10" />

      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Hero Top Copy */}
        <div className="mx-auto max-w-3xl text-center">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 rounded-full border border-cyan-200 bg-cyan-50/80 px-3.5 py-1.5 text-xs font-semibold text-cyan-800 shadow-sm backdrop-blur">
            <span className="flex h-2 w-2 rounded-full bg-cyan-500 animate-pulse" />
            <span>Nền tảng lên kế hoạch du lịch AI #1 Việt Nam</span>
            <span className="rounded-full bg-cyan-200/70 px-1.5 py-0.5 text-[10px] uppercase font-bold text-cyan-800">
              Mới
            </span>
          </div>

          {/* Main Title */}
          <h1 className="mt-5 text-4xl font-extrabold tracking-tight text-slate-900 sm:text-5xl lg:text-6xl leading-[1.15]">
            Ứng dụng lên kế hoạch du lịch{" "}
            <span className="bg-gradient-to-r from-cyan-600 via-teal-600 to-sky-600 bg-clip-text text-transparent">
              thông minh & dễ dàng
            </span>{" "}
            nhất
          </h1>

          {/* Subtitle */}
          <p className="mt-5 text-lg text-slate-600 sm:text-xl max-w-2xl mx-auto leading-relaxed">
            Tất cả trong một: Tự động tối ưu lịch trình với AI, bản đồ tương tác
            Google Maps trực quan, quản lý đặt phòng và lên kế hoạch cùng bạn bè
            theo thời gian thực.
          </p>

          {/* CTA Buttons */}
          <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4">
            <Link
              href={ROUTES.tripNew}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl bg-cyan-500 px-7 py-3.5 text-base font-bold text-white shadow-lg shadow-cyan-500/25 hover:bg-cyan-600 active:scale-[0.99] transition-all"
            >
              <span>Bắt đầu lên kế hoạch — Miễn phí</span>
              <ArrowRight className="h-5 w-5" />
            </Link>

            <a
              href="#app-preview"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl border border-slate-300/80 bg-white/90 px-6 py-3.5 text-base font-semibold text-slate-800 shadow-sm hover:bg-slate-50 hover:border-slate-400 transition"
            >
              <Play className="h-4 w-4 fill-slate-700 text-slate-700" />
              <span>Xem giao diện mẫu</span>
            </a>
          </div>

          {/* Social Proof Stats */}
          <div className="mt-6 flex flex-wrap items-center justify-center gap-y-2 gap-x-6 text-xs text-slate-500">
            <div className="flex items-center gap-1.5">
              <div className="flex text-amber-400">
                {[...Array(5)].map((_, i) => (
                  <Star key={i} className="h-3.5 w-3.5 fill-amber-400" />
                ))}
              </div>
              <span className="font-semibold text-slate-700">4.9/5</span>
              <span>(12,000+ đánh giá)</span>
            </div>
            <div className="flex items-center gap-1.5">
              <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" />
              <span>500,000+ chuyến đi đã tạo</span>
            </div>
            <div className="flex items-center gap-1.5">
              <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" />
              <span>Miễn phí 100% không giới hạn</span>
            </div>
          </div>
        </div>

        {/* ─── App Preview Mockup (2-column Wanderlog style) ─── */}
        <div id="app-preview" className="mt-12 lg:mt-16">
          <div className="rounded-2xl border border-slate-200/90 bg-white shadow-2xl shadow-slate-900/10 overflow-hidden ring-1 ring-black/5">
            {/* Window Header */}
            <div className="border-b border-slate-200 bg-slate-50/90 px-4 py-3 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                {/* Traffic light dots */}
                <div className="flex items-center gap-1.5">
                  <div className="h-3 w-3 rounded-full bg-rose-400" />
                  <div className="h-3 w-3 rounded-full bg-amber-400" />
                  <div className="h-3 w-3 rounded-full bg-emerald-400" />
                </div>
                {/* Trip Title */}
                <div className="flex items-center gap-2 pl-2 border-l border-slate-200">
                  <span className="text-sm font-bold text-slate-800">
                    🌴 Đà Nẵng - Hội An: Hành Trình Ẩm Thực & Biển Xanh (4N3Đ)
                  </span>
                  <span className="hidden sm:inline-flex items-center rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-700">
                    Sẵn sàng khởi hành
                  </span>
                </div>
              </div>

              {/* Collaborators & Action icons */}
              <div className="flex items-center gap-3">
                <div className="flex items-center -space-x-2">
                  <img
                    src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=60&auto=format&fit=crop&q=80"
                    alt="Linh"
                    className="h-7 w-7 rounded-full border-2 border-white object-cover"
                    title="Linh (Bạn)"
                  />
                  <img
                    src="https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=60&auto=format&fit=crop&q=80"
                    alt="Tuấn"
                    className="h-7 w-7 rounded-full border-2 border-white object-cover"
                    title="Tuấn"
                  />
                  <img
                    src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=60&auto=format&fit=crop&q=80"
                    alt="Hương"
                    className="h-7 w-7 rounded-full border-2 border-white object-cover"
                    title="Hương"
                  />
                  <div className="flex h-7 w-7 items-center justify-center rounded-full border-2 border-white bg-cyan-100 text-[11px] font-bold text-cyan-700">
                    +2
                  </div>
                </div>

                <div className="hidden md:flex items-center gap-2">
                  <button className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50">
                    <Share2 className="h-3.5 w-3.5 text-slate-500" />
                    <span>Chia sẻ</span>
                  </button>
                  <button className="inline-flex items-center gap-1.5 rounded-lg bg-cyan-500 px-3 py-1.5 text-xs font-semibold text-white hover:bg-cyan-600">
                    <Sparkles className="h-3.5 w-3.5" />
                    <span>AI Tối ưu lại</span>
                  </button>
                </div>
              </div>
            </div>

            {/* Sub-header Day Tabs */}
            <div className="border-b border-slate-200 bg-white px-4 flex items-center justify-between overflow-x-auto">
              <div className="flex items-center gap-2 py-2">
                {[
                  { day: 1, label: "Ngày 1: Biển Mỹ Khê & Cầu Rồng", date: "T6, 24 Th10" },
                  { day: 2, label: "Ngày 2: Ngũ Hành Sơn & Hội An", date: "T7, 25 Th10" },
                  { day: 3, label: "Ngày 3: Bà Nà Hills Cầu Vàng", date: "CN, 26 Th10" },
                  { day: 4, label: "Ngày 4: Chợ Cồn & Mua sắm", date: "T2, 27 Th10" },
                ].map((tab) => (
                  <button
                    key={tab.day}
                    onClick={() => setActiveDay(tab.day)}
                    className={`flex items-center gap-2 rounded-lg px-3.5 py-2 text-xs font-semibold transition shrink-0 ${
                      activeDay === tab.day
                        ? "bg-cyan-500 text-white shadow-sm"
                        : "text-slate-600 hover:bg-slate-100"
                    }`}
                  >
                    <span>{tab.label}</span>
                    <span
                      className={`rounded px-1.5 py-0.5 text-[10px] ${
                        activeDay === tab.day
                          ? "bg-cyan-600/60 text-white"
                          : "bg-slate-100 text-slate-500"
                      }`}
                    >
                      {tab.date}
                    </span>
                  </button>
                ))}
              </div>

              <div className="hidden lg:flex items-center gap-2 text-xs text-slate-500 pl-4 border-l border-slate-100">
                <span className="font-semibold text-slate-700">4 điểm đến</span>
                <span>•</span>
                <span>15.6 km</span>
                <span>•</span>
                <span className="text-emerald-600 font-semibold flex items-center gap-1">
                  <Check className="h-3 w-3" /> Đã tối ưu tuyến đường
                </span>
              </div>
            </div>

            {/* 2-Column Split: Itinerary Left & Map Right */}
            <div className="grid grid-cols-1 lg:grid-cols-12 min-h-[560px]">
              {/* Left Column: Itinerary List */}
              <div className="lg:col-span-5 border-r border-slate-200 bg-white p-4 sm:p-5 overflow-y-auto max-h-[620px]">
                {/* AI Optimization Notice */}
                <div className="mb-4 rounded-xl border border-cyan-200 bg-gradient-to-r from-cyan-50 to-sky-50 p-3.5">
                  <div className="flex items-start gap-2.5">
                    <div className="rounded-lg bg-cyan-500 p-1 text-white">
                      <Sparkles className="h-4 w-4" />
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-cyan-950">
                        AI Held-Karp đã sắp xếp thứ tự di chuyển tối ưu
                      </h4>
                      <p className="text-[11px] text-cyan-800/90 mt-0.5">
                        Tiết kiệm 45 phút di chuyển & 8.4 km so với thứ tự ban đầu.
                      </p>
                    </div>
                  </div>
                </div>

                {/* Places list */}
                <div className="space-y-3">
                  {places.map((place) => {
                    const isSelected = selectedPlaceId === place.id;
                    return (
                      <div key={place.id} className="group">
                        <div
                          onClick={() => setSelectedPlaceId(place.id)}
                          className={`cursor-pointer rounded-xl border p-3.5 transition-all ${
                            isSelected
                              ? "border-cyan-500 bg-cyan-50/40 shadow-sm ring-1 ring-cyan-500"
                              : "border-slate-200 bg-white hover:border-slate-300 hover:shadow-sm"
                          }`}
                        >
                          <div className="flex items-start gap-3">
                            {/* Pin Number */}
                            <div
                              className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                                isSelected
                                  ? "bg-cyan-600 text-white shadow-sm"
                                  : "bg-slate-100 text-slate-700 group-hover:bg-cyan-100 group-hover:text-cyan-700"
                              }`}
                            >
                              {place.num}
                            </div>

                            {/* Place Info */}
                            <div className="flex-1 min-w-0">
                              <div className="flex items-start justify-between gap-2">
                                <h4 className="text-sm font-bold text-slate-900 truncate">
                                  {place.name}
                                </h4>
                                <div className="flex items-center gap-1 shrink-0 text-amber-500 text-xs font-bold">
                                  <Star className="h-3 w-3 fill-amber-400 text-amber-400" />
                                  <span>{place.rating}</span>
                                </div>
                              </div>

                              <div className="mt-1 flex items-center gap-2 text-xs text-slate-500">
                                <span className="text-cyan-600 font-medium">{place.category}</span>
                                <span>•</span>
                                <span className="flex items-center gap-1">
                                  <Clock className="h-3 w-3 text-slate-400" />
                                  {place.time}
                                </span>
                              </div>

                              <p className="mt-1.5 text-xs text-slate-600 line-clamp-2">
                                {place.note}
                              </p>
                            </div>

                            {/* Thumbnail */}
                            <img
                              src={place.image}
                              alt={place.name}
                              className="h-16 w-16 rounded-lg object-cover shrink-0 border border-slate-100"
                            />
                          </div>
                        </div>

                        {/* Route / Distance badge between items */}
                        {place.durationToNext && (
                          <div className="my-1.5 flex items-center justify-center">
                            <div className="flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-[11px] font-semibold text-slate-600">
                              <Car className="h-3 w-3 text-slate-400" />
                              <span>
                                {place.durationToNext} ({place.distToNext})
                              </span>
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* Add place button */}
                <button className="mt-4 w-full flex items-center justify-center gap-2 rounded-xl border-2 border-dashed border-slate-200 py-3 text-xs font-semibold text-slate-600 hover:border-cyan-400 hover:text-cyan-600 hover:bg-cyan-50/40 transition">
                  <Plus className="h-4 w-4" />
                  <span>Thêm địa điểm vào Ngày 1</span>
                </button>
              </div>

              {/* Right Column: Google Maps Simulation */}
              <div className="lg:col-span-7 relative bg-slate-100 min-h-[420px] lg:min-h-[560px] overflow-hidden flex flex-col">
                {/* Simulated Google Map Background */}
                <div className="absolute inset-0 bg-[#e5e3df] overflow-hidden">
                  {/* Stylized Map SVGs: Sea, Land, Roads, River */}
                  <svg className="w-full h-full" xmlns="http://www.w3.org/2000/svg">
                    <defs>
                      <linearGradient id="seaGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="#a5d4f3" />
                        <stop offset="100%" stopColor="#89c2ea" />
                      </linearGradient>
                    </defs>

                    {/* Ocean on the right */}
                    <path
                      d="M 450,0 Q 520,200 480,400 T 560,700 L 900,700 L 900,0 Z"
                      fill="url(#seaGrad)"
                    />

                    {/* Han River flowing down */}
                    <path
                      d="M 280,0 Q 300,180 270,350 T 230,700"
                      fill="none"
                      stroke="#89c2ea"
                      strokeWidth="28"
                    />

                    {/* Bridge connecting across Han River */}
                    <line x1="240" y1="460" x2="310" y2="455" stroke="#f59e0b" strokeWidth="6" strokeLinecap="round" />

                    {/* Main Roads */}
                    <path d="M 0,120 L 500,120" stroke="#ffffff" strokeWidth="10" />
                    <path d="M 0,310 L 500,310" stroke="#ffffff" strokeWidth="12" />
                    <path d="M 0,460 L 500,460" stroke="#ffffff" strokeWidth="14" />
                    <path d="M 430,0 L 430,700" stroke="#ffffff" strokeWidth="12" />

                    {/* Secondary streets grid */}
                    <path d="M 120,0 L 120,700" stroke="#f1f0eb" strokeWidth="5" />
                    <path d="M 200,0 L 200,700" stroke="#f1f0eb" strokeWidth="5" />
                    <path d="M 360,0 L 360,700" stroke="#f1f0eb" strokeWidth="5" />
                    <path d="M 0,220 L 500,220" stroke="#f1f0eb" strokeWidth="5" />
                    <path d="M 0,390 L 500,390" stroke="#f1f0eb" strokeWidth="5" />

                    {/* Route Direction Polyline with cyan style */}
                    <path
                      d="M 175,115 L 200,120 L 430,120 L 425,165"
                      fill="none"
                      stroke="#06b6d4"
                      strokeWidth="5"
                      strokeDasharray="6,4"
                    />
                    <path
                      d="M 425,165 L 430,310 L 245,305"
                      fill="none"
                      stroke="#06b6d4"
                      strokeWidth="5"
                      strokeDasharray="6,4"
                    />
                    <path
                      d="M 245,305 L 245,455 L 365,455"
                      fill="none"
                      stroke="#06b6d4"
                      strokeWidth="5"
                      strokeDasharray="6,4"
                    />
                  </svg>

                  {/* Danang Landmarks Labels */}
                  <div className="absolute top-6 left-8 bg-white/80 backdrop-blur px-2.5 py-1 rounded-md text-[11px] font-bold text-slate-700 shadow-sm">
                    🏙️ Trung tâm TP. Đà Nẵng
                  </div>
                  <div className="absolute top-8 right-14 bg-sky-100/90 text-sky-800 px-2.5 py-1 rounded-md text-[11px] font-bold shadow-sm">
                    🌊 Biển Đông (Bãi biển Mỹ Khê)
                  </div>
                  <div className="absolute bottom-8 right-10 bg-amber-50/90 text-amber-900 px-2.5 py-1 rounded-md text-[11px] font-bold shadow-sm">
                    🏮 Hướng đi Phố Cổ Hội An (25km)
                  </div>

                  {/* Map Pins - No hover scale */}
                  {/* Pin 1: Bún chả cá Hòn */}
                  <div
                    onClick={() => setSelectedPlaceId(1)}
                    className="absolute cursor-pointer"
                    style={{ top: "115px", left: "175px" }}
                  >
                    <div
                      className={`flex h-8 w-8 items-center justify-center rounded-full font-bold text-xs shadow-lg border-2 border-white ${
                        selectedPlaceId === 1
                          ? "bg-cyan-600 text-white ring-4 ring-cyan-400/40"
                          : "bg-slate-800 text-white"
                      }`}
                    >
                      1
                    </div>
                  </div>

                  {/* Pin 2: Bãi biển Mỹ Khê */}
                  <div
                    onClick={() => setSelectedPlaceId(2)}
                    className="absolute cursor-pointer z-20"
                    style={{ top: "165px", left: "425px" }}
                  >
                    <div
                      className={`flex h-9 w-9 items-center justify-center rounded-full font-extrabold text-sm shadow-xl border-2 border-white ${
                        selectedPlaceId === 2
                          ? "bg-cyan-600 text-white ring-4 ring-cyan-400/50"
                          : "bg-slate-800 text-white"
                      }`}
                    >
                      2
                    </div>

                    {/* Selected Place Popup Card */}
                    <div className="absolute top-11 -left-28 w-60 rounded-xl bg-white p-3 shadow-2xl border border-slate-200 pointer-events-auto animate-fade-in z-30">
                      <div className="flex items-start gap-2.5">
                        <img
                          src="https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=120&auto=format&fit=crop&q=80"
                          alt="Mỹ Khê"
                          className="h-12 w-12 rounded-lg object-cover"
                        />
                        <div className="flex-1 min-w-0">
                          <h5 className="font-bold text-xs text-slate-900 truncate">
                            Bãi biển Mỹ Khê
                          </h5>
                          <div className="flex items-center gap-1 text-[11px] text-amber-500 font-semibold mt-0.5">
                            <Star className="h-3 w-3 fill-amber-400" />
                            <span>4.9 (5,890 đánh giá)</span>
                          </div>
                          <span className="text-[10px] text-emerald-600 font-semibold">
                            ● Đang mở cửa • Bãi tắm số 1
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Pin 3: Cơm Niêu Nhà Đỏ */}
                  <div
                    onClick={() => setSelectedPlaceId(3)}
                    className="absolute cursor-pointer"
                    style={{ top: "305px", left: "245px" }}
                  >
                    <div
                      className={`flex h-8 w-8 items-center justify-center rounded-full font-bold text-xs shadow-lg border-2 border-white ${
                        selectedPlaceId === 3
                          ? "bg-cyan-600 text-white ring-4 ring-cyan-400/40"
                          : "bg-slate-800 text-white"
                      }`}
                    >
                      3
                    </div>
                  </div>

                  {/* Pin 4: Cầu Rồng */}
                  <div
                    onClick={() => setSelectedPlaceId(4)}
                    className="absolute cursor-pointer"
                    style={{ top: "455px", left: "365px" }}
                  >
                    <div
                      className={`flex h-8 w-8 items-center justify-center rounded-full font-bold text-xs shadow-lg border-2 border-white ${
                        selectedPlaceId === 4
                          ? "bg-cyan-600 text-white ring-4 ring-cyan-400/40"
                          : "bg-slate-800 text-white"
                      }`}
                    >
                      4
                    </div>
                  </div>
                </div>

                {/* Map Control Buttons (Simulated Google Maps UI) */}
                <div className="absolute top-4 right-4 flex flex-col gap-1.5 z-10">
                  <div className="rounded-lg bg-white shadow-md border border-slate-200 flex flex-col divide-y divide-slate-100 overflow-hidden text-xs font-bold text-slate-700">
                    <button className="px-2.5 py-1.5 hover:bg-slate-50">+</button>
                    <button className="px-2.5 py-1.5 hover:bg-slate-50">−</button>
                  </div>
                  <button className="rounded-lg bg-white p-2 shadow-md border border-slate-200 text-slate-700 hover:bg-slate-50">
                    <Layers className="h-4 w-4" />
                  </button>
                  <button className="rounded-lg bg-white p-2 shadow-md border border-slate-200 text-orange-600 hover:bg-slate-50">
                    <Navigation className="h-4 w-4" />
                  </button>
                </div>

                {/* Bottom Route Summary Bar on Map */}
                <div className="mt-auto p-4 z-10">
                  <div className="rounded-xl border border-white/80 bg-white/95 backdrop-blur-md p-3 shadow-lg flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-orange-100 text-orange-600">
                        <Navigation className="h-5 w-5" />
                      </div>
                      <div>
                        <div className="text-xs font-bold text-slate-900">
                          Lộ trình Ngày 1: 4 chặng di chuyển
                        </div>
                        <div className="text-[11px] text-slate-500">
                          Tổng thời gian lái xe: <span className="font-semibold text-slate-700">30 phút</span> • Khoảng cách: <span className="font-semibold text-slate-700">9.8 km</span>
                        </div>
                      </div>
                    </div>
                    <button className="rounded-lg bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white hover:bg-slate-800">
                      Mở Google Maps
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
