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
        <div className="rounded-lg bg-destructive/10 border border-destructive/20 p-3 text-xs text-destructive flex items-start gap-2">
          <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
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
            className="text-muted-foreground hover:text-foreground focus:outline-none"
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
        className="w-full rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors flex items-center justify-center gap-2 mt-3 shadow-xs"
      >
        {loginMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
        <span>{loginMutation.isPending ? 'Đang đăng nhập...' : 'Đăng nhập'}</span>
      </button>

      <p className="text-center text-xs text-muted-foreground pt-3">
        Chưa có tài khoản?{' '}
        <Link href={ROUTES.register} className="font-semibold text-primary hover:underline">
          Đăng ký miễn phí
        </Link>
      </p>
    </form>
  );
}
