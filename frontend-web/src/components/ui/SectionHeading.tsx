import React from 'react';
import type { LucideIcon } from 'lucide-react';

interface SectionHeadingProps {
  badgeIcon?: LucideIcon;
  badgeText?: string;
  title: string;
  highlight?: string;
  subtitle?: string;
  align?: 'left' | 'center';
  className?: string;
}

export function SectionHeading({
  badgeIcon: Icon,
  badgeText,
  title,
  highlight,
  subtitle,
  align = 'center',
  className = '',
}: SectionHeadingProps) {
  const isCenter = align === 'center';
  return (
    <div
      className={`${
        isCenter ? 'mx-auto max-w-3xl text-center' : 'max-w-2xl text-left'
      } ${className}`}
    >
      {badgeText && (
        <div className="inline-flex items-center gap-2 rounded-full border border-cyan-200 bg-cyan-50 px-3.5 py-1 text-xs font-semibold text-cyan-800">
          {Icon && <Icon className="h-3.5 w-3.5 text-cyan-600" />}
          <span>{badgeText}</span>
        </div>
      )}
      <h2 className="mt-4 text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl leading-tight">
        {title} {highlight && <span className="text-cyan-600">{highlight}</span>}
      </h2>
      {subtitle && (
        <p className="mt-3 text-base text-slate-600 sm:text-lg leading-relaxed">
          {subtitle}
        </p>
      )}
    </div>
  );
}
