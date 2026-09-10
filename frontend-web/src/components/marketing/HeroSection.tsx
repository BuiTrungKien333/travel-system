"use client";

import Link from "next/link";
import Image from "next/image";
import { ArrowRight, Calendar, MapPin, Compass } from "lucide-react";
import { ROUTES } from "@/constants";

export function HeroSection() {
  return (
    <section className="relative overflow-hidden bg-background pt-10 pb-16 lg:pt-16 lg:pb-24 border-b border-border/60">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        
        {/* ─── Hero Header & Typography ─── */}
        <div className="mx-auto max-w-3xl text-center">
          <div className="inline-flex items-center gap-2 rounded-full border border-border bg-muted/40 px-3 py-1 text-xs font-medium text-muted-foreground mb-6">
            <Compass className="h-3.5 w-3.5 text-primary" />
            <span>Ứng dụng lập kế hoạch & nhật ký du lịch cá nhân</span>
          </div>

          <h1 className="text-4xl font-bold tracking-tight text-foreground sm:text-5xl lg:text-6xl leading-[1.12]">
            Plan your journey.
            <br />
            <span className="text-primary font-serif font-normal italic">
              Make every place memorable.
            </span>
          </h1>

          <p className="mt-5 text-base sm:text-lg text-muted-foreground max-w-2xl mx-auto leading-relaxed">
            Sắp xếp lịch trình tối ưu với AI, khám phá cùng bản đồ tương tác
            và lưu giữ từng khoảnh khắc trên 63 tỉnh thành Việt Nam.
          </p>

          <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4">
            <Link
              href={ROUTES.tripNew}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-lg bg-primary px-6 py-3.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors shadow-sm"
            >
              <span>Lên kế hoạch chuyến đi</span>
              <ArrowRight className="h-4 w-4" />
            </Link>
            <Link
              href={ROUTES.journey}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-lg border border-border bg-background px-5 py-3.5 text-sm font-medium text-foreground hover:bg-muted/50 transition-colors"
            >
              <span>Khám phá 63 tỉnh thành</span>
            </Link>
          </div>
        </div>

        {/* ─── Photography-First Destination Collage ─── */}
        <div className="mt-14 lg:mt-18">
          <div className="grid grid-cols-1 md:grid-cols-12 gap-4 max-w-6xl mx-auto">
            
            {/* Featured Left Photo */}
            <div className="md:col-span-7 relative h-72 sm:h-96 rounded-xl overflow-hidden group">
              <Image
                src="https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1000&auto=format&fit=crop&q=80"
                alt="Bãi biển Mỹ Khê, Đà Nẵng"
                fill
                priority
                className="object-cover transition-transform duration-700 group-hover:scale-105"
                sizes="(max-width: 768px) 100vw, 60vw"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/10 to-transparent" />
              <div className="absolute bottom-5 left-5 right-5 text-white">
                <span className="text-xs uppercase tracking-wider text-white/80 font-medium">Chuyến đi mẫu</span>
                <h3 className="text-xl sm:text-2xl font-bold mt-1">Đà Nẵng & Hội An: Biển xanh & Phố cổ</h3>
                <div className="flex items-center gap-3 text-xs text-white/80 mt-2">
                  <span className="flex items-center gap-1"><MapPin className="h-3.5 w-3.5" /> 8 địa điểm</span>
                  <span>·</span>
                  <span className="flex items-center gap-1"><Calendar className="h-3.5 w-3.5" /> 4 ngày 3 đêm</span>
                </div>
              </div>
            </div>

            {/* Right Side 2 Photos */}
            <div className="md:col-span-5 grid grid-cols-2 md:grid-cols-1 gap-4">
              
              {/* Photo 2: Hoi An */}
              <div className="relative h-44 sm:h-46 rounded-xl overflow-hidden group">
                <Image
                  src="https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=600&auto=format&fit=crop&q=80"
                  alt="Phố cổ Hội An"
                  fill
                  className="object-cover transition-transform duration-700 group-hover:scale-105"
                  sizes="(max-width: 768px) 50vw, 40vw"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent" />
                <div className="absolute bottom-3 left-4 text-white">
                  <p className="text-sm font-semibold">Chợ đêm & Đèn lồng Hội An</p>
                  <p className="text-[11px] text-white/70">Quảng Nam</p>
                </div>
              </div>

              {/* Photo 3: Ba Na Hills */}
              <div className="relative h-44 sm:h-46 rounded-xl overflow-hidden group">
                <Image
                  src="https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=600&auto=format&fit=crop&q=80"
                  alt="Cầu Vàng Bà Nà Hills"
                  fill
                  className="object-cover transition-transform duration-700 group-hover:scale-105"
                  sizes="(max-width: 768px) 50vw, 40vw"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent" />
                <div className="absolute bottom-3 left-4 text-white">
                  <p className="text-sm font-semibold">Cầu Vàng Bà Nà</p>
                  <p className="text-[11px] text-white/70">Đà Nẵng</p>
                </div>
              </div>

            </div>

          </div>
        </div>

      </div>
    </section>
  );
}
