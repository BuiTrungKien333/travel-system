"use client";

import { useTripQuery } from "@/features/trip/api/useTripQuery";
import { TripHeader } from "./TripHeader";
import { TripSidebar } from "./TripSidebar";
import { useIsMobile } from "@/hooks/useMediaQuery";
import { cn } from "@/lib/utils";

interface TripLayoutProps {
  tripId: string;
  children: React.ReactNode;
  /** Whether to show the Map panel on the right (desktop). Itinerary page shows it; others may not. */
  showMap?: boolean;
  mapSlot?: React.ReactNode;
}

/**
 * TripLayout — shared by ALL Trip Workspace routes:
 *   /trips/[tripId]
 *   /trips/[tripId]/itinerary
 *   /trips/[tripId]/memories
 *   /trips/[tripId]/settings
 *
 * Layout:
 *   Desktop:  [Sidebar | Content | Map?]
 *   Mobile:   [Content full-width, Sidebar → Drawer]
 *
 * Do NOT create separate layout/header per sub-page.
 */
export function TripLayout({
  tripId,
  children,
  showMap = false,
  mapSlot,
}: TripLayoutProps) {
  const { data: trip, isLoading, isError } = useTripQuery(tripId);
  const isMobile = useIsMobile();

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="space-y-2 text-center">
          <div className="h-6 w-48 rounded bg-muted animate-pulse mx-auto" />
          <div className="h-4 w-32 rounded bg-muted animate-pulse mx-auto" />
        </div>
      </div>
    );
  }

  if (isError || !trip) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center space-y-2">
          <p className="font-semibold">Không thể tải thông tin chuyến đi.</p>
          <p className="text-sm text-muted-foreground">
            Kiểm tra kết nối và thử lại.
          </p>
          <button
            onClick={() => window.location.reload()}
            className="mt-2 text-sm text-primary hover:underline"
          >
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col overflow-hidden">
      {/* Shared header — same for all Trip sub-pages */}
      <TripHeader trip={trip} />

      {/* Body */}
      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar — hidden on mobile (drawer handled separately) */}
        {!isMobile && <TripSidebar tripId={tripId} />}

        {/* Main content */}
        <main
          className={cn(
            "flex-1 overflow-y-auto",
            showMap && !isMobile && "overflow-hidden"
          )}
        >
          {showMap && !isMobile ? (
            // Split view: Content | Map
            <div className="flex h-full">
              <div className="flex-1 overflow-y-auto">{children}</div>
              {mapSlot && (
                <div className="w-[45%] flex-shrink-0 border-l bg-muted/20">
                  {mapSlot}
                </div>
              )}
            </div>
          ) : (
            children
          )}
        </main>
      </div>
    </div>
  );
}
