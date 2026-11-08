import type { Metadata } from 'next';
import { LoginForm } from '@/features/auth/components/LoginForm';

export const metadata: Metadata = {
  title: 'Đăng nhập — WnGo',
  description: 'Đăng nhập tài khoản WnGo để khám phá và quản lý hành trình du lịch.',
};

export default function LoginPage() {
  return (
    <>
      <div className="text-center mb-8">
        <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Chào mừng trở lại</h2>
        <p className="text-sm text-slate-500 mt-2">Vui lòng đăng nhập vào tài khoản của bạn</p>
      </div>
      <LoginForm />
    </>
  );
}
