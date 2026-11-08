"use client";

import { use } from "react";
import { TripLayout } from "@/components/shared/TripLayout";

interface TripWorkspaceLayoutProps {
  children: React.ReactNode;
  params: Promise<{ tripId: string }>;
}

export default function TripWorkspaceLayout({
  children,
  params,
}: TripWorkspaceLayoutProps) {
  const resolvedParams = use(params);

  return (
    <TripLayout tripId={resolvedParams.tripId}>
      {children}
    </TripLayout>
  );
}
