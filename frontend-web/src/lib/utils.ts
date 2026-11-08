import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import { format, parseISO, isValid } from "date-fns";
import { vi } from "date-fns/locale";

// ─── Tailwind class merge ──────────────────────────────────────────────────────
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// ─── Date formatting ──────────────────────────────────────────────────────────
/**
 * Format ISO date string for display.
 * @example formatDate("2026-08-15") → "15 tháng 8, 2026"
 */
export function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return "";
  try {
    const d = parseISO(dateStr);
    if (!isValid(d)) return dateStr;
    return format(d, "d MMMM, yyyy", { locale: vi });
  } catch {
    return dateStr;
  }
}

/**
 * Format ISO date string to short form.
 * @example formatDateShort("2026-08-15") → "15/08/2026"
 */
export function formatDateShort(dateStr: string | null | undefined): string {
  if (!dateStr) return "";
  try {
    const d = parseISO(dateStr);
    if (!isValid(d)) return dateStr;
    return format(d, "dd/MM/yyyy");
  } catch {
    return dateStr;
  }
}

/**
 * Format a date range for display.
 * @example formatDateRange("2026-08-15", "2026-08-18") → "15 – 18 tháng 8, 2026"
 */
export function formatDateRange(start: string, end: string): string {
  try {
    const s = parseISO(start);
    const e = parseISO(end);
    if (!isValid(s) || !isValid(e)) return `${start} – ${end}`;
    if (s.getMonth() === e.getMonth() && s.getFullYear() === e.getFullYear()) {
      return `${format(s, "d")} – ${format(e, "d MMMM, yyyy", { locale: vi })}`;
    }
    return `${format(s, "d MMM", { locale: vi })} – ${format(e, "d MMM, yyyy", { locale: vi })}`;
  } catch {
    return `${start} – ${end}`;
  }
}

/**
 * Calculate number of days between two ISO date strings (inclusive).
 * @example daysBetween("2026-08-15", "2026-08-17") → 3
 */
export function daysBetween(start: string, end: string): number {
  try {
    const s = parseISO(start);
    const e = parseISO(end);
    return Math.round((e.getTime() - s.getTime()) / 86_400_000) + 1;
  } catch {
    return 0;
  }
}

// ─── Currency formatting ──────────────────────────────────────────────────────
/**
 * Format number as Vietnamese Dong.
 * @example formatCurrency(150000) → "150.000 ₫"
 */
export function formatCurrency(
  amount: number,
  currency: string = "VND"
): string {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(amount);
}

// ─── Duration formatting ──────────────────────────────────────────────────────
/**
 * Format minutes to human-readable duration.
 * @example formatDuration(90) → "1 giờ 30 phút"
 */
export function formatDuration(minutes: number): string {
  if (minutes < 60) return `${minutes} phút`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h} giờ ${m} phút` : `${h} giờ`;
}

/**
 * Format distance in km.
 * @example formatDistance(1.5) → "1.5 km"
 */
export function formatDistance(km: number): string {
  if (km < 1) return `${Math.round(km * 1000)} m`;
  return `${km.toFixed(1)} km`;
}

// ─── String helpers ────────────────────────────────────────────────────────────
export function truncate(str: string, maxLength: number): string {
  if (str.length <= maxLength) return str;
  return str.slice(0, maxLength - 3) + "...";
}

export function initials(name: string): string {
  return name
    .split(" ")
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? "")
    .join("");
}
