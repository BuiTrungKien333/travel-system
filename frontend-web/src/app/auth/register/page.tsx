import type { Metadata } from 'next';
import { RegisterForm } from '@/features/auth/components/RegisterForm';

export const metadata: Metadata = {
  title: 'Đăng ký — WnGo',
  description: 'Đăng ký tài khoản WnGo miễn phí để bắt đầu lên kế hoạch du lịch thông minh.',
};

export default function RegisterPage() {
  return (
    <>
      <div className="text-center mb-8">
        <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Tạo tài khoản</h2>
        <p className="text-sm text-slate-500 mt-2">Bắt đầu hành trình của bạn với WnGo</p>
      </div>
      <RegisterForm />
    </>
  );
}
