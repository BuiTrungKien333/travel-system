'use client';

import { Sparkles, Navigation, X, Check } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { AIItineraryRecommendation } from '../types';

interface PlanningResultPreviewProps {
  recommendation: AIItineraryRecommendation;
  onApply: () => void;
  onIgnore: () => void;
}

export function PlanningResultPreview({
  recommendation,
  onApply,
  onIgnore,
}: PlanningResultPreviewProps) {
  const isAI = recommendation.type === 'AI';

  return (
    <div className="rounded-xl border-2 border-primary/30 bg-primary/5 p-4 space-y-3">
      <div className="flex items-start gap-2">
        <div className={cn(
          'flex-shrink-0 rounded-full p-1.5',
          isAI ? 'bg-primary/10' : 'bg-muted'
        )}>
          {isAI ? (
            <Sparkles className="h-4 w-4 text-primary" />
          ) : (
            <Navigation className="h-4 w-4 text-muted-foreground" />
          )}
        </div>
        <div className="min-w-0">
          <p className="text-sm font-semibold">
            {isAI ? '✨ AI đề xuất' : '🧭 Lịch trình cơ bản'}
          </p>
          <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">
            {recommendation.summary}
          </p>
        </div>
      </div>

      {recommendation.weatherWarnings && recommendation.weatherWarnings.length > 0 && (
        <div className="rounded-md bg-yellow-50 border border-yellow-200 px-3 py-2">
          {recommendation.weatherWarnings.map((w, i) => (
            <p key={i} className="text-xs text-yellow-800">{w}</p>
          ))}
        </div>
      )}

      {recommendation.notes && recommendation.notes.length > 0 && (
        <ul className="space-y-1">
          {recommendation.notes.map((note, i) => (
            <li key={i} className="text-xs text-muted-foreground flex items-start gap-1.5">
              <span className="mt-0.5 h-1.5 w-1.5 rounded-full bg-muted-foreground/50 flex-shrink-0" />
              {note}
            </li>
          ))}
        </ul>
      )}

      <div className="flex gap-2 pt-1">
        <button
          onClick={onApply}
          className="flex-1 flex items-center justify-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90 transition"
        >
          <Check className="h-3.5 w-3.5" />
          Áp dụng
        </button>
        <button
          onClick={onIgnore}
          className="flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted transition"
        >
          <X className="h-3.5 w-3.5" />
          Bỏ qua
        </button>
      </div>
    </div>
  );
}
