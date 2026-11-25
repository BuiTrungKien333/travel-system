"use client";
import { useEffect, useState, useRef } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useMemoryUIStore } from "@/features/memory/store/useMemoryUIStore";
import { useJournalUIStore } from "@/features/journal/store/useJournalUIStore";
import { useItineraryUIStore } from "@/features/itinerary/store/useItineraryUIStore";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
export function AuthSession({ children }: { children: React.ReactNode }) {
  const [ready, setReady] = useState(false);
  const authenticated = useAuthStore((s) => s.isAuthenticated);
  const userId = useAuthStore((s) => s.user?.id);
  const previousUser = useRef(userId);
  const pathname = usePathname();
  const router = useRouter();
  const queryClient = useQueryClient();
  const protectedPage = [
    "/trips",
    "/journal",
    "/memory",
    "/journey",
    "/account",
  ].some((p) => pathname === p || pathname.startsWith(`${p}/`));
  useEffect(() => {
    setReady(true);
  }, []);
  useEffect(() => {
    if (!ready) return;
    if (previousUser.current !== userId) {
      queryClient.clear();
      previousUser.current = userId;
      useMemoryUIStore.getState().closeLightbox();
      useMemoryUIStore.getState().closePhotoViewer();
      useMemoryUIStore.getState().setIsUploadOpen(false);
      useJournalUIStore.getState().setActiveMemoryForLightbox(null);
      useJournalUIStore.getState().resetFilters();
      useItineraryUIStore.getState().setPendingRecommendation(null);
    }
    if (useAuthStore.getState().token?.startsWith("mock-"))
      useAuthStore.getState().clearAuth();
    else if (protectedPage && !authenticated) router.replace("/auth/login");
  }, [ready, userId, authenticated, protectedPage, router, queryClient]);
  if (protectedPage && (!ready || !authenticated))
    return (
      <p className="p-8" role="status">
        Đang kiểm tra phiên đăng nhập...
      </p>
    );
  return children;
}
