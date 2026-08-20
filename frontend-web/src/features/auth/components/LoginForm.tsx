'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Loader2, Mail, Lock, Eye, EyeOff, AlertCircle } from 'lucide-react';
import { loginSchema, type LoginInput } from '../schema';
import { useLoginMutation } from '../api/useLoginMutation';
import { ROUTES } from '@/constants';
import { FormInput } from '@/components/ui';

export function LoginForm() {
  const [showPassword, setShowPassword] = useState(false);
  const router = useRouter();
  const loginMutation = useLoginMutation();

  const {
    register,
    handleSubmit,
    formState: { errors, touchedFields },
  } = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    mode: 'onTouched',
  });

  const onSubmit = (data: LoginInput) => {
    loginMutation.mutate(data, {
      onSuccess: () => router.push(ROUTES.trips),
    });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
      {loginMutation.error && (
        <div className="rounded-xl bg-red-50 border border-red-200 p-3 text-xs text-red-700 flex items-start gap-2 animate-fade-in">
          <AlertCircle className="h-4 w-4 shrink-0 text-red-500 mt-0.5" />
          <span>{loginMutation.error.message || 'Đăng nhập không thành công. Vui lòng kiểm tra lại.'}</span>
        </div>
      )}

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
        placeholder="Tối thiểu 6 ký tự"
        autoComplete="current-password"
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

      <button
        type="submit"
        disabled={loginMutation.isPending}
        className="w-full rounded-xl bg-cyan-500 px-4 py-3 text-sm font-bold text-white shadow-md shadow-cyan-500/25 hover:bg-cyan-600 active:scale-[0.99] disabled:opacity-60 transition-all flex items-center justify-center gap-2 mt-2"
      >
        {loginMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
        <span>{loginMutation.isPending ? 'Đang đăng nhập...' : 'Đăng nhập'}</span>
      </button>

      <p className="text-center text-xs text-slate-500 pt-2">
        Chưa có tài khoản?{' '}
        <Link href={ROUTES.register} className="font-bold text-cyan-600 hover:text-cyan-700 hover:underline">
          Đăng ký miễn phí
        </Link>
      </p>
    </form>
  );
}
