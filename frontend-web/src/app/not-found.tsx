import Link from 'next/link';
import { Compass, ArrowLeft, Search } from 'lucide-react';
import { ROUTES } from '@/constants';

export default function NotFound() {
  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4 bg-background">
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted text-primary mb-6">
        <Compass className="h-8 w-8" strokeWidth={1.5} />
      </div>
      <h1 className="text-4xl sm:text-5xl font-bold text-foreground tracking-tight mb-2">404</h1>
      <h2 className="text-lg font-semibold text-foreground mb-2">Điểm dừng chân không tồn tại</h2>
      <p className="text-xs sm:text-sm text-muted-foreground max-w-sm mb-8 leading-relaxed">
        Trang bạn đang tìm kiếm có thể đã được di chuyển hoặc không còn tồn tại. Hãy quay lại hành trình của bạn.
      </p>
      <div className="flex flex-col sm:flex-row items-center gap-3">
        <Link
          href={ROUTES.home}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-xs font-semibold text-primary-foreground hover:bg-primary/90 transition-colors shadow-xs"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Về trang chủ</span>
        </Link>
        <Link
          href={ROUTES.trips}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-lg border border-border bg-background px-5 py-2.5 text-xs font-medium text-foreground hover:bg-muted/50 transition-colors"
        >
          <Search className="h-4 w-4" />
          <span>Danh sách chuyến đi</span>
        </Link>
      </div>
    </div>
  );
}
