"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Compass,
  Menu,
  X,
  Sparkles,
  Plus,
  Luggage,
  LogIn,
  LogOut,
  User as UserIcon,
  ChevronDown,
  MapPin,
} from "lucide-react";
import { ROUTES, HEADER_NAV_LINKS } from "@/constants";
import { useAuth } from "@/hooks/useAuth";
import { useLogoutMutation } from "@/features/auth/api/useLogoutMutation";
import { UserAvatar } from "@/components/ui/UserAvatar";

export function Header() {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [mounted, setMounted] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);

  const pathname = usePathname();
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  const logoutMutation = useLogoutMutation();

  useEffect(() => {
    setMounted(true);
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 15);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Close user dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        userMenuRef.current &&
        !userMenuRef.current.contains(event.target as Node)
      ) {
        setIsUserMenuOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleLogout = () => {
    setIsUserMenuOpen(false);
    setIsMobileMenuOpen(false);
    logoutMutation.mutate(undefined, {
      onSettled: () => {
        router.push(ROUTES.home);
      },
    });
  };

  const navLinks = HEADER_NAV_LINKS;

  const isLoggedIn = mounted && isAuthenticated && user;

  return (
    <header
      className={`sticky top-0 z-50 transition-all duration-200 ${
        isScrolled
          ? "bg-white/95 backdrop-blur-md shadow-sm border-b border-cyan-100"
          : "bg-white/90 backdrop-blur-sm border-b border-slate-100"
      }`}
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          {/* Left: Logo & Brand */}
          <div className="flex items-center gap-8">
            <Link href={ROUTES.home} className="flex items-center gap-2.5 group">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-cyan-500 via-teal-500 to-sky-400 text-white shadow-md shadow-cyan-500/25 transition-colors">
                <Compass className="h-5 w-5" />
              </div>
              <div className="flex flex-col">
                <span className="font-extrabold text-xl tracking-tight bg-gradient-to-r from-slate-900 via-slate-800 to-cyan-600 bg-clip-text text-transparent">
                  Tripzy
                </span>
                <span className="text-[10px] font-semibold uppercase tracking-wider text-cyan-600 -mt-1 flex items-center gap-1">
                  <Sparkles className="h-2.5 w-2.5" /> AI Travel Planner
                </span>
              </div>
            </Link>

            {/* Desktop Navigation Links */}
            <nav className="hidden md:flex items-center gap-1 lg:gap-2">
              {navLinks.map((item) => {
                const isActive = pathname === item.href;
                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium transition ${
                      isActive
                        ? "bg-cyan-50 text-cyan-700 font-semibold"
                        : "text-slate-600 hover:text-cyan-600 hover:bg-cyan-50/60"
                    }`}
                  >
                    <span>{item.name}</span>
                  </Link>
                );
              })}
            </nav>
          </div>

          {/* Desktop Right Action Buttons */}
          <div className="hidden sm:flex items-center gap-3">
            <Link
              href={ROUTES.trips}
              className="flex items-center gap-1.5 text-sm font-medium text-slate-700 hover:text-cyan-600 px-3 py-2 rounded-lg hover:bg-slate-50 transition"
            >
              <Luggage className="h-4 w-4 text-slate-400" />
              <span>Chuyến đi của tôi</span>
            </Link>

            <Link
              href={ROUTES.tripNew}
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-cyan-500 px-4 py-2 text-sm font-bold text-white shadow-md shadow-cyan-500/25 hover:bg-cyan-600 active:scale-[0.99] transition-all"
            >
              <Plus className="h-4 w-4 stroke-[2.5]" />
              <span>Tạo chuyến đi</span>
            </Link>

            {/* User Profile Avatar Dropdown (when logged in) OR Login button */}
            {isLoggedIn ? (
              <div className="relative" ref={userMenuRef}>
                <button
                  type="button"
                  onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                  className="flex items-center gap-2 rounded-full border border-cyan-100 bg-slate-50 p-1 pr-2.5 hover:bg-cyan-50 hover:border-cyan-300 transition"
                  aria-label="Menu người dùng"
                >
                  <UserAvatar size="sm" alt={user.fullName} />
                  <span className="text-xs font-semibold text-slate-800 max-w-[100px] truncate">
                    {user.fullName}
                  </span>
                  <ChevronDown className="h-3 w-3 text-slate-400" />
                </button>

                {/* Dropdown Menu */}
                {isUserMenuOpen && (
                  <div className="absolute right-0 mt-2 w-64 rounded-2xl border border-slate-200 bg-white p-2 shadow-xl animate-fade-in z-50">
                    <div className="p-3 border-b border-slate-100 flex items-center gap-3">
                      <UserAvatar size="lg" alt={user.fullName} />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-bold text-slate-900 truncate">
                          {user.fullName}
                        </p>
                        <p className="text-xs text-slate-500 truncate">
                          {user.email}
                        </p>
                      </div>
                    </div>

                    <div className="py-1 space-y-0.5">
                      <Link
                        href={ROUTES.account}
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-slate-700 rounded-xl hover:bg-cyan-50 hover:text-cyan-700 transition"
                      >
                        <UserIcon className="h-4 w-4 text-slate-400" />
                        <span>Tài khoản & Hồ sơ</span>
                      </Link>

                      <Link
                        href={ROUTES.trips}
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-slate-700 rounded-xl hover:bg-cyan-50 hover:text-cyan-700 transition"
                      >
                        <Luggage className="h-4 w-4 text-slate-400" />
                        <span>Chuyến đi đã tạo</span>
                      </Link>

                      <Link
                        href={ROUTES.journey}
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-slate-700 rounded-xl hover:bg-cyan-50 hover:text-cyan-700 transition"
                      >
                        <MapPin className="h-4 w-4 text-slate-400" />
                        <span>Hành trình 63 tỉnh thành</span>
                      </Link>
                    </div>

                    <div className="pt-1 border-t border-slate-100">
                      <button
                        onClick={handleLogout}
                        className="flex w-full items-center gap-2.5 px-3 py-2 text-xs font-medium text-red-600 rounded-xl hover:bg-red-50 transition"
                      >
                        <LogOut className="h-4 w-4" />
                        <span>Đăng xuất</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <Link
                href={ROUTES.login}
                className="flex items-center gap-1.5 text-sm font-semibold text-slate-700 hover:text-cyan-600 px-3.5 py-2 rounded-xl hover:bg-cyan-50 transition"
              >
                <LogIn className="h-4 w-4 text-slate-400" />
                <span>Đăng nhập</span>
              </Link>
            )}
          </div>

          {/* Mobile menu button */}
          <div className="flex sm:hidden">
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              type="button"
              className="inline-flex items-center justify-center rounded-lg p-2 text-slate-600 hover:bg-slate-100 hover:text-slate-900"
              aria-label="Menu"
            >
              {isMobileMenuOpen ? (
                <X className="h-6 w-6" />
              ) : (
                <Menu className="h-6 w-6" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu dropdown */}
      {isMobileMenuOpen && (
        <div className="md:hidden border-b border-slate-200 bg-white px-4 pt-2 pb-6 shadow-lg animate-fade-in">
          {/* User info in mobile menu if logged in */}
          {isLoggedIn && (
            <div className="p-3 mb-3 bg-cyan-50/70 rounded-2xl border border-cyan-100 flex items-center justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                <UserAvatar size="md" alt={user.fullName} />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-bold text-slate-900 truncate">
                    {user.fullName}
                  </p>
                  <p className="text-xs text-slate-500 truncate">
                    {user.email}
                  </p>
                </div>
              </div>
              <button
                onClick={handleLogout}
                className="text-xs font-semibold text-red-600 hover:underline shrink-0"
              >
                Đăng xuất
              </button>
            </div>
          )}

          <div className="space-y-1">
            {navLinks.map((item) => {
              return (
                <Link
                  key={item.name}
                  href={item.href}
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-base font-medium text-slate-700 hover:bg-cyan-50 hover:text-cyan-600"
                >
                  <span>{item.name}</span>
                </Link>
              );
            })}
          </div>

          <div className="mt-4 pt-4 border-t border-slate-100 flex flex-col gap-2">
            <Link
              href={ROUTES.trips}
              onClick={() => setIsMobileMenuOpen(false)}
              className="flex items-center justify-center gap-2 rounded-xl border border-slate-200 px-4 py-2.5 text-center text-sm font-semibold text-slate-800 hover:bg-slate-50"
            >
              <Luggage className="h-4 w-4" />
              <span>Chuyến đi của tôi</span>
            </Link>

            {!isLoggedIn && (
              <Link
                href={ROUTES.login}
                onClick={() => setIsMobileMenuOpen(false)}
                className="flex items-center justify-center gap-2 rounded-xl border border-cyan-200 bg-cyan-50/60 px-4 py-2.5 text-center text-sm font-semibold text-cyan-800 hover:bg-cyan-100/70"
              >
                <LogIn className="h-4 w-4 text-cyan-600" />
                <span>Đăng nhập tài khoản</span>
              </Link>
            )}

            <Link
              href={ROUTES.tripNew}
              onClick={() => setIsMobileMenuOpen(false)}
              className="flex items-center justify-center gap-2 rounded-xl bg-cyan-500 px-4 py-2.5 text-center text-sm font-bold text-white shadow-sm hover:bg-cyan-600"
            >
              <Plus className="h-4 w-4 stroke-[2.5]" />
              <span>Tạo chuyến đi mới</span>
            </Link>
          </div>
        </div>
      )}
    </header>
  );
}
