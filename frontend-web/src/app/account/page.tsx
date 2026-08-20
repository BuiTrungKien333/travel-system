'use client';

import Link from 'next/link';
import { ArrowLeft, LogOut, Mail, Calendar, ShieldCheck } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { useLogoutMutation } from '@/features/auth/api/useLogoutMutation';
import { ROUTES } from '@/lib/constants';
import { UserAvatar } from '@/components/ui/UserAvatar';

export default function AccountPage() {
  const { user } = useAuth();
  const logoutMutation = useLogoutMutation();

  const handleLogout = () => {
    logoutMutation.mutate();
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-sky-50/50 via-cyan-50/20 to-white">
      <header className="border-b border-cyan-100 bg-white/90 backdrop-blur-sm px-4 h-14 flex items-center gap-3">
        <Link
          href={ROUTES.trips}
          className="p-1.5 rounded-lg hover:bg-slate-100 transition-colors"
          aria-label="Quay lại"
        >
          <ArrowLeft className="h-4 w-4 text-slate-600" />
        </Link>
        <h1 className="font-bold text-sm text-slate-800">Tài khoản & Hồ sơ</h1>
      </header>

      <main className="mx-auto max-w-xl px-4 py-10 space-y-6">
        <div className="rounded-3xl border border-cyan-100 bg-white p-7 shadow-xl shadow-cyan-950/5 space-y-6">
          <div className="flex items-center gap-4">
            <UserAvatar size="lg" className="h-16 w-16" alt={user?.fullName} />
            <div>
              <h2 className="font-extrabold text-xl text-slate-900">{user?.fullName ?? 'Người dùng Tripzy'}</h2>
              <p className="text-xs text-slate-500 flex items-center gap-1.5 mt-0.5">
                <Mail className="h-3.5 w-3.5 text-cyan-600" />
                <span>{user?.email ?? 'user@example.com'}</span>
              </p>
            </div>
          </div>

          <div className="rounded-2xl bg-slate-50 border border-slate-200/80 p-4 space-y-3 text-xs">
            <div className="flex items-center justify-between text-slate-600">
              <span className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-cyan-600" />
                <span>Trạng thái tài khoản:</span>
              </span>
              <span className="font-bold text-emerald-600 bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-200">
                Đã xác thực
              </span>
            </div>
            <div className="flex items-center justify-between text-slate-600">
              <span className="flex items-center gap-2">
                <Calendar className="h-4 w-4 text-cyan-600" />
                <span>Ngày tham gia:</span>
              </span>
              <span className="font-semibold text-slate-800">
                {user?.createdAt ? new Date(user.createdAt).toLocaleDateString('vi-VN') : '2026'}
              </span>
            </div>
          </div>

          <div className="border-t border-slate-100 pt-4 flex justify-between items-center">
            <Link
              href={ROUTES.trips}
              className="text-xs font-semibold text-cyan-600 hover:text-cyan-700 hover:underline"
            >
              ← Quay lại Chuyến đi của tôi
            </Link>
            <button
              onClick={handleLogout}
              className="inline-flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-xs font-bold text-red-600 hover:bg-red-100 transition"
            >
              <LogOut className="h-4 w-4" />
              <span>Đăng xuất</span>
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
