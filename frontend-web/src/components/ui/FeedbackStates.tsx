import React from 'react';
import { Loader2, RefreshCw, AlertCircle } from 'lucide-react';

export function LoadingState({
  message = 'Đang tải dữ liệu...',
  className = '',
}: {
  message?: string;
  className?: string;
}) {
  return (
    <div className={`flex items-center justify-center py-20 min-h-[45vh] ${className}`}>
      <div className="text-center space-y-3">
        <Loader2 className="h-8 w-8 animate-spin text-cyan-500 mx-auto" />
        <p className="text-sm text-slate-500 font-medium">{message}</p>
      </div>
    </div>
  );
}

export function ErrorState({
  title = 'Đã xảy ra lỗi khi tải dữ liệu',
  message = 'Kiểm tra kết nối mạng và thử lại.',
  onRetry,
  className = '',
}: {
  title?: string;
  message?: string;
  onRetry?: () => void;
  className?: string;
}) {
  return (
    <div className={`flex flex-col items-center justify-center py-16 text-center px-4 min-h-[45vh] ${className}`}>
      <div className="rounded-full bg-red-50 p-4 mb-4">
        <AlertCircle className="h-8 w-8 text-red-500" />
      </div>
      <p className="font-semibold text-slate-900">{title}</p>
      <p className="mt-1 text-sm text-slate-500 max-w-sm">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          type="button"
          className="mt-4 inline-flex items-center gap-2 rounded-xl bg-cyan-500 px-4 py-2 text-sm font-semibold text-white hover:bg-cyan-600 active:scale-95 transition"
        >
          <RefreshCw className="h-4 w-4" />
          <span>Thử lại</span>
        </button>
      )}
    </div>
  );
}
