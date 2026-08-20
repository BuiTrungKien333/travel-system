import type { Metadata } from 'next';
import { LoginForm } from '@/features/auth/components/LoginForm';

export const metadata: Metadata = {
  title: 'Đăng nhập — Tripzy',
  description: 'Đăng nhập tài khoản Tripzy để khám phá và quản lý hành trình du lịch.',
};

export default function LoginPage() {
  return (
    <>
      <div className="text-center mb-6">
        <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Đăng nhập tài khoản</h1>
        <p className="text-xs text-slate-500 mt-1">Chào mừng bạn quay trở lại với Tripzy</p>
      </div>
      <LoginForm />
    </>
  );
}
