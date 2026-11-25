import React from 'react';
import { RefreshCw, AlertTriangle, Compass } from 'lucide-react';
import Link from 'next/link';

export function LoadingState({
  message = 'Đang tải dữ liệu...',
  className = '',
}: {
  message?: string;
  className?: string;
}) {
  return (
    <div className={`flex flex-col items-center justify-center p-8 min-h-[35vh] w-full ${className}`}>
      <div className="w-full max-w-sm space-y-3 animate-pulse">
        <div className="h-32 bg-muted rounded-xl w-full" />
        <div className="space-y-2 pt-2">
          <div className="h-3.5 bg-muted rounded w-3/4" />
          <div className="h-3 bg-muted rounded w-1/2" />
        </div>
      </div>
      <p className="mt-6 text-xs text-muted-foreground font-medium">{message}</p>
    </div>
  );
}

export function ErrorState({
  title = 'Đã xảy ra lỗi',
  message = 'Chúng tôi không thể tải dữ liệu lúc này. Vui lòng thử lại sau.',
  onRetry,
  className = '',
}: {
  title?: string;
  message?: string;
  onRetry?: () => void;
  className?: string;
}) {
  return (
    <div className={`flex flex-col items-center justify-center py-12 text-center px-4 min-h-[35vh] ${className}`}>
      <div className="rounded-full bg-destructive/10 p-3 mb-4 text-destructive">
        <AlertTriangle className="h-6 w-6" />
      </div>
      <h3 className="text-base font-bold text-foreground mb-1.5">{title}</h3>
      <p className="text-xs text-muted-foreground max-w-sm mb-5 leading-relaxed">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          type="button"
          className="inline-flex items-center gap-1.5 rounded-lg bg-foreground px-4 py-2 text-xs font-semibold text-background hover:bg-foreground/90 transition-colors shadow-xs"
        >
          <RefreshCw className="h-3.5 w-3.5" />
          <span>Thử lại</span>
        </button>
      )}
    </div>
  );
}

export function EmptyState({
  title,
  message,
  actionLabel,
  actionHref,
  className = '',
}: {
  title: string;
  message: string;
  actionLabel?: string;
  actionHref?: string;
  className?: string;
}) {
  return (
    <div className={`flex flex-col items-center justify-center py-12 text-center px-4 border border-dashed border-border/70 rounded-xl bg-card ${className}`}>
      <div className="rounded-full bg-muted/50 p-3 mb-4 text-primary">
        <Compass className="h-8 w-8" strokeWidth={1.5} />
      </div>
      <h3 className="text-base font-bold text-foreground mb-1">{title}</h3>
      <p className="text-xs text-muted-foreground max-w-sm mb-5 leading-relaxed">{message}</p>
      {actionLabel && actionHref && (
        <Link
          href={actionHref}
          className="inline-flex items-center justify-center rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground hover:bg-primary/90 transition-colors shadow-xs"
        >
          {actionLabel}
        </Link>
      )}
    </div>
  );
}
