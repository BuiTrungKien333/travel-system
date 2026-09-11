'use client';

import { useEffect, useRef } from 'react';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import { cn } from '@/lib/utils';
import { useItineraryUIStore } from '@/features/itinerary/store/useItineraryUIStore';
import type { MapMarker } from '../types';

const MAPBOX_TOKEN =
  process.env.NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN ||
  'pk.eyJ1IjoiZGFpc3VraTA5NTEiLCJhIjoiY210d3BrcHB3MDEwbDJ4cXp1anI0b3d0cSJ9.EgIkKbRMop9DYAUJ5SF4_A';

mapboxgl.accessToken = MAPBOX_TOKEN;

const DEFAULT_CENTER: [number, number] = [107.8, 16.0]; // [lng, lat] (Mapbox coordinates format)
const DEFAULT_ZOOM = 5.5;

interface MapboxViewProps {
  markers?: MapMarker[];
  className?: string;
  styleUrl?: string; // mapbox://styles/mapbox/streets-v12, outdoors-v12, etc.
}

export function MapboxView({
  markers = [],
  className,
  styleUrl = 'mapbox://styles/mapbox/streets-v12',
}: MapboxViewProps) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const markersRef = useRef<mapboxgl.Marker[]>([]);

  const hoveredPlaceId = useItineraryUIStore((s) => s.hoveredPlaceId);
  const selectedPlaceId = useItineraryUIStore((s) => s.selectedPlaceId);
  const setSelectedPlaceId = useItineraryUIStore((s) => s.setSelectedPlaceId);

  // 1. Khởi tạo bản đồ Mapbox
  useEffect(() => {
    if (!mapContainerRef.current) return;
    if (!mapboxgl.accessToken) return;

    const map = new mapboxgl.Map({
      container: mapContainerRef.current,
      style: styleUrl,
      center: DEFAULT_CENTER,
      zoom: DEFAULT_ZOOM,
      attributionControl: false,
    });

    // Thêm các nút điều khiển thu phóng và toàn màn hình
    map.addControl(new mapboxgl.NavigationControl({ visualizePitch: true }), 'top-right');
    map.addControl(new mapboxgl.FullscreenControl(), 'top-right');

    // Đảm bảo bản đồ resize đúng kích thước sau khi load xong
    map.on('load', () => {
      map.resize();
    });

    // Lắng nghe thay đổi kích thước của khung chứa (vd: khi resize panel)
    const resizeObserver = new ResizeObserver(() => {
      if (mapRef.current) {
        mapRef.current.resize();
      }
    });

    if (mapContainerRef.current) {
      resizeObserver.observe(mapContainerRef.current);
    }

    mapRef.current = map;

    return () => {
      resizeObserver.disconnect();
      map.remove();
      mapRef.current = null;
    };
  }, [styleUrl]);

  // 2. Cập nhật các Markers khi danh sách markers thay đổi
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    // Xóa marker cũ
    markersRef.current.forEach((m) => m.remove());
    markersRef.current = [];

    if (!markers || markers.length === 0) return;

    const bounds = new mapboxgl.LngLatBounds();
    let hasValidCoords = false;

    markers.forEach((marker, index) => {
      if (typeof marker.lng !== 'number' || typeof marker.lat !== 'number') return;

      hasValidCoords = true;
      const isSelected = selectedPlaceId === marker.placeId;
      const isHovered = hoveredPlaceId === marker.placeId;

      // Tạo Custom Marker DOM element
      const el = document.createElement('div');
      el.className = 'group flex flex-col items-center cursor-pointer transition-transform duration-200';

      const pinColor = marker.isVisited
        ? '#16a34a'
        : isSelected || isHovered
        ? '#ff385c'
        : '#2563eb';

      el.innerHTML = `
        <div style="background-color: ${pinColor}; transform: scale(${isSelected || isHovered ? 1.2 : 1});"
             class="w-7 h-7 rounded-full text-white text-xs font-bold flex items-center justify-center shadow-lg border-2 border-white transition-all">
          ${marker.order ?? index + 1}
        </div>
      `;

      // Tạo Popup khi click
      const popup = new mapboxgl.Popup({ offset: 25, closeButton: false }).setHTML(`
        <div style="padding: 4px; font-family: sans-serif;">
          <p style="font-weight: 600; font-size: 13px; color: #111827; margin: 0;">${marker.placeName}</p>
          <p style="font-size: 11px; color: #6b7280; margin-top: 2px; margin-bottom: 0;">Điểm dừng số ${marker.order ?? index + 1}</p>
        </div>
      `);

      el.addEventListener('click', () => {
        setSelectedPlaceId(marker.placeId);
      });

      const mapboxMarker = new mapboxgl.Marker(el)
        .setLngLat([marker.lng, marker.lat])
        .setPopup(popup)
        .addTo(map);

      markersRef.current.push(mapboxMarker);
      bounds.extend([marker.lng, marker.lat]);
    });

    // Fit bản đồ theo các điểm marker
    if (hasValidCoords && !bounds.isEmpty()) {
      if (markers.length > 1) {
        map.fitBounds(bounds, { padding: 60, maxZoom: 14, duration: 800 });
      } else if (markers.length === 1) {
        map.flyTo({ center: [markers[0].lng, markers[0].lat], zoom: 12, duration: 800 });
      }
    }
  }, [markers, selectedPlaceId, hoveredPlaceId, setSelectedPlaceId]);

  if (!MAPBOX_TOKEN) {
    return (
      <div className={cn('flex items-center justify-center bg-muted text-sm text-muted-foreground p-6', className)}>
        <div className="text-center">
          <p className="font-semibold text-foreground">Chưa cấu hình Mapbox Token</p>
          <p className="text-xs mt-1">Vui lòng thêm <code>NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN</code> vào <code>.env.local</code></p>
        </div>
      </div>
    );
  }

  return (
    <div className={cn('relative w-full h-full min-h-[400px] overflow-hidden', className)}>
      <div ref={mapContainerRef} className="absolute inset-0 w-full h-full" />
    </div>
  );
}