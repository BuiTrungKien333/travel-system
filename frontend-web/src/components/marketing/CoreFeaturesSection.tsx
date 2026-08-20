"use client";

import { Compass, ArrowRight, Sparkles } from "lucide-react";
import Link from "next/link";
import { ROUTES, MARKETING_CORE_FEATURES } from "@/constants";
import { SectionHeading } from "@/components/ui";

export function CoreFeaturesSection() {
  return (
    <section id="features" className="py-20 lg:py-28 bg-white border-y border-slate-100">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <SectionHeading
          badgeIcon={Compass}
          badgeText="Tính năng cốt lõi"
          title="Mọi thứ bạn cần cho một chuyến đi"
          highlight="hoàn hảo"
          subtitle="Không còn phải đau đầu mở hàng chục tab trình duyệt, gửi tin nhắn rời rạc hay ghi chú lộn xộn trong bảng tính Excel."
        />

        {/* Features Grid */}
        <div className="mt-14 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {MARKETING_CORE_FEATURES.map((feature, idx) => {
            const Icon = feature.icon;
            return (
              <div
                key={idx}
                className="group relative rounded-3xl border border-cyan-100/80 bg-white p-7 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-cyan-300 hover:shadow-xl hover:shadow-cyan-500/5"
              >
                <div className="flex items-center justify-between">
                  <div
                    className={`flex h-12 w-12 items-center justify-center rounded-2xl border ${feature.iconBg}`}
                  >
                    <Icon className="h-6 w-6" />
                  </div>
                  <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-[11px] font-semibold text-slate-600">
                    {feature.badge}
                  </span>
                </div>

                <h3 className="mt-6 text-lg font-bold text-slate-900 group-hover:text-cyan-600 transition-colors">
                  {feature.title}
                </h3>

                <p className="mt-2.5 text-sm leading-relaxed text-slate-600">
                  {feature.description}
                </p>

                <div className="mt-5 flex items-center gap-1.5 text-xs font-bold text-cyan-600 opacity-0 group-hover:opacity-100 transition-opacity">
                  <span>Tìm hiểu thêm</span>
                  <ArrowRight className="h-3.5 w-3.5" />
                </div>
              </div>
            );
          })}
        </div>

        {/* Feature Bottom Callout */}
        <div className="mt-12 rounded-3xl bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 p-6 sm:p-8 text-white shadow-xl">
          <div className="flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-cyan-500 text-white shadow-md">
                <Sparkles className="h-6 w-6" />
              </div>
              <div>
                <h4 className="text-lg font-bold">
                  Sẵn sàng nâng tầm trải nghiệm du lịch của bạn?
                </h4>
                <p className="text-sm text-slate-300 mt-1">
                  Chỉ mất 60 giây để tạo một lịch trình chi tiết và mời bạn bè tham gia.
                </p>
              </div>
            </div>

            <Link
              href={ROUTES.tripNew}
              className="shrink-0 inline-flex items-center gap-2 rounded-xl bg-cyan-500 px-6 py-3 text-sm font-bold text-white shadow-md hover:bg-cyan-600 transition-all"
            >
              <span>Thử ngay miễn phí</span>
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
