import { TripNavigation } from "@/features/map/components/TripNavigation";
export default async function TripMapPage({
  params,
  searchParams,
}: {
  params: Promise<{ tripId: string }>;
  searchParams: Promise<{ day?: string; placeId?: string }>;
}) {
  const { tripId } = await params;
  const query = await searchParams;
  const day = Number(query.day ?? 0);
  return (
    <TripNavigation
      tripId={tripId}
      initialDay={Number.isInteger(day) && day >= 0 ? day : 0}
      initialPlaceId={query.placeId}
    />
  );
}
