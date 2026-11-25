"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Compass,
  Menu,
  X,
  Plus,
  Luggage,
  LogOut,
  User as UserIcon,
  MapPin,
  ChevronDown,
} from "lucide-react";
import { ROUTES, HEADER_NAV_LINKS, isTripWorkspacePath } from "@/constants";
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
      setIsScrolled(window.scrollY > 10);
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

  if (isTripWorkspacePath(pathname)) return null;
  return (
    <header
      className={`sticky top-0 z-50 transition-colors duration-200 ${
        isScrolled
          ? "bg-background/95 backdrop-blur-sm border-b border-border shadow-[0_1px_3px_rgba(0,0,0,0.02)]"
          : "bg-background border-b border-border/60"
      }`}
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          {/* Left: Logo & Brand */}
          <div className="flex items-center gap-8">
            <Link href={ROUTES.home} className="flex items-center gap-2.5 group">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
                <Compass className="h-4.5 w-4.5" />
              </div>
              <div className="flex flex-col">
                <span className="font-bold text-lg tracking-tight text-foreground leading-none">
                  WnGo
                </span>
                <span className="text-[10px] font-medium text-muted-foreground mt-0.5 tracking-wide">
                  Travel Planner
                </span>
              </div>
            </Link>

            {/* Desktop Navigation Links */}
            <nav className="hidden md:flex items-center gap-1">
              {navLinks.map((item) => {
                const isActive =
                  pathname === item.href ||
                  (item.href === ROUTES.journal &&
                    (pathname === '/journal' || pathname.startsWith('/journal/')));
                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                      isActive
                        ? "text-primary font-semibold bg-primary/5"
                        : "text-muted-foreground hover:text-foreground hover:bg-muted/50"
                    }`}
                  >
                    {item.name}
                  </Link>
                );
              })}
            </nav>
          </div>

          {/* Desktop Right Action Buttons */}
          <div className="hidden sm:flex items-center gap-3">
            <Link
              href={ROUTES.tripNew}
              className="inline-flex items-center justify-center gap-1.5 rounded-lg bg-primary px-3.5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              <Plus className="h-4 w-4" />
              <span>Tạo chuyến đi</span>
            </Link>

            {/* User Profile Avatar Dropdown OR Login button */}
            {isLoggedIn ? (
              <div className="relative" ref={userMenuRef}>
                <button
                  type="button"
                  onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                  className="flex items-center gap-2 rounded-lg border border-border/70 p-1.5 pr-2.5 hover:bg-muted/40 transition-colors"
                  aria-label="Menu tài khoản"
                  aria-expanded={isUserMenuOpen}
                >
                  <UserAvatar size="sm" alt={user.fullName} />
                  <span className="text-xs font-medium text-foreground max-w-[120px] truncate">
                    {user.fullName}
                  </span>
                  <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
                </button>

                {/* Dropdown Menu */}
                {isUserMenuOpen && (
                  <div className="absolute right-0 mt-2 w-60 rounded-xl border border-border bg-card p-1.5 shadow-lg animate-fade-in z-50">
                    <div className="p-2.5 border-b border-border/60">
                      <p className="text-sm font-semibold text-foreground truncate">
                        {user.fullName}
                      </p>
                      <p className="text-xs text-muted-foreground truncate">
                        {user.email}
                      </p>
                    </div>

                    <div className="py-1">
                      <Link
                        href={ROUTES.account}
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center gap-2 px-2.5 py-2 text-xs font-medium text-foreground rounded-md hover:bg-muted transition-colors"
                      >
                        <UserIcon className="h-4 w-4 text-muted-foreground" />
                        <span>Tài khoản cá nhân</span>
                      </Link>

                      <Link
                        href={ROUTES.trips}
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center gap-2 px-2.5 py-2 text-xs font-medium text-foreground rounded-md hover:bg-muted transition-colors"
                      >
                        <Luggage className="h-4 w-4 text-muted-foreground" />
                        <span>Chuyến đi của tôi</span>
                      </Link>

                      <Link
                        href={ROUTES.journal}
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-slate-700 rounded-xl hover:bg-cyan-50 hover:text-cyan-700 transition"
                      >
                        <Compass className="h-4 w-4 text-slate-400" />
                        <span>Nhật ký hành trình (Journal)</span>
                      </Link>

                      <Link
                        href={ROUTES.journey}
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center gap-2 px-2.5 py-2 text-xs font-medium text-foreground rounded-md hover:bg-muted transition-colors"
                      >
                        <MapPin className="h-4 w-4 text-muted-foreground" />
                        <span>Hành trình Việt Nam</span>
                      </Link>
                    </div>

                    <div className="pt-1 border-t border-border/60">
                      <button
                        onClick={handleLogout}
                        className="flex w-full items-center gap-2 px-2.5 py-2 text-xs font-medium text-destructive rounded-md hover:bg-destructive/10 transition-colors"
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
                className="text-sm font-medium text-foreground hover:text-primary px-3 py-2 rounded-lg hover:bg-muted/40 transition-colors"
              >
                Đăng nhập
              </Link>
            )}
          </div>

          {/* Mobile menu button */}
          <div className="flex sm:hidden">
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              type="button"
              className="inline-flex items-center justify-center rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-foreground"
              aria-label="Mở menu"
            >
              {isMobileMenuOpen ? (
                <X className="h-5 w-5" />
              ) : (
                <Menu className="h-5 w-5" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu dropdown */}
      {isMobileMenuOpen && (
        <div className="md:hidden border-b border-border bg-background px-4 pt-2 pb-5 shadow-md animate-fade-in">
          {isLoggedIn && (
            <div className="p-3 mb-3 bg-muted/40 rounded-lg flex items-center justify-between gap-3">
              <div className="flex items-center gap-2.5 min-w-0">
                <UserAvatar size="md" alt={user.fullName} />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-foreground truncate">
                    {user.fullName}
                  </p>
                  <p className="text-xs text-muted-foreground truncate">
                    {user.email}
                  </p>
                </div>
              </div>
              <button
                onClick={handleLogout}
                className="text-xs font-medium text-destructive hover:underline shrink-0"
              >
                Đăng xuất
              </button>
            </div>
          )}

          <div className="space-y-1">
            {navLinks.map((item) => (
              <Link
                key={item.name}
                href={item.href}
                onClick={() => setIsMobileMenuOpen(false)}
                className="flex items-center px-3 py-2.5 text-sm font-medium text-foreground rounded-lg hover:bg-muted"
              >
                {item.name}
              </Link>
            ))}
          </div>

          <div className="mt-4 pt-4 border-t border-border/60 flex flex-col gap-2">
            <Link
              href={ROUTES.tripNew}
              onClick={() => setIsMobileMenuOpen(false)}
              className="flex items-center justify-center gap-1.5 rounded-lg bg-primary px-4 py-2.5 text-center text-sm font-medium text-primary-foreground"
            >
              <Plus className="h-4 w-4" />
              <span>Tạo chuyến đi mới</span>
            </Link>

            {!isLoggedIn && (
              <Link
                href={ROUTES.login}
                onClick={() => setIsMobileMenuOpen(false)}
                className="flex items-center justify-center rounded-lg border border-border px-4 py-2.5 text-center text-sm font-medium text-foreground hover:bg-muted"
              >
                Đăng nhập tài khoản
              </Link>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
