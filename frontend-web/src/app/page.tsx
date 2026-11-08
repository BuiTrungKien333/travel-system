import type { Metadata } from "next";
import { HeroSection } from "@/components/marketing/HeroSection";
import { CoreFeaturesSection } from "@/components/marketing/CoreFeaturesSection";
import { DetailedFeaturesShowcase } from "@/components/marketing/DetailedFeaturesShowcase";
import { CtaBanner } from "@/components/marketing/CtaBanner";

export const metadata: Metadata = {
  title: "WnGo — Lập kế hoạch thông minh · Trải nghiệm trọn vẹn",
  description:
    "Lên kế hoạch du lịch dễ dàng với AI tối ưu lịch trình, bản đồ tương tác Google Maps và nhật ký hành trình 63 tỉnh thành.",
  keywords: [
    "lập kế hoạch du lịch",
    "du lịch tự túc",
    "bản đồ du lịch",
    "AI travel planner",
    "WnGo",
    "lịch trình du lịch",
    "63 tỉnh thành Việt Nam",
  ],
};

export default function HomePage() {
  return (
    <div className="flex flex-col bg-background text-foreground">
      <HeroSection />
      <CoreFeaturesSection />
      <DetailedFeaturesShowcase />
      <CtaBanner />
    </div>
  );
}
