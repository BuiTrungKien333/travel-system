'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Loader2, User, Mail, Lock, Eye, EyeOff, AlertCircle } from 'lucide-react';
import { registerSchema, type RegisterInput } from '../schema';
import { useRegisterMutation } from '../api/useRegisterMutation';
import { ROUTES } from '@/constants';
import { FormInput } from '@/components/ui';

export function RegisterForm() {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const router = useRouter();
  const registerMutation = useRegisterMutation();

  const {
    register,
    handleSubmit,
    formState: { errors, touchedFields },
  } = useForm<RegisterInput>({
    resolver: zodResolver(registerSchema),
    mode: 'onTouched',
  });

  const onSubmit = (data: RegisterInput) => {
    registerMutation.mutate(data, {
      onSuccess: () => router.push(ROUTES.trips),
    });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
      {registerMutation.error && (
        <div className="rounded-xl bg-red-50 border border-red-200 p-3 text-xs text-red-700 flex items-start gap-2 animate-fade-in">
          <AlertCircle className="h-4 w-4 shrink-0 text-red-500 mt-0.5" />
          <span>{registerMutation.error.message || 'Đăng ký không thành công. Vui lòng thử lại.'}</span>
        </div>
      )}

      <FormInput
        id="fullName"
        type="text"
        label="Họ và tên"
        required
        icon={User}
        placeholder="Nguyễn Văn A"
        autoComplete="name"
        error={errors.fullName?.message}
        isTouched={touchedFields.fullName}
        {...register('fullName')}
      />

      <FormInput
        id="email"
        type="email"
        label="Địa chỉ Email"
        required
        icon={Mail}
        placeholder="name@example.com"
        autoComplete="email"
        error={errors.email?.message}
        isTouched={touchedFields.email}
        {...register('email')}
      />

      <FormInput
        id="password"
        type={showPassword ? 'text' : 'password'}
        label="Mật khẩu"
        required
        icon={Lock}
        placeholder="Tối thiểu 8 ký tự"
        autoComplete="new-password"
        error={errors.password?.message}
        isTouched={touchedFields.password}
        rightElement={
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="text-slate-400 hover:text-slate-600 focus:outline-none"
            title={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          >
            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        }
        {...register('password')}
      />

      <FormInput
        id="confirmPassword"
        type={showConfirmPassword ? 'text' : 'password'}
        label="Xác nhận mật khẩu"
        required
        icon={Lock}
        placeholder="Nhập lại mật khẩu"
        autoComplete="new-password"
        error={errors.confirmPassword?.message}
        isTouched={touchedFields.confirmPassword}
        rightElement={
          <button
            type="button"
            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
            className="text-slate-400 hover:text-slate-600 focus:outline-none"
            title={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          >
            {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        }
        {...register('confirmPassword')}
      />

      <button
        type="submit"
        disabled={registerMutation.isPending}
        className="w-full rounded-xl bg-cyan-500 px-4 py-3 text-sm font-bold text-white shadow-md shadow-cyan-500/25 hover:bg-cyan-600 active:scale-[0.99] disabled:opacity-60 transition-all flex items-center justify-center gap-2 mt-2"
      >
        {registerMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
        <span>{registerMutation.isPending ? 'Đang tạo tài khoản...' : 'Tạo tài khoản miễn phí'}</span>
      </button>

      <p className="text-center text-xs text-slate-500 pt-2">
        Đã có tài khoản?{' '}
        <Link href={ROUTES.login} className="font-bold text-cyan-600 hover:text-cyan-700 hover:underline">
          Đăng nhập ngay
        </Link>
      </p>
    </form>
  );
}
