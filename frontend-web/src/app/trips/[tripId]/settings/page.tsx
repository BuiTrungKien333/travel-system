'use client';

import { use } from 'react';
import { useRouter } from 'next/navigation';
import { useTripQuery } from '@/features/trip/api/useTripQuery';
import { useDeleteTripMutation } from '@/features/trip/api/useDeleteTripMutation';
import { ROUTES } from '@/constants';
import { AlertTriangle, Trash2 } from 'lucide-react';

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
    if (!window.confirm('Bạn có chắc muốn xóa chuyến đi này? Hành động này không thể hoàn tác.')) return;
    deleteTripMutation.mutate(tripId, {
      onSuccess: () => {
        router.push(ROUTES.trips);
      },
    });
  };

  if (!trip) return null;

  return (
    <div className="p-4 sm:p-8 max-w-3xl mx-auto space-y-6">
      <div className="pb-4 border-b border-border/60">
        <h2 className="text-xl font-bold tracking-tight text-foreground">Cài đặt chuyến đi</h2>
        <p className="text-xs text-muted-foreground mt-0.5">Quản lý thông tin và dữ liệu chuyến đi</p>
      </div>

      {/* General Settings */}
      <div className="rounded-xl border border-border bg-card p-5 space-y-3 shadow-xs">
        <h3 className="text-sm font-semibold text-foreground">Thông tin cơ bản</h3>
        <div className="space-y-2 text-xs text-muted-foreground">
          <p><strong className="text-foreground">Tên:</strong> {trip.name}</p>
          <p><strong className="text-foreground">Điểm đến:</strong> {trip.destination}</p>
          <p><strong className="text-foreground">Thời gian:</strong> {trip.startDate} đến {trip.endDate}</p>
        </div>
      </div>

      {/* Danger Zone */}
      <div className="rounded-xl border border-destructive/20 bg-destructive/5 p-5 space-y-3">
        <div className="flex items-start gap-3">
          <div className="rounded-md bg-destructive/10 p-2 text-destructive shrink-0 mt-0.5">
            <AlertTriangle className="h-4 w-4" />
          </div>
          <div className="space-y-1">
            <h3 className="text-sm font-semibold text-destructive">Xóa chuyến đi</h3>
            <p className="text-xs text-muted-foreground leading-relaxed">
              Hành động này sẽ xóa vĩnh viễn chuyến đi, lịch trình và danh sách các địa điểm đã lưu. Các kỷ niệm ảnh chụp vẫn được lưu trữ an toàn trong tài khoản của bạn.
            </p>
            <div className="pt-3">
              <button
                onClick={handleDelete}
                disabled={deleteTripMutation.isPending}
                className="inline-flex items-center gap-1.5 rounded-lg bg-destructive px-3.5 py-2 text-xs font-semibold text-destructive-foreground hover:bg-destructive/90 transition-colors disabled:opacity-50 shadow-xs"
              >
                <Trash2 className="h-3.5 w-3.5" />
                <span>{deleteTripMutation.isPending ? 'Đang xóa...' : 'Xóa chuyến đi này'}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
