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
import { useJournalUIStore } from '@/features/journal/store/useJournalUIStore';
import type { Journey, TravelLocation } from '@/features/journal/types';

const VIETNAM_CENTER: [number, number] = [16.0544, 108.2022]; // Center of Vietnam (Da Nang coordinates)
const DEFAULT_ZOOM = 6;

interface JournalMapProps {
  journeys?: Journey[];
  allLocations?: TravelLocation[];
  selectedJourney?: Journey | null;
  onSelectLocation?: (location: TravelLocation) => void;
  onViewMemories?: (location: TravelLocation) => void;
  className?: string;
}

export function JournalMap({ className = '' }: JournalMapProps) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<LeafletMap | null>(null);
  const tileLayerRef = useRef<LeafletTileLayer | null>(null);

  const isMapFullscreen = useJournalUIStore((s) => s.isMapFullscreen);
  const setIsMapFullscreen = useJournalUIStore((s) => s.setIsMapFullscreen);

  const [currentLayer, setCurrentLayer] = useState<'voyager' | 'satellite' | 'osm'>('voyager');

  // Initialize Leaflet Map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    let isMounted = true;

    // Dynamically import Leaflet to ensure SSR safety in Next.js
    import('leaflet').then((L) => {
      if (!isMounted || !mapContainerRef.current) return;

      const container = mapContainerRef.current;
      // Prevent re-initialization if container is already initialized
      if ('_leaflet_id' in container && container._leaflet_id) {
        return;
      }

      const map = L.map(container, {
        center: VIETNAM_CENTER,
        zoom: DEFAULT_ZOOM,
        minZoom: 4,
        maxZoom: 18,
        zoomControl: false, // We use custom styled zoom buttons
        attributionControl: false,
      });

      // Default Travel-styled Tile Layer (CartoDB Voyager)
      const tileLayer = L.tileLayer(
        'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
        {
          subdomains: 'abcd',
          maxZoom: 19,
        }
      ).addTo(map);

      // Add custom scale control
      L.control.scale({ imperial: false, position: 'bottomright' }).addTo(map);

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
  }, [isMapFullscreen]);

  // Map Controls
  const handleZoomIn = () => {
    mapInstanceRef.current?.zoomIn();
  };

  const handleZoomOut = () => {
    mapInstanceRef.current?.zoomOut();
  };

  const handleResetView = () => {
    mapInstanceRef.current?.setView(VIETNAM_CENTER, DEFAULT_ZOOM, {
      animate: true,
      duration: 0.8,
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
        isMapFullscreen
          ? 'fixed inset-0 z-50 rounded-none h-screen border-none'
          : 'h-[520px] lg:h-[620px]'
      } ${className}`}
    >
      {/* Leaflet DOM Container */}
      <div
        ref={mapContainerRef}
        className="h-full w-full z-0 outline-none focus:outline-none"
        tabIndex={0}
      />

      {/* Floating Top Banner */}
      <div className="absolute left-4 top-4 z-[400] flex items-center gap-2 rounded-full border border-slate-200/80 bg-white/90 px-3.5 py-1.5 text-xs font-bold text-slate-800 shadow-md backdrop-blur-md">
        <span className="flex h-2 w-2 rounded-full bg-cyan-500 animate-pulse" />
        <span className="flex items-center gap-1.5">
          <Compass className="h-3.5 w-3.5 text-cyan-600" />
          <span>Bản đồ hành trình tương tác (Leaflet)</span>
        </span>
      </div>

      {/* Floating Controls (Top Right) */}
      <div className="absolute right-4 top-4 z-[400] flex flex-col gap-2">
        {/* Zoom Controls */}
        <div className="flex flex-col overflow-hidden rounded-2xl border border-slate-200/80 bg-white/95 shadow-lg backdrop-blur-md">
          <button
            type="button"
            onClick={handleZoomIn}
            className="flex h-9 w-9 items-center justify-center text-slate-700 hover:bg-cyan-50 hover:text-cyan-600 transition"
            title="Phóng to"
          >
            <Plus className="h-4 w-4" />
          </button>
          <div className="h-px bg-slate-100" />
          <button
            type="button"
            onClick={handleZoomOut}
            className="flex h-9 w-9 items-center justify-center text-slate-700 hover:bg-cyan-50 hover:text-cyan-600 transition"
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
            className="flex h-9 w-9 items-center justify-center text-slate-700 hover:bg-cyan-50 hover:text-cyan-600 transition"
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
            onClick={() => setIsMapFullscreen(!isMapFullscreen)}
            className="flex h-9 w-9 items-center justify-center text-slate-700 hover:bg-cyan-50 hover:text-cyan-600 transition"
            title={isMapFullscreen ? 'Thu nhỏ' : 'Toàn màn hình'}
          >
            {isMapFullscreen ? (
              <Minimize2 className="h-4 w-4" />
            ) : (
              <Maximize2 className="h-4 w-4" />
            )}
          </button>
          <div className="h-px bg-slate-100" />
          <button
            type="button"
            onClick={handleResetView}
            className="flex h-9 w-9 items-center justify-center text-slate-700 hover:bg-cyan-50 hover:text-cyan-600 transition"
            title="Xem lại toàn cảnh Việt Nam"
          >
            <RotateCcw className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Subtle Bottom Attribution / Layer Badge */}
      <div className="absolute bottom-3 left-4 z-[400] hidden sm:flex items-center gap-2 rounded-xl bg-black/40 px-2.5 py-1 text-[10px] font-medium text-white/90 backdrop-blur-sm">
        <span>Bản đồ {currentLayer === 'voyager' ? 'Voyager' : currentLayer === 'satellite' ? 'Vệ tinh' : 'OpenStreetMap'}</span>
        <span className="opacity-50">•</span>
        <span>Leaflet Engine</span>
      </div>
    </div>
  );
}
