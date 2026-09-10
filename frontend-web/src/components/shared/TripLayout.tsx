"use client";

import { useTripQuery } from "@/features/trip/api/useTripQuery";
import { TripHeader } from "./TripHeader";
import { TripSidebar } from "./TripSidebar";
import { useIsMobile } from "@/hooks/useMediaQuery";
import { Panel, PanelGroup, PanelResizeHandle } from "react-resizable-panels";
import { useTripUIStore } from "@/features/trip/store/useTripUIStore";
import { Menu, X } from "lucide-react";
import { useState } from "react";

interface TripLayoutProps {
  tripId: string;
  children: React.ReactNode;
  /** Whether to show the Map panel on the right (desktop). Itinerary page shows it; others may not. */
  showMap?: boolean;
  mapSlot?: React.ReactNode;
}

export function TripLayout({
  tripId,
  children,
  showMap = false,
  mapSlot,
}: TripLayoutProps) {
  const { data: trip, isLoading, isError } = useTripQuery(tripId);
  const isMobile = useIsMobile();
  const sidebarCollapsed = useTripUIStore((s) => s.sidebarCollapsed);
  const mobileView = useTripUIStore((s) => s.mobileView);
  const setMobileView = useTripUIStore((s) => s.setMobileView);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="space-y-3 text-center">
          <div className="h-6 w-6 rounded-full border-2 border-primary border-t-transparent animate-spin mx-auto" />
          <p className="text-xs text-muted-foreground">Đang tải chuyến đi...</p>
        </div>
      </div>
    );
  }

  if (isError || !trip) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="text-center space-y-3 max-w-sm px-4">
          <h2 className="text-base font-semibold text-foreground">Không thể tải thông tin chuyến đi</h2>
          <p className="text-xs text-muted-foreground">
            Có lỗi xảy ra khi tải dữ liệu. Vui lòng kiểm tra kết nối mạng và thử lại.
          </p>
          <button
            onClick={() => window.location.reload()}
            className="mt-3 rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background">
      {/* Mobile Header with Drawer Toggle */}
      {isMobile && (
        <div className="flex items-center gap-3 px-4 h-14 border-b border-border/60 bg-background/95 backdrop-blur-sm z-20 sticky top-0">
          <button
            onClick={() => setMobileDrawerOpen(true)}
            className="p-1.5 -ml-1.5 rounded-md hover:bg-muted text-muted-foreground hover:text-foreground"
            aria-label="Mở menu"
          >
            <Menu className="h-5 w-5" />
          </button>
          <div className="flex-1 font-semibold text-sm truncate text-foreground">
            {trip.name}
          </div>
          {showMap && (
            <button 
              onClick={() => setMobileView(mobileView === 'map' ? 'content' : 'map')}
              className="text-xs font-medium text-primary bg-primary/10 px-2.5 py-1 rounded-md"
            >
              {mobileView === 'map' ? 'Danh sách' : 'Bản đồ'}
            </button>
          )}
        </div>
      )}

      {/* Shared header (Desktop) */}
      {!isMobile && <TripHeader trip={trip} />}

      {/* Mobile Drawer */}
      {isMobile && mobileDrawerOpen && (
        <div className="fixed inset-0 z-50 flex animate-fade-in">
          <div
            className="fixed inset-0 bg-black/40 backdrop-blur-xs"
            onClick={() => setMobileDrawerOpen(false)}
          />
          <div className="relative w-64 bg-card h-full flex flex-col shadow-xl z-10 border-r border-border">
            <div className="p-4 flex items-center justify-between border-b border-border/60">
              <span className="font-bold text-base text-foreground tracking-tight">WnGo</span>
              <button
                onClick={() => setMobileDrawerOpen(false)}
                className="p-1 rounded-md hover:bg-muted text-muted-foreground"
                aria-label="Đóng menu"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto" onClick={() => setMobileDrawerOpen(false)}>
              <TripSidebar tripId={tripId} />
            </div>
          </div>
        </div>
      )}

      {/* Main Workspace Body */}
      <div className="flex-1 overflow-hidden">
        {isMobile ? (
          <div className="h-full overflow-y-auto">
            {showMap && mobileView === 'map' ? (
              <div className="h-full w-full">{mapSlot}</div>
            ) : (
              children
            )}
          </div>
        ) : (
          <PanelGroup direction="horizontal" className="h-full">
            {/* Left: Sidebar */}
            <Panel
              defaultSize={sidebarCollapsed ? 5 : 16}
              minSize={4}
              maxSize={22}
              collapsible
              className="transition-all duration-200"
            >
              <TripSidebar tripId={tripId} />
            </Panel>

            <PanelResizeHandle className="w-1 bg-border/40 hover:bg-primary/40 transition-colors cursor-col-resize" />

            {/* Center: Main Content */}
            <Panel minSize={30} className="overflow-y-auto">
              {children}
            </Panel>

            {/* Right: Map Panel (if enabled) */}
            {showMap && mapSlot && (
              <>
                <PanelResizeHandle className="w-1 bg-border/40 hover:bg-primary/40 transition-colors cursor-col-resize" />
                <Panel defaultSize={42} minSize={25} maxSize={60} className="h-full">
                  {mapSlot}
                </Panel>
              </>
            )}
          </PanelGroup>
        )}
      </div>
    </div>
  );
}
