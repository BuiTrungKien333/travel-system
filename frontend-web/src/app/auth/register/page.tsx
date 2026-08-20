import type { Metadata } from 'next';
import { RegisterForm } from '@/features/auth/components/RegisterForm';

export const metadata: Metadata = {
  title: 'Đăng ký — Tripzy',
  description: 'Đăng ký tài khoản Tripzy miễn phí để bắt đầu lên kế hoạch du lịch thông minh.',
};

export default function RegisterPage() {
  return (
    <>
      <div className="text-center mb-6">
        <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Đăng ký tài khoản</h1>
        <p className="text-xs text-slate-500 mt-1">Bắt đầu lên kế hoạch và lưu giữ kỷ niệm cùng Tripzy</p>
      </div>
      <RegisterForm />
    </>
  );
}
