import type { Metadata } from "next";
import { HeroSection } from "@/components/marketing/HeroSection";
import { CoreFeaturesSection } from "@/components/marketing/CoreFeaturesSection";
import { SocialProofSection } from "@/components/marketing/SocialProofSection";
import { DetailedFeaturesShowcase } from "@/components/marketing/DetailedFeaturesShowcase";
import { PopularDestinationsSection } from "@/components/marketing/PopularDestinationsSection";
import { AboutSection } from "@/components/marketing/AboutSection";
import { FaqSection } from "@/components/marketing/FaqSection";
import { CtaBanner } from "@/components/marketing/CtaBanner";

export const metadata: Metadata = {
  title: "Tripzy — Ứng dụng lên kế hoạch du lịch thông minh & Bản đồ tương tác",
  description:
    "Lên kế hoạch du lịch tự túc dễ dàng với AI tối ưu lịch trình, tích hợp Google Maps, quản lý đặt phòng, vé máy bay và cộng tác nhóm theo thời gian thực.",
  keywords: [
    "lập kế hoạch du lịch",
    "du lịch tự túc",
    "bản đồ du lịch",
    "AI travel planner",
    "Wanderlog Việt Nam",
    "lịch trình du lịch Đà Nẵng",
    "lịch trình Phú Quốc",
    "63 tỉnh thành Việt Nam",
  ],
};

export default function HomePage() {
  return (
    <div className="flex flex-col bg-white text-slate-900 selection:bg-orange-500 selection:text-white">
      {/* 1. Khối Hero (Hero Section / Main Banner & App Preview Mockup) */}
      <HeroSection />

      {/* 2. Bảng tính năng cốt lõi (Core Features / Benefits Grid) */}
      <CoreFeaturesSection />

      {/* 3. Đánh giá từ người dùng & Báo chí (Social Proof / Testimonials) */}
      <SocialProofSection />

      {/* 4. Minh họa chi tiết chức năng (Detailed Feature Showcase - Z-Pattern) */}
      <DetailedFeaturesShowcase />

      {/* 5. Danh mục gợi ý & Điểm đến phổ biến (Popular Destinations / Travel Guides) */}
      <PopularDestinationsSection />

      {/* 6. Về chúng tôi (About Us) */}
      <AboutSection />

      {/* 7. Hỏi đáp thường gặp (FAQ) */}
      <FaqSection />

      {/* 8. Kêu gọi hành động cuối trang (Final CTA Banner) */}
      <CtaBanner />
    </div>
  );
}
