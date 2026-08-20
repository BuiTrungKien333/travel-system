"use client";

import Link from "next/link";
import { ArrowRight, Sparkles, CheckCircle2, Luggage } from "lucide-react";
import { ROUTES } from "@/lib/constants";

export function CtaBanner() {
  return (
    <section className="relative overflow-hidden bg-slate-900 py-20 lg:py-24 text-white">
      {/* Background glow & decorative circles */}
      <div className="absolute top-0 right-0 -mt-12 -mr-12 h-96 w-96 rounded-full bg-cyan-500/20 blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 left-0 -mb-12 -ml-12 h-96 w-96 rounded-full bg-teal-500/20 blur-3xl pointer-events-none" />

      <div className="relative mx-auto max-w-5xl px-4 sm:px-6 lg:px-8 text-center">
        {/* Badge */}
        <div className="inline-flex items-center gap-2 rounded-full border border-cyan-500/30 bg-cyan-500/10 px-4 py-1.5 text-xs font-semibold text-cyan-400">
          <Sparkles className="h-3.5 w-3.5" />
          <span>Bắt đầu hành trình ngay hôm nay</span>
        </div>

        {/* Headline */}
        <h2 className="mt-6 text-3xl font-extrabold tracking-tight sm:text-4xl lg:text-5xl leading-tight">
          Sẵn sàng cho chuyến phiêu lưu{" "}
          <span className="bg-gradient-to-r from-cyan-400 via-teal-300 to-sky-400 bg-clip-text text-transparent">
            tiếp theo của bạn?
          </span>
        </h2>

        {/* Subtitle */}
        <p className="mt-4 text-base sm:text-lg text-slate-300 max-w-2xl mx-auto leading-relaxed">
          Bắt đầu lên lịch trình khám phá 63 tỉnh thành Việt Nam và lưu giữ những kỷ niệm quý giá ngay bây giờ — hoàn toàn miễn phí và không bắt buộc đăng nhập!
        </p>

        {/* Action Buttons */}
        <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link
            href={ROUTES.tripNew}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-500 px-8 py-4 text-base font-bold text-white shadow-xl shadow-cyan-500/30 hover:bg-cyan-600 active:scale-[0.99] transition-all"
          >
            <span>Tạo chuyến đi mới ngay</span>
            <ArrowRight className="h-5 w-5" />
          </Link>

          <Link
            href={ROUTES.trips}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-700 bg-slate-800/80 px-7 py-4 text-base font-semibold text-slate-200 hover:bg-slate-800 hover:text-white transition"
          >
            <Luggage className="h-5 w-5 text-slate-400" />
            <span>Xem chuyến đi mẫu</span>
          </Link>
        </div>

        {/* Trust Badges */}
        <div className="mt-10 flex flex-wrap items-center justify-center gap-6 text-xs text-slate-400">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 text-cyan-400" />
            <span>Miễn phí 100% trọn đời</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 text-cyan-400" />
            <span>Không yêu cầu đăng nhập</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 text-cyan-400" />
            <span>Đầy đủ dữ liệu 63 tỉnh thành</span>
          </div>
        </div>
      </div>
    </section>
  );
}
