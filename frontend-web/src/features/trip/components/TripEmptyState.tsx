import Link from 'next/link';
import { Plus, Compass } from 'lucide-react';
import { ROUTES } from '@/lib/constants';

export function TripEmptyState() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center px-4">
      <div className="mb-4 rounded-full bg-muted p-5">
        <Compass className="h-10 w-10 text-muted-foreground" />
      </div>
      <h2 className="text-xl font-semibold">Bạn đang muốn đi đâu?</h2>
      <p className="mt-2 text-sm text-muted-foreground max-w-xs">
        Tạo chuyến đi đầu tiên và để AI giúp bạn lên kế hoạch hành trình.
      </p>
      <Link
        href={ROUTES.tripNew}
        className="mt-6 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition"
      >
        <Plus className="h-4 w-4" />
        Tạo chuyến đi
      </Link>
    </div>
  );
}
