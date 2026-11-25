"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import {
  Loader2,
  User,
  Mail,
  Lock,
  Eye,
  EyeOff,
  AlertCircle,
} from "lucide-react";
import { registerSchema, type RegisterInput } from "../schema";
import { useRegisterMutation } from "../api/useRegisterMutation";
import { ROUTES } from "@/constants";
import { FormInput } from "@/components/ui";

export function RegisterForm() {
  const [ready, setReady] = useState(false);
  useEffect(() => {
    setReady(true);
  }, []);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const registerMutation = useRegisterMutation();

  const {
    register,
    handleSubmit,
    formState: { errors, touchedFields },
  } = useForm<RegisterInput>({
    resolver: zodResolver(registerSchema),
    mode: "onTouched",
  });

  const onSubmit = (data: RegisterInput) => {
    registerMutation.mutate(data);
  };

  if (registerMutation.isSuccess) {
    return (
      <div role="status" className="space-y-4 text-sm">
        <p>
          Đã tạo tài khoản. Vui lòng kiểm tra email và xác minh địa chỉ trước
          khi đăng nhập.
        </p>
        <Link
          href={ROUTES.login}
          className="font-semibold text-primary hover:underline"
        >
          Đến trang đăng nhập
        </Link>
      </div>
    );
  }

  return (
    <form
      method="post"
      onSubmit={handleSubmit(onSubmit)}
      className="space-y-4"
      noValidate
    >
      {registerMutation.error && (
        <div className="rounded-lg bg-destructive/10 border border-destructive/20 p-3 text-xs text-destructive flex items-start gap-2">
          <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
          <span>
            {registerMutation.error.message ||
              "Đăng ký không thành công. Vui lòng thử lại."}
          </span>
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
        {...register("fullName")}
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
        {...register("email")}
      />

      <FormInput
        id="password"
        type={showPassword ? "text" : "password"}
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
            className="text-muted-foreground hover:text-foreground focus:outline-none"
            title={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
          >
            {showPassword ? (
              <EyeOff className="h-4 w-4" />
            ) : (
              <Eye className="h-4 w-4" />
            )}
          </button>
        }
        {...register("password")}
      />

      <FormInput
        id="confirmPassword"
        type={showConfirmPassword ? "text" : "password"}
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
            className="text-muted-foreground hover:text-foreground focus:outline-none"
            title={showConfirmPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
          >
            {showConfirmPassword ? (
              <EyeOff className="h-4 w-4" />
            ) : (
              <Eye className="h-4 w-4" />
            )}
          </button>
        }
        {...register("confirmPassword")}
      />

      <button
        type="submit"
        disabled={!ready || registerMutation.isPending}
        className="w-full rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors flex items-center justify-center gap-2 mt-3 shadow-xs"
      >
        {registerMutation.isPending && (
          <Loader2 className="h-4 w-4 animate-spin" />
        )}
        <span>
          {registerMutation.isPending
            ? "Đang tạo tài khoản..."
            : "Tạo tài khoản"}
        </span>
      </button>

      <p className="text-center text-xs text-muted-foreground pt-3">
        Đã có tài khoản?{" "}
        <Link
          href={ROUTES.login}
          className="font-semibold text-primary hover:underline"
        >
          Đăng nhập
        </Link>
      </p>
    </form>
  );
}
