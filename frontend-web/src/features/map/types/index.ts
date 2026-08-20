export interface MapMarker {
  placeId: string;
  placeName: string;
  lat: number;
  lng: number;
  dayIndex: number;
  order: number;
  isVisited?: boolean;
}

export interface MapViewConfig {
  center?: { lat: number; lng: number };
  zoom?: number;
  markers: MapMarker[];
  activeDayIndex?: number;
}
