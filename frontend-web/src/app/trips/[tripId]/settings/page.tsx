'use client';

import { use } from 'react';
import { useRouter } from 'next/navigation';
import { useTripQuery } from '@/features/trip/api/useTripQuery';
import { useDeleteTripMutation } from '@/features/trip/api/useDeleteTripMutation';
import { ROUTES } from '@/lib/constants';

export default function TripSettingsPage({
  params,
}: {
  params: Promise<{ tripId: string }>;
}) {
  const resolvedParams = use(params);
  const { tripId } = resolvedParams;
  const router = useRouter();
  const { data: trip } = useTripQuery(tripId);
  const deleteTripMutation = useDeleteTripMutation();

  const handleDelete = () => {
    if (!window.confirm('Bạn có chắc muốn xóa chuyến đi này?')) return;
    deleteTripMutation.mutate(tripId, {
      onSuccess: () => {
        router.push(ROUTES.trips);
      },
    });
  };

  if (!trip) return null;

  return (
    <div className="p-6 max-w-xl space-y-6">
      <h2 className="text-xl font-bold">Cài đặt chuyến đi</h2>

      <div className="rounded-xl border bg-card p-4 space-y-4">
        <h3 className="font-semibold text-sm">Vùng nguy hiểm</h3>
        <p className="text-xs text-muted-foreground">
          Xóa chuyến đi sẽ xóa toàn bộ lịch trình. Kỷ niệm đã lưu có thể vẫn được giữ lại trong dữ liệu cá nhân.
        </p>
        <button
          onClick={handleDelete}
          disabled={deleteTripMutation.isPending}
          className="rounded-md bg-destructive px-4 py-2 text-xs font-medium text-destructive-foreground hover:opacity-90 transition"
        >
          Xóa chuyến đi
        </button>
      </div>
    </div>
  );
}
