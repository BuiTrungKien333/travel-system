'use client';

import { useEffect, useRef } from 'react';
import { useMap } from '@vis.gl/react-google-maps';
import type { TravelLocation } from '@/features/journal/types';

interface JourneyRouteProps {
  locations: TravelLocation[];
  strokeColor?: string;
  strokeWeight?: number;
  strokeOpacity?: number;
}

export function JourneyRoute({
  locations,
  strokeColor = '#06b6d4',
  strokeWeight = 4,
  strokeOpacity = 0.85,
}: JourneyRouteProps) {
  const map = useMap();
  const polylineRef = useRef<google.maps.Polyline | null>(null);

  useEffect(() => {
    if (!map || locations.length < 2 || typeof google === 'undefined' || !google.maps) {
      if (polylineRef.current) {
        polylineRef.current.setMap(null);
        polylineRef.current = null;
      }
      return;
    }

    // Sort locations chronologically
    const sortedLocations = [...locations].sort((a, b) => {
      if (a.order !== undefined && b.order !== undefined) {
        return a.order - b.order;
      }
      return new Date(a.visitedAt).getTime() - new Date(b.visitedAt).getTime();
    });

    const path = sortedLocations.map((loc) => ({
      lat: loc.latitude,
      lng: loc.longitude,
    }));

    if (polylineRef.current) {
      polylineRef.current.setPath(path);
      polylineRef.current.setMap(map);
    } else {
      // Create Polyline with smooth style & directional arrow
      const lineSymbol = {
        path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW,
        scale: 2.5,
        strokeColor: '#0891b2',
        fillColor: '#06b6d4',
        fillOpacity: 1,
      };

      const polyline = new google.maps.Polyline({
        path,
        geodesic: true,
        strokeColor,
        strokeOpacity,
        strokeWeight,
        icons: [
          {
            icon: lineSymbol,
            offset: '50%',
            repeat: '100px',
          },
        ],
      });

      polyline.setMap(map);
      polylineRef.current = polyline;
    }

    return () => {
      if (polylineRef.current) {
        polylineRef.current.setMap(null);
        polylineRef.current = null;
      }
    };
  }, [map, locations, strokeColor, strokeWeight, strokeOpacity]);

  return null;
}
