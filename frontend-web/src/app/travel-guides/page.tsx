import type { Metadata } from "next";
import { TravelGuidesView } from "@/components/guides/TravelGuidesView";

export const metadata: Metadata = {
  title: "Cẩm nang du lịch & Lịch trình mẫu — Tripzy",
  description:
    "Khám phá hàng trăm cẩm nang du lịch, kinh nghiệm phượt và lịch trình tự túc từ cộng đồng du khách tại Nhật Bản, Paris, Đà Nẵng, Phú Quốc và khắp thế giới.",
  keywords: [
    "cẩm nang du lịch",
    "travel guides",
    "lịch trình mẫu",
    "kinh nghiệm du lịch",
    "du lịch Nhật Bản",
    "du lịch Paris",
    "du lịch Đà Nẵng",
    "du lịch Việt Nam",
  ],
};

export default function TravelGuidesPage() {
  return <TravelGuidesView />;
}
