'use client';

import React, { useState, useEffect, useRef } from 'react';
import type { Map as LeafletMap, TileLayer as LeafletTileLayer } from 'leaflet';
import 'leaflet/dist/leaflet.css';
import {
  RotateCcw,
  Layers,
  Compass,
  Maximize2,
  Minimize2,
  Plus,
  Minus,
} from 'lucide-react';

// Coordinates for Hà Giang (North) and Phú Quốc (South)
const HA_GIANG_COORDS: [number, number] = [22.8233, 104.9839];
const PHU_QUOC_COORDS: [number, number] = [10.2899, 103.984];
const VIETNAM_CENTER: [number, number] = [16.5, 106.5];

interface MemoryMapProps {
  className?: string;
}

export function MemoryMap({ className = '' }: MemoryMapProps) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<LeafletMap | null>(null);
  const tileLayerRef = useRef<LeafletTileLayer | null>(null);

  const [isFullscreen, setIsFullscreen] = useState(false);
  const [currentLayer, setCurrentLayer] = useState<'voyager' | 'satellite' | 'osm'>('voyager');

  useEffect(() => {
    if (!mapContainerRef.current) return;

    let isMounted = true;

    // Dynamically import Leaflet for SSR safety
    import('leaflet').then((L) => {
      if (!isMounted || !mapContainerRef.current) return;

      const container = mapContainerRef.current;
      if ('_leaflet_id' in container && container._leaflet_id) {
        return;
      }

      // Initialize map
      const map = L.map(container, {
        center: VIETNAM_CENTER,
        zoom: 5.5,
        minZoom: 4,
        maxZoom: 18,
        zoomControl: false,
        attributionControl: false,
      });

      // Default Tile Layer (CartoDB Voyager)
      const tileLayer = L.tileLayer(
        'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
        {
          subdomains: 'abcd',
          maxZoom: 19,
        }
      ).addTo(map);

      // Custom DivIcon for Hà Giang (North marker)
      const haGiangIcon = L.divIcon({
        className: 'custom-map-marker-hagiang',
        html: `
          <div style="position: relative; display: flex; flex-direction: column; align-items: center; cursor: pointer; transform: translate(-50%, -100%);">
            <div style="background: linear-gradient(135deg, #ef4444, #dc2626); color: white; padding: 4px 10px; border-radius: 9999px; font-size: 11px; font-weight: 800; box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4); border: 2px solid white; display: flex; align-items: center; gap: 4px; white-space: nowrap;">
              <span>📍 Hà Giang</span>
            </div>
            <div style="width: 10px; height: 10px; background: #dc2626; border-radius: 50%; border: 2px solid white; margin-top: -2px; box-shadow: 0 0 8px rgba(239, 68, 68, 0.8);"></div>
          </div>
        `,
        iconSize: [0, 0],
      });

      // Custom DivIcon for Phú Quốc (South marker)
      const phuQuocIcon = L.divIcon({
        className: 'custom-map-marker-phuquoc',
        html: `
          <div style="position: relative; display: flex; flex-direction: column; align-items: center; cursor: pointer; transform: translate(-50%, -100%);">
            <div style="background: linear-gradient(135deg, #ef4444, #dc2626); color: white; padding: 4px 10px; border-radius: 9999px; font-size: 11px; font-weight: 800; box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4); border: 2px solid white; display: flex; align-items: center; gap: 4px; white-space: nowrap;">
              <span>📍 Phú Quốc</span>
            </div>
            <div style="width: 10px; height: 10px; background: #dc2626; border-radius: 50%; border: 2px solid white; margin-top: -2px; box-shadow: 0 0 8px rgba(239, 68, 68, 0.8);"></div>
          </div>
        `,
        iconSize: [0, 0],
      });

      // Add Marker Hà Giang
      const markerHaGiang = L.marker(HA_GIANG_COORDS, { icon: haGiangIcon }).addTo(map);
      markerHaGiang.bindPopup(`
        <div style="font-family: inherit; padding: 4px;">
          <h4 style="font-weight: 800; font-size: 14px; margin: 0 0 4px 0; color: #0f172a;">Hà Giang</h4>
          <p style="font-size: 12px; color: #64748b; margin: 0;">Điểm cực Bắc — Cao nguyên đá Đồng Văn & Đèo Mã Pí Lèng</p>
        </div>
      `);

      // Add Marker Phú Quốc
      const markerPhuQuoc = L.marker(PHU_QUOC_COORDS, { icon: phuQuocIcon }).addTo(map);
      markerPhuQuoc.bindPopup(`
        <div style="font-family: inherit; padding: 4px;">
          <h4 style="font-weight: 800; font-size: 14px; margin: 0 0 4px 0; color: #0f172a;">Phú Quốc</h4>
          <p style="font-size: 12px; color: #64748b; margin: 0;">Đảo Ngọc Kiên Giang — Bãi Sao & Cáp treo Hòn Thơm</p>
        </div>
      `);

      // Draw Red Line connecting Hà Giang and Phú Quốc
      L.polyline([HA_GIANG_COORDS, PHU_QUOC_COORDS], {
        color: '#ef4444', // Red line connecting Ha Giang and Phu Quoc
        weight: 4,
        opacity: 0.9,
        dashArray: '8, 8', // Styled dashed travel line
        lineCap: 'round',
        lineJoin: 'round',
      }).addTo(map);

      // Fit map bounds to cover both points smoothly
      const bounds = L.latLngBounds([HA_GIANG_COORDS, PHU_QUOC_COORDS]);
      map.fitBounds(bounds, { padding: [50, 50] });

      mapInstanceRef.current = map;
      tileLayerRef.current = tileLayer;
    });

    return () => {
      isMounted = false;
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
        tileLayerRef.current = null;
      }
    };
  }, []);

  // Handle Layer switching
  useEffect(() => {
    if (!mapInstanceRef.current) return;

    import('leaflet').then((L) => {
      if (!mapInstanceRef.current) return;

      if (tileLayerRef.current) {
        mapInstanceRef.current.removeLayer(tileLayerRef.current);
      }

      let newTileUrl = 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png';
      let subdomains = 'abcd';

      if (currentLayer === 'satellite') {
        newTileUrl = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}';
        subdomains = '';
      } else if (currentLayer === 'osm') {
        newTileUrl = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
        subdomains = 'abc';
      }

      const newLayer = L.tileLayer(newTileUrl, {
        subdomains: subdomains || 'abc',
        maxZoom: 19,
      }).addTo(mapInstanceRef.current);

      tileLayerRef.current = newLayer;
    });
  }, [currentLayer]);

  // Handle Window Resize / Fullscreen transition
  useEffect(() => {
    if (mapInstanceRef.current) {
      setTimeout(() => {
        mapInstanceRef.current?.invalidateSize();
      }, 300);
    }
  }, [isFullscreen]);

  const handleZoomIn = () => {
    mapInstanceRef.current?.zoomIn();
  };

  const handleZoomOut = () => {
    mapInstanceRef.current?.zoomOut();
  };

  const handleResetBounds = () => {
    import('leaflet').then((L) => {
      if (!mapInstanceRef.current) return;
      const bounds = L.latLngBounds([HA_GIANG_COORDS, PHU_QUOC_COORDS]);
      mapInstanceRef.current.fitBounds(bounds, { padding: [50, 50] });
    });
  };

  const toggleLayer = () => {
    setCurrentLayer((prev) => {
      if (prev === 'voyager') return 'satellite';
      if (prev === 'satellite') return 'osm';
      return 'voyager';
    });
  };

  return (
    <div
      className={`relative w-full overflow-hidden rounded-3xl border border-slate-200/80 bg-slate-100 shadow-xl transition-all duration-300 ${
        isFullscreen
          ? 'fixed inset-0 z-50 rounded-none h-screen border-none'
          : 'h-[500px] lg:h-[600px]'
      } ${className}`}
    >
      {/* Leaflet DOM Container */}
      <div
        ref={mapContainerRef}
        className="h-full w-full z-0 outline-none focus:outline-none"
        tabIndex={0}
      />

      {/* Floating Top Banner Indicator */}
      <div className="absolute left-4 top-4 z-[400] flex items-center gap-2 rounded-full border border-slate-200/80 bg-white/95 px-3.5 py-1.5 text-xs font-bold text-slate-800 shadow-md backdrop-blur-md">
        <span className="flex h-2.5 w-2.5 rounded-full bg-red-500 animate-ping" />
        <span className="flex items-center gap-1.5">
          <Compass className="h-3.5 w-3.5 text-red-600" />
          <span>Tuyến kết nối: Hà Giang ⟷ Phú Quốc</span>
        </span>
      </div>

      {/* Floating Location Badges (Bottom Left) */}
      <div className="absolute bottom-4 left-4 z-[400] flex flex-col sm:flex-row items-start sm:items-center gap-2">
        <div className="flex items-center gap-2 rounded-2xl border border-slate-200/80 bg-white/95 px-3 py-2 shadow-lg backdrop-blur-md text-xs">
          <div className="flex h-7 w-7 items-center justify-center rounded-xl bg-red-500 text-white shadow-sm font-black">
            N
          </div>
          <div>
            <p className="font-extrabold text-slate-900">Hà Giang</p>
            <p className="text-[10px] text-slate-500">22.82° B, 104.98° Đ</p>
          </div>
        </div>

        <div className="flex items-center gap-2 rounded-2xl border border-slate-200/80 bg-white/95 px-3 py-2 shadow-lg backdrop-blur-md text-xs">
          <div className="flex h-7 w-7 items-center justify-center rounded-xl bg-red-500 text-white shadow-sm font-black">
            S
          </div>
          <div>
            <p className="font-extrabold text-slate-900">Phú Quốc</p>
            <p className="text-[10px] text-slate-500">10.28° B, 103.98° Đ</p>
          </div>
        </div>
      </div>

      {/* Floating Controls (Top Right) */}
      <div className="absolute right-4 top-4 z-[400] flex flex-col gap-2">
        {/* Zoom Controls */}
        <div className="flex flex-col overflow-hidden rounded-2xl border border-slate-200/80 bg-white/95 shadow-lg backdrop-blur-md">
          <button
            type="button"
            onClick={handleZoomIn}
            className="flex h-9 w-9 items-center justify-center text-slate-700 hover:bg-red-50 hover:text-red-600 transition"
            title="Phóng to"
          >
            <Plus className="h-4 w-4" />
          </button>
          <div className="h-px bg-slate-100" />
          <button
            type="button"
            onClick={handleZoomOut}
            className="flex h-9 w-9 items-center justify-center text-slate-700 hover:bg-red-50 hover:text-red-600 transition"
            title="Thu nhỏ"
          >
            <Minus className="h-4 w-4" />
          </button>
        </div>

        {/* Layer & Fullscreen Controls */}
        <div className="flex flex-col overflow-hidden rounded-2xl border border-slate-200/80 bg-white/95 shadow-lg backdrop-blur-md">
          <button
            type="button"
            onClick={toggleLayer}
            className="flex h-9 w-9 items-center justify-center text-slate-700 hover:bg-red-50 hover:text-red-600 transition"
            title={`Đổi kiểu bản đồ (Hiện tại: ${
              currentLayer === 'voyager'
                ? 'Du lịch'
                : currentLayer === 'satellite'
                ? 'Vệ tinh'
                : 'Đường phố'
            })`}
          >
            <Layers className="h-4 w-4" />
          </button>
          <div className="h-px bg-slate-100" />
          <button
            type="button"
            onClick={() => setIsFullscreen(!isFullscreen)}
            className="flex h-9 w-9 items-center justify-center text-slate-700 hover:bg-red-50 hover:text-red-600 transition"
            title={isFullscreen ? 'Thu nhỏ' : 'Toàn màn hình'}
          >
            {isFullscreen ? (
              <Minimize2 className="h-4 w-4" />
            ) : (
              <Maximize2 className="h-4 w-4" />
            )}
          </button>
          <div className="h-px bg-slate-100" />
          <button
            type="button"
            onClick={handleResetBounds}
            className="flex h-9 w-9 items-center justify-center text-slate-700 hover:bg-red-50 hover:text-red-600 transition"
            title="Căn chỉnh toàn bộ tuyến đường"
          >
            <RotateCcw className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
