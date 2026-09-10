import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "@/styles/globals.css";
import { Providers } from "./providers";
import { Header, Footer } from "@/components/layout";

const inter = Inter({
  subsets: ["latin", "vietnamese"],
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "WnGo — Lập kế hoạch thông minh · Trải nghiệm trọn vẹn",
    template: "%s | WnGo",
  },
  description:
    "Hệ thống hỗ trợ lập kế hoạch và quản lý hành trình du lịch cá nhân tích hợp bản đồ tương tác và nhật ký trải nghiệm.",
  keywords: ["du lịch", "lập kế hoạch", "hành trình", "WnGo", "Việt Nam", "travel planner"],
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <body className={`${inter.className} min-h-screen flex flex-col bg-background text-foreground antialiased`}>
        <Providers>
          <Header />
          <div className="flex-1 flex flex-col">
            {children}
          </div>
          <Footer />
        </Providers>
      </body>
    </html>
  );
}
