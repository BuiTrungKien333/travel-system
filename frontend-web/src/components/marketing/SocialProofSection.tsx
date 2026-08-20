"use client";

import { Star, Heart } from "lucide-react";
import { TESTIMONIAL_REVIEWS, PRESS_LOGOS_DATA } from "@/constants";
import { SectionHeading } from "@/components/ui";

export function SocialProofSection() {
  return (
    <section
      id="testimonials"
      className="py-20 lg:py-28 bg-gradient-to-b from-slate-50/70 via-cyan-50/20 to-white"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <SectionHeading
          badgeIcon={Heart}
          badgeText="Được yêu thích bởi hơn 500,000+ du khách"
          title="Du khách nói gì về"
          highlight="Tripzy"
          subtitle="Khám phá những trải nghiệm thực tế từ những người đã dùng Tripzy để biến mọi chuyến đi thành kỷ niệm đáng nhớ."
        />

        {/* Testimonials Grid */}
        <div className="mt-14 grid grid-cols-1 gap-6 lg:grid-cols-3">
          {TESTIMONIAL_REVIEWS.map((review, idx) => (
            <div
              key={idx}
              className="relative flex flex-col justify-between rounded-3xl border border-cyan-100/80 bg-white p-7 shadow-md shadow-slate-100 hover:shadow-xl hover:border-cyan-200 transition-all duration-300"
            >
              <div>
                {/* Quote Icon & Tag */}
                <div className="flex items-center justify-between">
                  <div className="flex text-amber-400">
                    {[...Array(review.rating)].map((_, i) => (
                      <Star key={i} className="h-4 w-4 fill-amber-400" />
                    ))}
                  </div>
                  <span className="rounded-full bg-cyan-50 px-2.5 py-0.5 text-[11px] font-bold text-cyan-700 border border-cyan-100">
                    {review.tag}
                  </span>
                </div>

                {/* Title */}
                <h3 className="mt-4 text-base font-bold text-slate-900 leading-snug">
                  &ldquo;{review.title}&rdquo;
                </h3>

                {/* Content */}
                <p className="mt-3 text-sm text-slate-600 leading-relaxed italic">
                  &ldquo;{review.content}&rdquo;
                </p>
              </div>

              {/* Author */}
              <div className="mt-6 pt-5 border-t border-slate-100 flex items-center gap-3">
                <img
                  src={review.avatar}
                  alt={review.name}
                  className="h-11 w-11 rounded-full object-cover border border-slate-200"
                />
                <div>
                  <h4 className="text-sm font-bold text-slate-900">
                    {review.name}
                  </h4>
                  <p className="text-xs text-slate-500">{review.role}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Press / Media Section */}
        <div className="mt-20 border-t border-slate-200/80 pt-12">
          <p className="text-center text-xs font-bold uppercase tracking-wider text-slate-600 mb-8">
            Được đưa tin và đánh giá cao trên các phương tiện truyền thông
          </p>

          <div className="grid grid-cols-2 md:grid-cols-5 gap-6 items-center">
            {PRESS_LOGOS_DATA.map((press, i) => (
              <div
                key={i}
                className="flex flex-col items-center justify-center p-4 rounded-xl bg-white border border-slate-200/70 text-center shadow-sm hover:border-cyan-300 transition"
              >
                <span className="text-base font-black tracking-tight text-slate-800">
                  {press.name}
                </span>
                <span className="text-[11px] text-slate-600 mt-1 line-clamp-1">
                  {press.desc}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
