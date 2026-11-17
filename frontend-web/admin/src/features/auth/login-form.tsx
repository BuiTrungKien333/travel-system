"use client";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff, ArrowRight, ShieldCheck } from "lucide-react";
import { loginSchema } from "@/lib/contracts";
import { errorMessage } from "@/lib/errors";
import { Banner, Brand, Button } from "@/components/ui";
import { useSignIn } from "./api";
export function LoginForm({ notice }: { notice?: string }) {
  const login = useSignIn();
  const [visible, setVisible] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<{ email: string; password: string }>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  });
  return (
    <main className="grid min-h-dvh lg:grid-cols-[1fr_1.05fr]">
      <section className="login-art relative hidden overflow-hidden bg-ink p-12 text-white lg:flex lg:flex-col lg:justify-between xl:p-16">
        <Brand light />
        <div className="relative z-10 max-w-lg pb-16">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/5 px-4 py-2 text-xs text-teal-100">
            <ShieldCheck size={15} /> WEGO MANAGEMENT CONSOLE
          </div>
          <h1 className="text-5xl font-semibold leading-[1.18] tracking-tight xl:text-6xl">
            Quản trị tập trung.
            <br />
            <span className="text-teal-500">Vận hành rõ ràng.</span>
          </h1>
          <p className="mt-6 max-w-sm text-base leading-7 text-white/65">
            Một không gian riêng để quản lý quyền truy cập và giữ hệ thống vận
            hành có kiểm soát.
          </p>
        </div>
        <p className="relative z-10 text-xs text-white/40">
          WeGo · Administration
        </p>
      </section>
      <section className="flex min-h-dvh flex-col px-6 py-8 sm:px-12 lg:px-16">
        <div className="lg:hidden">
          <Brand />
        </div>
        <div className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-12">
          <div className="mb-8">
            <p className="eyebrow">CHÀO MỪNG TRỞ LẠI</p>
            <h2 className="mt-3 text-3xl font-semibold tracking-tight text-ink">
              Đăng nhập quản trị
            </h2>
            <p className="mt-3 text-sm leading-6 text-muted">
              Sử dụng tài khoản quản trị viên đã được cấp quyền.
            </p>
          </div>
          {notice === "expired" ? (
            <Banner error>
              Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.
            </Banner>
          ) : notice === "logout-incomplete" ? (
            <Banner error>
              Đã đóng phiên trên trình duyệt. Chưa xác nhận được việc thu hồi
              phiên ở backend.
            </Banner>
          ) : null}
          <form
            className="mt-5 space-y-5"
            onSubmit={handleSubmit(async (values) => {
              try {
                await login.mutateAsync(values);
              } catch {
                /* Mutation state renders the failure below. */
              }
            })}
          >
            <div>
              <label htmlFor="email" className="label">
                Email quản trị
              </label>
              <input
                id="email"
                type="email"
                autoComplete="username"
                autoFocus
                className="input"
                placeholder="admin@your-company.vn"
                aria-invalid={!!errors.email}
                aria-describedby={errors.email ? "email-error" : undefined}
                {...register("email")}
              />
              {errors.email ? (
                <p id="email-error" className="field-error">
                  {errors.email.message}
                </p>
              ) : null}
            </div>
            <div>
              <label htmlFor="password" className="label">
                Mật khẩu
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={visible ? "text" : "password"}
                  autoComplete="current-password"
                  className="input pr-12"
                  placeholder="Nhập mật khẩu của bạn"
                  aria-invalid={!!errors.password}
                  aria-describedby={
                    errors.password ? "password-error" : undefined
                  }
                  {...register("password")}
                />
                <button
                  type="button"
                  className="absolute right-1 top-1 grid size-10 place-items-center rounded-lg text-muted hover:bg-canvas"
                  aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                  aria-pressed={visible}
                  onClick={() => setVisible(!visible)}
                >
                  {visible ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {errors.password ? (
                <p id="password-error" className="field-error">
                  {errors.password.message}
                </p>
              ) : null}
            </div>
            {login.isError ? (
              <Banner error>{errorMessage(login.error)}</Banner>
            ) : null}
            <Button
              type="submit"
              busy={login.isPending}
              className="w-full justify-between py-3.5"
            >
              Đăng nhập
              <ArrowRight size={17} aria-hidden="true" />
            </Button>
          </form>
          <p className="mt-8 flex items-start gap-2 border-t border-line pt-6 text-xs leading-5 text-muted">
            <ShieldCheck
              size={16}
              className="mt-0.5 shrink-0"
              aria-hidden="true"
            />
            Khu vực giới hạn quyền truy cập. Tài khoản người dùng và khách không
            thể đăng nhập tại đây.
          </p>
        </div>
        <p className="text-center text-xs text-muted">
          WeGo Admin · Quyền truy cập được xác thực bởi hệ thống
        </p>
      </section>
    </main>
  );
}
