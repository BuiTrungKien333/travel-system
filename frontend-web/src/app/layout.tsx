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
    default: "Tripzy — Lập kế hoạch hành trình du lịch cá nhân",
    template: "%s | Tripzy",
  },
  description:
    "Lập kế hoạch, trải nghiệm và lưu giữ hành trình du lịch cá nhân với AI hỗ trợ lịch trình và thời tiết.",
  keywords: ["du lịch", "lập kế hoạch", "hành trình", "AI", "Việt Nam"],
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <body className={`${inter.className} min-h-screen flex flex-col bg-background text-foreground`}>
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
