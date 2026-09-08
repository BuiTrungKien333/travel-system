'use client';

import React from 'react';
import { Compass } from 'lucide-react';
import { JourneyCard } from './JourneyCard';
import { EmptyJournal } from './EmptyJournal';
import { useJournalUIStore } from '@/features/journal/store/useJournalUIStore';
import type { Journey } from '@/features/journal/types';

interface JourneyGridProps {
  journeys: Journey[];
  onSelectJourney?: (journey: Journey) => void;
}

export function JourneyGrid({ journeys, onSelectJourney }: JourneyGridProps) {
  const selectedJourneyId = useJournalUIStore((s) => s.selectedJourneyId);
  const setSelectedJourneyId = useJournalUIStore((s) => s.setSelectedJourneyId);
  const resetFilters = useJournalUIStore((s) => s.resetFilters);

  const handleSelect = (journey: Journey) => {
    if (selectedJourneyId === journey.id) {
      setSelectedJourneyId(null);
    } else {
      setSelectedJourneyId(journey.id);
    }

    if (onSelectJourney) {
      onSelectJourney(journey);
    }

    // Scroll up smoothly to map section
    const mapElement = document.getElementById('journal-interactive-map');
    if (mapElement) {
      mapElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  };

  return (
    <section id="journal-journeys-grid" className="py-10">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-2 mb-6">
          <div>
            <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-cyan-600 mb-1">
              <Compass className="h-3.5 w-3.5" />
              <span>Bộ sưu tập hành trình</span>
            </div>
            <h2 className="text-2xl font-black tracking-tight text-slate-900 sm:text-3xl">
              Tất cả các chuyến đi ({journeys.length})
            </h2>
            <p className="mt-1 text-xs sm:text-sm text-slate-500">
              Chọn bất kỳ chuyến đi nào để xem toàn bộ lộ trình và điểm đến được trực quan hóa trên bản đồ.
            </p>
          </div>
        </div>

        {/* Grid or Empty */}
        {journeys.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-5 sm:gap-6">
            {journeys.map((journey) => (
              <JourneyCard
                key={journey.id}
                journey={journey}
                isSelected={selectedJourneyId === journey.id}
                onSelect={handleSelect}
              />
            ))}
          </div>
        ) : (
          <EmptyJournal
            type="no_results"
            onResetFilters={resetFilters}
          />
        )}
      </div>
    </section>
  );
}
