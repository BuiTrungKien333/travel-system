"use client";

import Link from "next/link";
import {
  Compass,
  Sparkles,
  Heart,
} from "lucide-react";
import { ROUTES, FOOTER_PRODUCT_LINKS } from "@/constants";

export function Footer() {
  return (
    <footer id="contact" className="bg-slate-950 text-slate-400 text-sm border-t border-slate-800">
      {/* Main Footer Content */}
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 pt-16 pb-12">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-10 lg:gap-8">
          {/* Brand & Mission column */}
          <div className="lg:col-span-4 space-y-4">
            <Link href={ROUTES.home} className="flex items-center gap-2.5 group">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-cyan-500 via-teal-500 to-sky-400 text-white shadow-md shadow-cyan-500/25">
                <Compass className="h-5 w-5" />
              </div>
              <div className="flex flex-col">
                <span className="font-extrabold text-xl tracking-tight text-white">
                  Tripzy
                </span>
                <span className="text-[10px] font-semibold uppercase tracking-wider text-cyan-400 -mt-1 flex items-center gap-1">
                  <Sparkles className="h-2.5 w-2.5" /> AI Travel Platform
                </span>
              </div>
            </Link>

            <p className="text-slate-400 text-sm leading-relaxed max-w-sm">
              Nền tảng lên kế hoạch du lịch thông minh, tối ưu tuyến đường bằng AI,
              tích hợp bản đồ trực quan và giúp bạn lưu giữ từng khoảnh khắc đẹp
              nhất trên 63 tỉnh thành Việt Nam.
            </p>

            {/* Newsletter input */}
            <div className="pt-2">
              <label className="block text-xs font-semibold text-slate-300 mb-2">
                Nhận cẩm nang du lịch & ưu đãi mới nhất
              </label>
              <div className="flex max-w-sm rounded-xl bg-slate-900 border border-slate-800 p-1 focus-within:border-cyan-500 transition">
                <input
                  type="email"
                  placeholder="Email của bạn..."
                  className="w-full bg-transparent px-3 py-2 text-xs text-white placeholder:text-slate-500 focus:outline-none"
                />
                <button
                  type="button"
                  className="rounded-lg bg-cyan-500 px-3.5 py-2 text-xs font-bold text-white hover:bg-cyan-600 transition shrink-0"
                >
                  Đăng ký
                </button>
              </div>
            </div>
          </div>

          {/* Product Links */}
          <div className="lg:col-span-2">
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-4">
              Sản phẩm
            </h4>
            <ul className="space-y-2.5 text-xs">
              {FOOTER_PRODUCT_LINKS.map((item, idx) => (
                <li key={idx}>
                  <Link href={item.href} className="hover:text-cyan-400 transition">
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Destinations / Travel Guides */}
          <div className="lg:col-span-2">
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-4">
              Cẩm nang điểm đến
            </h4>
            <ul className="space-y-2.5 text-xs">
              <li>
                <Link href={ROUTES.travelGuides} className="text-cyan-400 font-semibold hover:text-cyan-300 transition">
                  ✨ Khám phá tất cả cẩm nang
                </Link>
              </li>
              <li>
                <Link href="/#destinations" className="hover:text-cyan-400 transition">
                  Đà Nẵng & Hội An
                </Link>
              </li>
              <li>
                <Link href="/#destinations" className="hover:text-cyan-400 transition">
                  Phú Quốc
                </Link>
              </li>
              <li>
                <Link href="/#destinations" className="hover:text-cyan-400 transition">
                  Hà Nội & Ninh Bình
                </Link>
              </li>
              <li>
                <Link href="/#destinations" className="hover:text-cyan-400 transition">
                  Sa Pa & Hà Giang
                </Link>
              </li>
              <li>
                <Link href="/#destinations" className="hover:text-cyan-400 transition">
                  Đà Lạt & Nha Trang
                </Link>
              </li>
              <li>
                <Link href="/#destinations" className="hover:text-cyan-400 transition">
                  Quy Nhơn & Phú Yên
                </Link>
              </li>
            </ul>
          </div>

          {/* Resources & Support */}
          <div className="lg:col-span-2">
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-4">
              Hỗ trợ & Đánh giá
            </h4>
            <ul className="space-y-2.5 text-xs">
              <li>
                <Link href="/#testimonials" className="hover:text-cyan-400 transition">
                  Đánh giá từ du khách
                </Link>
              </li>
              <li>
                <Link href="/#faq" className="hover:text-cyan-400 transition">
                  Câu hỏi thường gặp
                </Link>
              </li>
              <li>
                <Link href="/#faq" className="hover:text-cyan-400 transition">
                  Hướng dẫn sử dụng
                </Link>
              </li>
              <li>
                <Link href="/#about" className="hover:text-cyan-400 transition">
                  Về chúng tôi (About Us)
                </Link>
              </li>
              <li>
                <a href="mailto:support@tripzy.vn" className="hover:text-cyan-400 transition">
                  Liên hệ hỗ trợ 24/7
                </a>
              </li>
            </ul>
          </div>

          {/* Legal & Policy */}
          <div className="lg:col-span-2">
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-4">
              Pháp lý & Chính sách
            </h4>
            <ul className="space-y-2.5 text-xs">
              <li>
                <a href="#" className="hover:text-cyan-400 transition">
                  Điều khoản dịch vụ
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-cyan-400 transition">
                  Chính sách quyền riêng tư
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-cyan-400 transition">
                  Chính sách cookie
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-cyan-400 transition">
                  Tiêu chuẩn cộng đồng
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-cyan-400 transition">
                  Bảo mật dữ liệu
                </a>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="mt-12 pt-8 border-t border-slate-800/80 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500">
          <p>
            © {new Date().getFullYear()} Tripzy Inc. Tất cả quyền được bảo lưu.
          </p>

          <div className="flex items-center gap-1 text-slate-400">
            <span>Được xây dựng với</span>
            <Heart className="h-3.5 w-3.5 fill-rose-500 text-rose-500 inline mx-0.5" />
            <span>dành cho cộng đồng yêu du lịch Việt Nam & Thế giới.</span>
          </div>

          <div className="flex items-center gap-4 text-slate-400">
            <a href="#" className="hover:text-white transition">
              Facebook
            </a>
            <span>•</span>
            <a href="#" className="hover:text-white transition">
              Instagram
            </a>
            <span>•</span>
            <a href="#" className="hover:text-white transition">
              TikTok
            </a>
            <span>•</span>
            <a href="#" className="hover:text-white transition">
              YouTube
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
