"use client";

import { MapPin, ArrowRight, Compass, Star } from "lucide-react";
import Link from "next/link";
import { ROUTES, POPULAR_DESTINATIONS_DATA } from "@/constants";

export function PopularDestinationsSection() {
  return (
    <section
      id="destinations"
      className="py-20 lg:py-28 bg-white border-t border-slate-100"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div className="max-w-2xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-cyan-200 bg-cyan-50 px-3.5 py-1 text-xs font-semibold text-cyan-800">
              <Compass className="h-3.5 w-3.5 text-cyan-600" />
              <span>Điểm đến hàng đầu Việt Nam</span>
            </div>

            <h2 className="mt-4 text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl">
              Khám phá các điểm đến{" "}
              <span className="text-cyan-600">thịnh hành</span>
            </h2>

            <p className="mt-3 text-base text-slate-600">
              Hàng ngàn du khách đã tạo lịch trình và trải nghiệm trọn vẹn những
              tọa độ tuyệt đẹp này trên khắp dải đất hình chữ S.
            </p>
          </div>

          <Link
            href={ROUTES.travelGuides}
            className="inline-flex items-center gap-2 text-sm font-bold text-cyan-600 hover:text-cyan-700 transition group self-start md:self-auto"
          >
            <span>Xem tất cả cẩm nang du lịch</span>
            <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
          </Link>
        </div>

        {/* Destinations Grid (2x3 on large screens) */}
        <div className="mt-14 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {POPULAR_DESTINATIONS_DATA.map((dest, idx) => (
            <div
              key={idx}
              className="group relative overflow-hidden rounded-3xl border border-slate-200/90 bg-white shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-cyan-300 hover:shadow-xl hover:shadow-cyan-500/5 flex flex-col"
            >
              {/* Destination Image (No hover zoom per UX requirement) */}
              <div className="relative h-56 w-full overflow-hidden bg-slate-100">
                <img
                  src={dest.image}
                  alt={dest.name}
                  className="h-full w-full object-cover"
                />

                {/* Rating Badge */}
                <div className="absolute top-3.5 right-3.5 flex items-center gap-1 rounded-full bg-white/95 px-2.5 py-1 text-xs font-bold text-slate-900 shadow-md backdrop-blur-sm">
                  <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                  <span>{dest.rating}</span>
                </div>

                {/* Region Badge */}
                <div className="absolute top-3.5 left-3.5 rounded-full bg-slate-900/70 px-3 py-1 text-xs font-semibold text-white backdrop-blur-sm">
                  {dest.region}
                </div>
              </div>

              {/* Destination Details */}
              <div className="p-6 flex-1 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between">
                    <h3 className="text-xl font-bold text-slate-900 group-hover:text-cyan-600 transition-colors">
                      {dest.name}
                    </h3>
                    <span className="text-xs font-medium text-slate-500">
                      {dest.tripsPlanned} chuyến đi
                    </span>
                  </div>

                  {/* Highlights Tags */}
                  <div className="mt-4 flex flex-wrap gap-1.5">
                    {dest.highlights.map((h, i) => (
                      <span
                        key={i}
                        className="inline-flex items-center gap-1 rounded-lg bg-slate-50 px-2.5 py-1 text-xs font-medium text-slate-600 border border-slate-100"
                      >
                        <MapPin className="h-3 w-3 text-cyan-600" />
                        {h}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Card Footer CTA */}
                <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-xs text-slate-500 font-medium">
                    Lịch trình gợi ý: 3N2Đ • 4N3Đ
                  </span>
                  <Link
                    href={ROUTES.tripNew}
                    className="inline-flex items-center gap-1 rounded-xl bg-cyan-50 px-3.5 py-1.5 text-xs font-bold text-cyan-700 hover:bg-cyan-500 hover:text-white transition-colors"
                  >
                    <span>Lập kế hoạch</span>
                    <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
