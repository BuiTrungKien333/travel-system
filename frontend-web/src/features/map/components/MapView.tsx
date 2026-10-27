'use client';

import { MapboxView } from './MapBoxView';
import type { MapMarker } from '../types';

interface MapViewProps {
  markers: MapMarker[];
  className?: string;
}

export function MapView({ markers, className }: MapViewProps) {
  return <MapboxView markers={markers} className={className} />;
}
