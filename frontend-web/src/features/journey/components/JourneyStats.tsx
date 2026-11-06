import { Compass, Image as ImageIcon, Luggage } from 'lucide-react';
import { VIETNAM_TOTAL_PROVINCES } from '@/lib/constants';
import type { JourneyStats } from '../types';

interface JourneyStatsProps {
  stats: JourneyStats;
}

export function JourneyStatsCard({ stats }: JourneyStatsProps) {
  const progress = stats.exploredProvinces.length / VIETNAM_TOTAL_PROVINCES;

  return (
    <div className="space-y-4">
      {/* Vietnam progress */}
      <div className="rounded-xl border bg-card p-5">
        <div className="flex items-center gap-2 mb-3">
          <span className="text-2xl" role="img" aria-label="Cờ Việt Nam">🇻🇳</span>
          <div>
            <p className="font-semibold">Hành trình Việt Nam</p>
            <p className="text-sm text-muted-foreground">
              {stats.exploredProvinces.length} / {VIETNAM_TOTAL_PROVINCES} tỉnh/thành
            </p>
          </div>
        </div>
        {/* Progress bar */}
        <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
          <div
            className="h-full rounded-full bg-primary transition-all duration-500"
            style={{ width: `${Math.round(progress * 100)}%` }}
            role="progressbar"
            aria-valuenow={stats.exploredProvinces.length}
            aria-valuemin={0}
            aria-valuemax={VIETNAM_TOTAL_PROVINCES}
            aria-label={`Đã khám phá ${stats.exploredProvinces.length} / ${VIETNAM_TOTAL_PROVINCES} tỉnh thành`}
          />
        </div>
        <p className="text-right text-xs text-muted-foreground mt-1">
          {Math.round(progress * 100)}%
        </p>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard icon={Luggage} label="Chuyến đi" value={stats.totalTrips} />
        <StatCard icon={Compass} label="Địa điểm" value={stats.totalPlaces} />
        <StatCard icon={ImageIcon} label="Kỷ niệm" value={stats.totalMemories} />
        <StatCard icon={ImageIcon} label="Ảnh" value={stats.totalPhotos} />
      </div>
    </div>
  );
}

function StatCard({
  icon: Icon,
  label,
  value,
}: {
  icon: React.ElementType;
  label: string;
  value: number;
}) {
  return (
    <div className="rounded-xl border bg-card p-4 text-center">
      <Icon className="h-5 w-5 text-muted-foreground mx-auto mb-2" />
      <p className="text-2xl font-bold">{value.toLocaleString('vi-VN')}</p>
      <p className="text-xs text-muted-foreground mt-0.5">{label}</p>
    </div>
  );
}
