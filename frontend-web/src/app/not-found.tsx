import Link from 'next/link';
import { Compass, ArrowLeft } from 'lucide-react';
import { ROUTES } from '@/lib/constants';

export default function NotFound() {
  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-orange-100 text-orange-600 mb-6 shadow-md shadow-orange-500/10">
        <Compass className="h-8 w-8 animate-spin" style={{ animationDuration: '8s' }} />
      </div>
      <h1 className="text-4xl font-extrabold text-slate-900 tracking-tight">404</h1>
      <h2 className="text-xl font-semibold text-slate-700 mt-2">Không tìm thấy trang</h2>
      <p className="text-sm text-slate-500 max-w-md mt-2">
        Trang bạn đang tìm kiếm không tồn tại hoặc đã được chuyển sang địa chỉ khác.
      </p>
      <div className="mt-6 flex items-center gap-3">
        <Link
          href={ROUTES.home}
          className="inline-flex items-center gap-2 rounded-xl bg-orange-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-orange-700 transition"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Về trang chủ</span>
        </Link>
        <Link
          href={ROUTES.travelGuides}
          className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition"
        >
          <span>Khám phá Cẩm nang</span>
        </Link>
      </div>
    </div>
  );
}
