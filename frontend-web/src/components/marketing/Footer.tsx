"use client";

import Link from "next/link";
import { Compass, Sparkles, Heart } from "lucide-react";
import { ROUTES } from "@/lib/constants";

export function Footer() {
  return (
    <footer className="bg-slate-950 text-slate-400 text-sm border-t border-slate-800">
      {/* Main Footer Content */}
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 pt-16 pb-12">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-10 lg:gap-8">
          {/* Brand & Mission column */}
          <div className="lg:col-span-4 space-y-4">
            <Link href={ROUTES.home} className="flex items-center gap-2.5 group">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-orange-600 to-amber-500 text-white shadow-md">
                <Compass className="h-5 w-5" />
              </div>
              <div className="flex flex-col">
                <span className="font-extrabold text-xl tracking-tight text-white">
                  Tripzy
                </span>
                <span className="text-[10px] font-semibold uppercase tracking-wider text-orange-500 -mt-1 flex items-center gap-1">
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
              <div className="flex max-w-sm rounded-xl bg-slate-900 border border-slate-800 p-1 focus-within:border-orange-500 transition">
                <input
                  type="email"
                  placeholder="Email của bạn..."
                  className="w-full bg-transparent px-3 py-2 text-xs text-white placeholder:text-slate-500 focus:outline-none"
                />
                <button
                  type="button"
                  className="rounded-lg bg-orange-600 px-3.5 py-2 text-xs font-bold text-white hover:bg-orange-500 transition shrink-0"
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
              <li>
                <a href="#features" className="hover:text-orange-400 transition">
                  Lập lịch trình AI
                </a>
              </li>
              <li>
                <a href="#showcase" className="hover:text-orange-400 transition">
                  Tối ưu lộ trình Held-Karp
                </a>
              </li>
              <li>
                <a href="#showcase" className="hover:text-orange-400 transition">
                  Quản lý đặt phòng & vé
                </a>
              </li>
              <li>
                <a href="#showcase" className="hover:text-orange-400 transition">
                  Cộng tác nhóm trực tiếp
                </a>
              </li>
              <li>
                <a href="#showcase" className="hover:text-orange-400 transition">
                  Chế độ ngoại tuyến & PDF
                </a>
              </li>
              <li>
                <Link href={ROUTES.journey} className="hover:text-orange-400 transition">
                  Bản đồ 63 tỉnh thành
                </Link>
              </li>
            </ul>
          </div>

          {/* Destinations */}
          <div className="lg:col-span-2">
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-4">
              Điểm đến nổi bật
            </h4>
            <ul className="space-y-2.5 text-xs">
              <li>
                <a href="#destinations" className="hover:text-orange-400 transition">
                  Đà Nẵng & Hội An
                </a>
              </li>
              <li>
                <a href="#destinations" className="hover:text-orange-400 transition">
                  Phú Quốc
                </a>
              </li>
              <li>
                <a href="#destinations" className="hover:text-orange-400 transition">
                  Hà Nội & Ninh Bình
                </a>
              </li>
              <li>
                <a href="#destinations" className="hover:text-orange-400 transition">
                  Sa Pa & Hà Giang
                </a>
              </li>
              <li>
                <a href="#destinations" className="hover:text-orange-400 transition">
                  Đà Lạt & Nha Trang
                </a>
              </li>
              <li>
                <a href="#destinations" className="hover:text-orange-400 transition">
                  Quy Nhơn & Phú Yên
                </a>
              </li>
            </ul>
          </div>

          {/* Resources */}
          <div className="lg:col-span-2">
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-4">
              Cẩm nang & Hỗ trợ
            </h4>
            <ul className="space-y-2.5 text-xs">
              <li>
                <a href="#testimonials" className="hover:text-orange-400 transition">
                  Đánh giá từ du khách
                </a>
              </li>
              <li>
                <a href="#faq" className="hover:text-orange-400 transition">
                  Câu hỏi thường gặp
                </a>
              </li>
              <li>
                <a href="#faq" className="hover:text-orange-400 transition">
                  Hướng dẫn sử dụng
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-orange-400 transition">
                  Kinh nghiệm du lịch tự túc
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-orange-400 transition">
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
                <a href="#" className="hover:text-orange-400 transition">
                  Điều khoản dịch vụ
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-orange-400 transition">
                  Chính sách quyền riêng tư
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-orange-400 transition">
                  Chính sách cookie
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-orange-400 transition">
                  Tiêu chuẩn cộng đồng
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-orange-400 transition">
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
