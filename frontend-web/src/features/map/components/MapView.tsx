'use client';

import { APIProvider, Map, AdvancedMarker, Pin } from '@vis.gl/react-google-maps';
import { cn } from '@/lib/utils';
import { useItineraryUIStore } from '@/features/itinerary/store/useItineraryUIStore';
import type { MapMarker } from '../types';

const GOOGLE_MAPS_API_KEY = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY ?? '';

const DEFAULT_CENTER = { lat: 16.0, lng: 107.8 };
const DEFAULT_ZOOM = 6;

interface MapViewProps {
  markers: MapMarker[];
  className?: string;
}

export function MapView({ markers, className }: MapViewProps) {
  const hoveredPlaceId = useItineraryUIStore((s) => s.hoveredPlaceId);
  const selectedPlaceId = useItineraryUIStore((s) => s.selectedPlaceId);
  const setSelectedPlaceId = useItineraryUIStore((s) => s.setSelectedPlaceId);

  if (!GOOGLE_MAPS_API_KEY) {
    return (
      <div className={cn('flex items-center justify-center bg-muted text-sm text-muted-foreground', className)}>
        <div className="text-center p-4">
          <p className="font-medium">Bản đồ chưa được cấu hình</p>
          <p className="text-xs mt-1">Thêm NEXT_PUBLIC_GOOGLE_MAPS_API_KEY vào .env.local</p>
        </div>
      </div>
    );
  }

  return (
    <APIProvider apiKey={GOOGLE_MAPS_API_KEY}>
      <Map
        defaultCenter={DEFAULT_CENTER}
        defaultZoom={DEFAULT_ZOOM}
        mapId="tripzy-main-map"
        className={cn('w-full h-full', className)}
        gestureHandling="greedy"
        disableDefaultUI={false}
      >
        {markers.map((marker) => {
          const isHovered = hoveredPlaceId === marker.placeId;
          const isSelected = selectedPlaceId === marker.placeId;

          return (
            <AdvancedMarker
              key={marker.placeId}
              position={{ lat: marker.lat, lng: marker.lng }}
              title={marker.placeName}
              onClick={() => setSelectedPlaceId(marker.placeId)}
            >
              <Pin
                background={
                  marker.isVisited
                    ? '#16a34a'
                    : isSelected || isHovered
                    ? 'hsl(240 5.9% 10%)'
                    : 'hsl(240 4.8% 95.9%)'
                }
                borderColor={
                  isSelected || isHovered ? 'hsl(240 5.9% 10%)' : 'hsl(240 5.9% 90%)'
                }
                glyphColor={'white'}
                scale={isSelected || isHovered ? 1.3 : 1}
              >
                <span className="text-[10px] font-bold">{marker.order}</span>
              </Pin>
            </AdvancedMarker>
          );
        })}
      </Map>
    </APIProvider>
  );
}
