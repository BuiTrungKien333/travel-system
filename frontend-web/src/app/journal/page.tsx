"use client";

import React, { useMemo } from "react";
import { useJournalDataQuery } from "@/features/journal/api/useJournalDataQuery";
import { useJournalUIStore } from "@/features/journal/store/useJournalUIStore";
import {
  JournalHero,
  JournalToolbar,
  JournalMap,
  TravelStats,
  RecentMemories,
  JourneyGrid,
  JournalMemoryModal,
} from "@/components/journal";
import type { Journey, TravelLocation, Memory } from "@/features/journal/types";

export default function JournalPage() {
  const { data, isLoading, isError, error, refetch } = useJournalDataQuery();

  const searchQuery = useJournalUIStore((s) => s.searchQuery);
  const selectedProvince = useJournalUIStore((s) => s.selectedProvince);
  const selectedJourneyId = useJournalUIStore((s) => s.selectedJourneyId);
  const sortBy = useJournalUIStore((s) => s.sortBy);
  const setSelectedLocationId = useJournalUIStore(
    (s) => s.setSelectedLocationId,
  );
  const setActiveMemoryForLightbox = useJournalUIStore(
    (s) => s.setActiveMemoryForLightbox,
  );

  const rawJourneys: Journey[] = useMemo(
    () => data?.journeys || [],
    [data?.journeys],
  );
  const stats = useMemo(
    () =>
      data?.stats || {
        totalProvinces: 0,
        totalPlaces: 0,
        totalMemories: 0,
        totalJourneys: 0,
        exploredProvinces: [],
      },
    [data?.stats],
  );

  // Collect all unique provinces across all journeys
  const allProvinces = useMemo(() => {
    const provinceSet = new Set<string>();
    rawJourneys.forEach((j) => {
      j.provinces?.forEach((p) => provinceSet.add(p));
      j.locations?.forEach((l) => {
        if (l.province) provinceSet.add(l.province);
      });
    });
    return Array.from(provinceSet).sort();
  }, [rawJourneys]);

  // Filter & Sort Journeys
  const filteredJourneys = useMemo(() => {
    let result = [...rawJourneys];

    // Filter by Selected Journey ID
    if (selectedJourneyId) {
      result = result.filter((j) => j.id === selectedJourneyId);
    }

    // Filter by Province
    if (selectedProvince) {
      result = result.filter(
        (j) =>
          j.provinces?.some((p) =>
            p.toLowerCase().includes(selectedProvince.toLowerCase()),
          ) ||
          j.locations?.some(
            (l) =>
              l.province &&
              l.province.toLowerCase().includes(selectedProvince.toLowerCase()),
          ),
      );
    }

    // Filter by Search Query
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      result = result.filter((j) => {
        const matchTitle = j.title?.toLowerCase().includes(q);
        const matchDesc = j.description?.toLowerCase().includes(q);
        const matchProvince = j.provinces?.some((p) =>
          p.toLowerCase().includes(q),
        );
        const matchLocation = j.locations?.some(
          (l) =>
            l.name?.toLowerCase().includes(q) ||
            (l.address && l.address.toLowerCase().includes(q)),
        );
        const matchMemory = j.memories?.some(
          (m) =>
            m.title?.toLowerCase().includes(q) ||
            (m.note && m.note.toLowerCase().includes(q)),
        );

        return (
          matchTitle ||
          matchDesc ||
          matchProvince ||
          matchLocation ||
          matchMemory
        );
      });
    }

    // Sorting
    result.sort((a, b) => {
      if (sortBy === "newest") {
        return (
          new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
        );
      }
      if (sortBy === "oldest") {
        return (
          new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
        );
      }
      if (sortBy === "most_visited") {
        return b.locations.length - a.locations.length;
      }
      if (sortBy === "most_memories") {
        const memA = a.memories?.length || 0;
        const memB = b.memories?.length || 0;
        return memB - memA;
      }
      return 0;
    });

    return result;
  }, [rawJourneys, selectedJourneyId, selectedProvince, searchQuery, sortBy]);

  // All locations derived from filtered journeys
  const allLocations: TravelLocation[] = useMemo(() => {
    return filteredJourneys.flatMap((j) => j.locations);
  }, [filteredJourneys]);

  // Selected Journey object
  const selectedJourney = useMemo(() => {
    if (!selectedJourneyId) return null;
    return rawJourneys.find((j) => j.id === selectedJourneyId) || null;
  }, [selectedJourneyId, rawJourneys]);

  // All memories flat list for lightbox & recent section
  const allMemories: Memory[] = useMemo(() => {
    const list = filteredJourneys.flatMap((j) => j.memories || []);
    return list.sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
  }, [filteredJourneys]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-12 space-y-8 animate-pulse">
          <div className="h-28 rounded-3xl bg-slate-200" />
          <div className="h-12 rounded-2xl bg-slate-200" />
          <div className="h-[500px] rounded-3xl bg-slate-200" />
          <div className="grid grid-cols-4 gap-4">
            <div className="h-28 rounded-2xl bg-slate-200" />
            <div className="h-28 rounded-2xl bg-slate-200" />
            <div className="h-28 rounded-2xl bg-slate-200" />
            <div className="h-28 rounded-2xl bg-slate-200" />
          </div>
        </div>
      </div>
    );
  }

  if (isError)
    return (
      <div className="p-8 text-center">
        <p role="alert">{error?.message ?? "Không thể tải nhật ký."}</p>
        <button
          onClick={() => refetch()}
          className="mt-4 rounded bg-primary px-4 py-2 text-primary-foreground"
        >
          Thử lại
        </button>
      </div>
    );

  return (
    <main className="min-h-screen bg-white text-slate-900 selection:bg-cyan-500 selection:text-white">
      {/* 1. Header / Hero */}
      <JournalHero
        totalJourneys={stats.totalJourneys}
        totalPlaces={stats.totalPlaces}
      />

      {/* 2. Search & Filter Toolbar */}
      <JournalToolbar
        journeys={rawJourneys}
        allProvinces={allProvinces}
        totalResultsCount={filteredJourneys.length}
      />

      {/* 3. Section 1: Interactive Timeline Map */}
      <section
        id="journal-interactive-map"
        className="py-6 sm:py-8 bg-slate-50/50"
      >
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <JournalMap
            journeys={filteredJourneys}
            allLocations={allLocations}
            selectedJourney={selectedJourney}
            onSelectLocation={(loc) => setSelectedLocationId(loc.id)}
            onViewMemories={(loc) => {
              const matchedMemory = allMemories.find(
                (m) => m.locationId === loc.id,
              );
              if (matchedMemory) {
                setActiveMemoryForLightbox(matchedMemory);
              } else if (allMemories.length > 0) {
                setActiveMemoryForLightbox(allMemories[0]);
              }
            }}
          />
        </div>
      </section>

      {/* 4. Section 2: Travel Stats */}
      <TravelStats stats={stats} />

      {/* 5. Section 3: Recent Memories */}
      {allMemories.length > 0 && (
        <RecentMemories
          memories={allMemories}
          onSelectMemory={(mem) => setActiveMemoryForLightbox(mem)}
        />
      )}

      {/* 6. Section 4: Journey Cards */}
      <JourneyGrid journeys={filteredJourneys} />

      {/* Lightbox Modal for Photo & Memory Details */}
      <JournalMemoryModal allMemories={allMemories} />
    </main>
  );
}
