import Link from 'next/link';
import { Compass } from 'lucide-react';
import { ROUTES } from '@/constants';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center bg-background py-12 px-4 min-h-[75vh]">
      <div className="mb-8 text-center space-y-2">
        <Link href={ROUTES.home} className="inline-flex items-center gap-2.5 group">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Compass className="h-5 w-5" />
          </div>
          <span className="font-bold text-2xl tracking-tight text-foreground">WnGo</span>
        </Link>
        <p className="text-xs text-muted-foreground font-medium">Plan less. Travel more. Remember everything.</p>
      </div>

      <div className="w-full max-w-md">
        <div className="rounded-xl border border-border bg-card p-6 sm:p-8 shadow-xs">
          {children}
        </div>
      </div>
    </div>
  );
}
