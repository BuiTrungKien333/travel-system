import Link from 'next/link';
import { Compass, Plus } from 'lucide-react';
import { ROUTES } from '@/constants';

export function TripEmptyState() {
  return (
    <div className="py-16 px-6 text-center max-w-md mx-auto space-y-5">
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-primary/10 text-primary">
        <Compass className="h-7 w-7" />
      </div>

      <div className="space-y-2">
        <h3 className="text-xl font-bold text-foreground tracking-tight">
          Bạn chưa có chuyến đi nào
        </h3>
        <p className="text-sm text-muted-foreground leading-relaxed">
          Hãy bắt đầu tạo hành trình đầu tiên. AI sẽ hỗ trợ bạn sắp xếp thứ tự địa điểm tối ưu nhất.
        </p>
      </div>

      <div className="pt-2">
        <Link
          href={ROUTES.tripNew}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors shadow-sm"
        >
          <Plus className="h-4 w-4" />
          <span>Tạo chuyến đi đầu tiên</span>
        </Link>
      </div>
    </div>
  );
}
