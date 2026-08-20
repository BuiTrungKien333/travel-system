"use client";

import { Compass } from "lucide-react";
import { ABOUT_VALUES_DATA } from "@/constants";

export function AboutSection() {
  return (
    <section id="about" className="py-20 lg:py-28 bg-gradient-to-b from-white via-cyan-50/20 to-sky-50/50 border-t border-slate-100">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          {/* Left Column: Story & Mission */}
          <div className="lg:col-span-6 space-y-6">
            <div className="inline-flex items-center gap-2 rounded-full border border-cyan-200 bg-cyan-50 px-3.5 py-1 text-xs font-semibold text-cyan-800">
              <Compass className="h-3.5 w-3.5 text-cyan-600" />
              <span>Về chúng tôi (About Us)</span>
            </div>

            <h2 className="text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl leading-tight">
              Sứ mệnh kết nối những{" "}
              <span className="text-cyan-600">tâm hồn mê xê dịch</span>
            </h2>

            <p className="text-base text-slate-600 leading-relaxed">
              Tripzy ra đời từ chính niềm đam mê khám phá vẻ đẹp bất tận của 63 tỉnh
              thành Việt Nam. Chúng tôi hiểu rằng việc lên lịch trình cho một chuyến
              đi tự túc thường mất nhiều thời gian, dễ gây tranh cãi khi đi theo
              nhóm và thiếu công cụ bản đồ trực quan.
            </p>

            <p className="text-base text-slate-600 leading-relaxed">
              Với Tripzy, chúng tôi kết hợp sức mạnh của <b>Trí tuệ nhân tạo (AI)</b>{" "}
              và <b>Bản đồ số tương tác</b> để biến mọi ý tưởng du lịch thành những
              hành trình trọn vẹn, đáng nhớ và thảnh thơi nhất.
            </p>

            <div className="pt-2 flex items-center gap-6">
              <div>
                <div className="text-2xl font-black text-slate-900">100%</div>
                <div className="text-xs text-slate-500 font-medium">Miễn phí cho du khách</div>
              </div>
              <div className="h-8 w-px bg-slate-200" />
              <div>
                <div className="text-2xl font-black text-slate-900">63/63</div>
                <div className="text-xs text-slate-500 font-medium">Tỉnh thành bao phủ</div>
              </div>
              <div className="h-8 w-px bg-slate-200" />
              <div>
                <div className="text-2xl font-black text-cyan-600">24/7</div>
                <div className="text-xs text-slate-500 font-medium">AI hỗ trợ lịch trình</div>
              </div>
            </div>
          </div>

          {/* Right Column: Core Values Grid */}
          <div className="lg:col-span-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
            {ABOUT_VALUES_DATA.map((item, idx) => {
              const Icon = item.icon;
              return (
                <div
                  key={idx}
                  className="rounded-2xl border border-slate-200/90 bg-white p-5 shadow-sm hover:border-cyan-300 hover:shadow-md transition"
                >
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-cyan-50 text-cyan-600 border border-cyan-100 mb-3">
                    <Icon className="h-5 w-5" />
                  </div>
                  <h4 className="text-sm font-bold text-slate-900 mb-1">
                    {item.title}
                  </h4>
                  <p className="text-xs text-slate-600 leading-relaxed">
                    {item.desc}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
}
