export type NavigationVehicle = "DRIVE" | "TWO_WHEELER";
export interface RoutePosition {
  latitude: number;
  longitude: number;
}
export interface NavigationStep {
  fromIndex: number;
  toIndex: number;
  distanceMeters: number;
  durationSeconds: number;
  maneuver: {
    type: string;
    streets: string[];
    roundaboutExit?: number | null;
  } | null;
}
export interface NavigationRoute {
  routeId: string;
  generatedAt: string;
  vehicle: NavigationVehicle;
  distanceMeters: number;
  durationSeconds: number;
  provider: string;
  waypoints: (RoutePosition & { name?: string })[];
  placeIds: string[];
  legs: {
    distanceMeters: number;
    durationSeconds: number;
    geometry: RoutePosition[];
    steps: NavigationStep[];
  }[];
  attributions: string[];
}
export interface TripNavigationInput {
  date: string;
  nextStopIndex: number;
  expectedPlaceIds: string[];
  vehicle: NavigationVehicle;
}
export function navigationPayload(
  tripId: string,
  input: TripNavigationInput,
  origin: RoutePosition,
) {
  if (
    !input.expectedPlaceIds.length ||
    input.expectedPlaceIds.length > 24 ||
    input.nextStopIndex < 0 ||
    input.nextStopIndex >= input.expectedPlaceIds.length
  ) {
    throw new Error("Chọn ngày có từ 1 đến 24 địa điểm và điểm đến hợp lệ.");
  }
  return {
    origin,
    vehicle: input.vehicle,
    trip: {
      tripId,
      date: input.date,
      nextStopIndex: input.nextStopIndex,
      expectedPlaceIds: input.expectedPlaceIds,
    },
  };
}
export function maneuverText(
  maneuver: NonNullable<NavigationStep["maneuver"]>,
) {
  const labels: Record<string, string> = {
    None: "Tiếp tục",
    Straight: "Đi thẳng",
    StartAt: "Xuất phát",
    StartAtRight: "Xuất phát về bên phải",
    StartAtLeft: "Xuất phát về bên trái",
    DestinationReached: "Đã tới điểm đến",
    DestinationReachedRight: "Điểm đến ở bên phải",
    DestinationReachedLeft: "Điểm đến ở bên trái",
    SlightRight: "Chếch sang phải",
    Right: "Rẽ phải",
    SharpRight: "Rẽ ngoặt sang phải",
    SlightLeft: "Chếch sang trái",
    Left: "Rẽ trái",
    SharpLeft: "Rẽ ngoặt sang trái",
    TurnAroundRight: "Quay đầu về bên phải",
    TurnAroundLeft: "Quay đầu về bên trái",
    ExitRight: "Ra lối bên phải",
    ExitLeft: "Ra lối bên trái",
    StayRight: "Giữ bên phải",
    StayLeft: "Giữ bên trái",
    Merge: "Nhập làn",
    MergeRight: "Nhập làn bên phải",
    MergeLeft: "Nhập làn bên trái",
    FerryEnter: "Lên phà",
    FerryExit: "Rời phà",
    Roundabout: "Đi vào vòng xuyến",
    EnterRoundabout: "Đi vào vòng xuyến",
    LeaveRoundabout: "Ra khỏi vòng xuyến",
  };
  const instruction =
    labels[maneuver.type] ?? "Đi theo tuyến đường trên bản đồ";
  return `${instruction}${maneuver.roundaboutExit ? `, lối ra ${maneuver.roundaboutExit}` : ""}${maneuver.streets?.length ? ` — ${maneuver.streets.join(", ")}` : ""}`;
}

export function requireGeoapifyRoute(route: NavigationRoute): NavigationRoute {
  if (route.provider !== "geoapify-routing")
    throw new Error(
      "Không nhận được tuyến đường Geoapify hợp lệ. Vui lòng thử lại.",
    );
  return route;
}
