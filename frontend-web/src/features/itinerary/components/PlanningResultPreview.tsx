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
    <div className="rounded-xl border border-primary/30 bg-card p-4 space-y-3 shadow-xs">
      <div className="flex items-start gap-2.5">
        <div className={cn(
          'shrink-0 rounded-md p-1.5 mt-0.5',
          isAI ? 'bg-primary/10 text-primary' : 'bg-muted text-muted-foreground'
        )}>
          {isAI ? (
            <Sparkles className="h-4 w-4" />
          ) : (
            <Navigation className="h-4 w-4" />
          )}
        </div>
        <div className="min-w-0">
          <p className="text-xs font-semibold text-foreground">
            {isAI ? 'Đề xuất lịch trình từ AI' : 'Lịch trình cơ bản'}
          </p>
          <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">
            {recommendation.summary}
          </p>
        </div>
      </div>

      {recommendation.weatherWarnings && recommendation.weatherWarnings.length > 0 && (
        <div className="rounded-md bg-amber-500/10 border border-amber-500/20 px-3 py-2">
          {recommendation.weatherWarnings.map((w, i) => (
            <p key={i} className="text-xs text-amber-800 dark:text-amber-200">{w}</p>
          ))}
        </div>
      )}

      {recommendation.notes && recommendation.notes.length > 0 && (
        <ul className="space-y-1 text-xs text-muted-foreground pt-1">
          {recommendation.notes.map((note, i) => (
            <li key={i} className="flex items-start gap-1.5">
              <span className="mt-1 h-1 w-1 rounded-full bg-muted-foreground/60 shrink-0" />
              <span>{note}</span>
            </li>
          ))}
        </ul>
      )}

      <div className="flex gap-2 pt-2 border-t border-border/40">
        <button
          onClick={onApply}
          className="flex-1 flex items-center justify-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground hover:bg-primary/90 transition-colors shadow-xs"
        >
          <Check className="h-3.5 w-3.5" />
          <span>Áp dụng</span>
        </button>
        <button
          onClick={onIgnore}
          className="flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-muted/50 transition-colors"
        >
          <X className="h-3.5 w-3.5" />
          <span>Bỏ qua</span>
        </button>
      </div>
    </div>
  );
}
