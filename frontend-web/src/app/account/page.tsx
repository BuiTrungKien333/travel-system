'use client';

import Link from 'next/link';
import { ArrowLeft, LogOut, Mail, Calendar, ShieldCheck, User, Settings, Bell } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { useLogoutMutation } from '@/features/auth/api/useLogoutMutation';
import { ROUTES } from '@/constants';
import { UserAvatar } from '@/components/ui/UserAvatar';

export default function AccountPage() {
  const { user } = useAuth();
  const logoutMutation = useLogoutMutation();

  const handleLogout = () => {
    logoutMutation.mutate();
  };

  return (
    <div className="min-h-screen bg-background pb-20">
      <main className="mx-auto max-w-2xl px-4 sm:px-6 py-8 sm:py-12 space-y-8">

        {/* Navigation back & Title */}
        <div className="space-y-3">
          <Link
            href={ROUTES.trips}
            className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Quay lại danh sách chuyến đi</span>
          </Link>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
            Hồ sơ tài khoản
          </h1>
        </div>

        {/* Profile Card */}
        <div className="rounded-xl border border-border bg-card p-6 flex flex-col sm:flex-row items-center sm:items-start gap-5 text-center sm:text-left shadow-xs">
          <UserAvatar size="lg" className="h-20 w-20 border border-border" alt={user?.fullName} />

          <div className="flex-1 min-w-0">
            <h2 className="font-bold text-xl text-foreground">{user?.fullName ?? 'Thành viên WnGo'}</h2>
            <p className="text-xs text-muted-foreground mt-1 flex items-center justify-center sm:justify-start gap-1.5">
              <Mail className="h-3.5 w-3.5" />
              <span>{user?.email ?? 'user@example.com'}</span>
            </p>

            <div className="mt-3 inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 text-xs font-medium border border-emerald-500/20">
              <ShieldCheck className="h-3.5 w-3.5" />
              <span>Đã xác thực tài khoản</span>
            </div>
          </div>
        </div>

        {/* Settings Menu */}
        <div className="space-y-3">
          <h3 className="font-semibold text-xs uppercase tracking-wider text-muted-foreground">
            Cài đặt & Tùy chọn
          </h3>

          <div className="rounded-xl border border-border bg-card divide-y divide-border/60 shadow-xs">
            <SettingsItem icon={<User className="h-4 w-4" />} label="Thông tin cá nhân & Liên hệ" />
            <SettingsItem icon={<Bell className="h-4 w-4" />} label="Thông báo chuyến đi" />
            <SettingsItem icon={<Settings className="h-4 w-4" />} label="Cài đặt hệ thống" />
          </div>
        </div>

        {/* Metadata */}
        <div className="space-y-3">
          <h3 className="font-semibold text-xs uppercase tracking-wider text-muted-foreground">
            Thông tin khác
          </h3>

          <div className="rounded-xl border border-border bg-card p-4 text-xs space-y-2.5 shadow-xs">
            <div className="flex items-center justify-between text-muted-foreground">
              <span className="flex items-center gap-2">
                <Calendar className="h-4 w-4" />
                <span>Ngày tham gia</span>
              </span>
              <span className="text-foreground font-medium">
                {user?.createdAt ? new Date(user.createdAt).toLocaleDateString('vi-VN') : '2026'}
              </span>
            </div>
          </div>
        </div>

        {/* Logout */}
        <div className="pt-2 flex justify-center sm:justify-start">
          <button
            onClick={handleLogout}
            className="inline-flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold text-destructive hover:bg-destructive/10 transition-colors"
          >
            <LogOut className="h-4 w-4" />
            <span>Đăng xuất tài khoản</span>
          </button>
        </div>

      </main>
    </div>
  );
}

function SettingsItem({ icon, label }: { icon: React.ReactNode; label: string }) {
  return (
    <button className="w-full flex items-center justify-between p-3.5 text-left hover:bg-muted/30 transition-colors group">
      <div className="flex items-center gap-3 text-foreground">
        <div className="text-muted-foreground group-hover:text-primary transition-colors">{icon}</div>
        <span className="text-xs font-medium">{label}</span>
      </div>
      <span className="text-muted-foreground/40 text-xs">→</span>
    </button>
  );
}
