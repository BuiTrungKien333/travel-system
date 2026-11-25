"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, MapPin, Database } from "lucide-react";
import { LogOut, ShieldCheck, Users } from "lucide-react";
import type { AdminSession } from "@/lib/contracts";
import { errorMessage } from "@/lib/errors";
import { useSignOut } from "@/features/auth/api";
import { Brand, Button, Banner } from "./ui";
export function AdminShell({
  session,
  children,
}: {
  session: AdminSession;
  children: React.ReactNode;
}) {
  const logout = useSignOut();
  const path = usePathname();
  const menu = [
    { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
    { href: "/users", label: "Quản lý user", icon: Users },
    { href: "/places", label: "Quản lý địa điểm", icon: MapPin },
    { href: "/knowledge", label: "Quản lý kho tri thức AI", icon: Database },
  ];
  const current = menu.find((m) => path.endsWith(m.href)) ?? menu[0];
  return (
    <div className="min-h-dvh bg-canvas lg:pl-64">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:left-3 focus:top-3 focus:z-50 focus:bg-white focus:p-3"
      >
        Chuyển đến nội dung chính
      </a>
      <aside className="border-b border-line bg-white px-5 py-5 lg:fixed lg:inset-y-0 lg:left-0 lg:flex lg:w-64 lg:flex-col lg:border-b-0 lg:border-r lg:px-6 lg:py-8">
        <Link href="/dashboard" aria-label="WeGo Admin - dashboard">
          <Brand />
        </Link>
        <p className="mb-3 mt-10 hidden text-xs font-semibold uppercase tracking-[.12em] text-muted lg:block">
          Không gian quản trị
        </p>
        <nav aria-label="Điều hướng quản trị" className="mt-5 lg:mt-0">
          {menu.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              aria-current={current.href === item.href ? "page" : undefined}
              className={`mb-2 flex items-center gap-3 rounded-xl border px-4 py-3 text-sm font-semibold ${current.href === item.href ? "border-teal-100 bg-teal-50 text-teal-700" : "border-transparent text-muted hover:bg-canvas"}`}
            >
              <item.icon size={18} />
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="mt-auto hidden border-t border-line pt-5 lg:block">
          <div className="flex items-center gap-2 text-xs font-medium text-ink">
            <ShieldCheck size={15} className="text-teal-600" />
            Quyền truy cập được kiểm soát
          </div>
          <p className="mt-2 text-xs leading-5 text-muted">
            Thay đổi quyền và trạng thái tài khoản được ghi lại trong hệ thống.
          </p>
        </div>
      </aside>
      <header className="flex flex-wrap items-center justify-between gap-4 border-b border-line bg-white px-6 py-4 lg:px-10">
        <p className="text-xs text-muted">
          Quản trị <span className="mx-2 text-line">/</span>
          <span className="font-medium text-ink">{current.label}</span>
        </p>
        <div className="flex items-center gap-4">
          <div className="hidden text-right sm:block">
            <p className="text-sm font-semibold text-ink">{session.fullName}</p>
            <p className="mt-0.5 text-xs text-muted">{session.email}</p>
          </div>
          <span
            className="grid size-10 place-items-center rounded-full border border-teal-100 bg-teal-50 text-sm font-bold text-teal-700"
            aria-hidden="true"
          >
            {session.fullName.trim().slice(0, 1).toUpperCase()}
          </span>
          <Button
            variant="secondary"
            busy={logout.isPending}
            onClick={() => {
              logout.mutate();
            }}
            aria-label="Đăng xuất"
          >
            <LogOut size={16} />
            <span className="hidden sm:inline">Đăng xuất</span>
          </Button>
        </div>
      </header>
      {logout.isError ? (
        <div className="px-6 pt-5 lg:px-10">
          <Banner error>{errorMessage(logout.error)}</Banner>
        </div>
      ) : null}
      <main
        id="main-content"
        className="mx-auto max-w-[1500px] px-5 py-8 sm:px-8 lg:px-10 lg:py-10"
      >
        {children}
      </main>
    </div>
  );
}
