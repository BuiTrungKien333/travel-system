import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { ROUTES } from "@/constants";

export function CtaBanner() {
  return (
    <section className="bg-background py-16 lg:py-24">
      <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
        <div className="rounded-2xl border border-border bg-card p-8 sm:p-12 lg:p-16 text-center space-y-6">
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-foreground max-w-xl mx-auto leading-tight">
            Sẵn sàng cho chuyến đi tiếp theo của bạn?
          </h2>
          <p className="text-sm sm:text-base text-muted-foreground max-w-lg mx-auto leading-relaxed">
            Bắt đầu lên kế hoạch chỉ với vài thao tác. Tự do tùy chỉnh, tối ưu lộ trình và ghi lại từng kỷ niệm đáng nhớ.
          </p>
          <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
            <Link
              href={ROUTES.tripNew}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-lg bg-primary px-6 py-3.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors shadow-sm"
            >
              <span>Tạo chuyến đi mới</span>
              <ArrowRight className="h-4 w-4" />
            </Link>
            <Link
              href={ROUTES.trips}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-lg border border-border px-5 py-3.5 text-sm font-medium text-foreground hover:bg-muted/50 transition-colors"
            >
              <span>Xem các chuyến đi mẫu</span>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
