"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  CalendarDays,
  Map,
  Camera,
  Settings,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/constants";
import { useTripUIStore } from "@/features/trip/store/useTripUIStore";

interface TripSidebarProps {
  tripId: string;
}

const NAV_ITEMS = [
  {
    label: "Tổng quan",
    href: (id: string) => ROUTES.trip(id),
    icon: LayoutDashboard,
    exact: true,
  },
  {
    label: "Lịch trình",
    href: (id: string) => ROUTES.tripItinerary(id),
    icon: CalendarDays,
    exact: false,
  },
  {
    label: "Bản đồ",
    href: (id: string) => `${ROUTES.trip(id)}/map`,
    icon: Map,
    exact: false,
  },
  {
    label: "Kỷ niệm",
    href: (id: string) => ROUTES.tripMemories(id),
    icon: Camera,
    exact: false,
  },
  {
    label: "Cài đặt",
    href: (id: string) => ROUTES.tripSettings(id),
    icon: Settings,
    exact: false,
  },
];

export function TripSidebar({ tripId }: TripSidebarProps) {
  const pathname = usePathname();
  const sidebarCollapsed = useTripUIStore((s) => s.sidebarCollapsed);
  const setSidebarCollapsed = useTripUIStore((s) => s.setSidebarCollapsed);

  return (
    <div className="h-full flex flex-col bg-card w-full border-r border-border/60">
      {/* Collapse toggle */}
      <div className="hidden sm:flex justify-end p-2.5 border-b border-border/40">
        <button
          onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          className="rounded-md p-1.5 hover:bg-muted/70 transition-colors text-muted-foreground hover:text-foreground"
          aria-label={sidebarCollapsed ? "Mở rộng thanh điều hướng" : "Thu gọn thanh điều hướng"}
        >
          {sidebarCollapsed ? (
            <ChevronRight className="h-4 w-4" />
          ) : (
            <ChevronLeft className="h-4 w-4" />
          )}
        </button>
      </div>

      {/* Nav links */}
      <nav className="flex flex-col gap-1 p-2 flex-1" role="navigation">
        {NAV_ITEMS.map((item) => {
          const href = item.href(tripId);
          const isActive = item.exact
            ? pathname === href
            : pathname.startsWith(href);

          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-xs font-medium transition-colors",
                isActive
                  ? "bg-primary/10 text-primary font-semibold"
                  : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
              )}
              aria-current={isActive ? "page" : undefined}
              title={sidebarCollapsed ? item.label : undefined}
            >
              <item.icon className="h-4 w-4 flex-shrink-0" />
              {!sidebarCollapsed && (
                <span className="truncate">{item.label}</span>
              )}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
