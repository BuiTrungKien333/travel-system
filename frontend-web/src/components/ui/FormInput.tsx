import React, { forwardRef } from 'react';
import { type LucideIcon, AlertCircle } from 'lucide-react';

interface FormInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  icon?: LucideIcon;
  error?: string;
  isTouched?: boolean;
  required?: boolean;
  rightElement?: React.ReactNode;
}

export const FormInput = forwardRef<HTMLInputElement, FormInputProps>(
  (
    {
      label,
      icon: Icon,
      error,
      isTouched,
      required,
      rightElement,
      className = '',
      id,
      ...props
    },
    ref
  ) => {
    return (
      <div className="space-y-1.5">
        <label htmlFor={id} className="block text-xs font-bold text-slate-700">
          {label} {required && <span className="text-red-500">*</span>}
        </label>
        <div className="relative">
          {Icon && (
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
              <Icon className="h-4 w-4" />
            </div>
          )}
          <input
            id={id}
            ref={ref}
            className={`w-full rounded-xl border bg-white ${
              Icon ? 'pl-10' : 'pl-3.5'
            } ${
              rightElement ? 'pr-10' : 'pr-3.5'
            } py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none transition-all ${
              error
                ? 'border-red-400 focus:border-red-500 focus:ring-4 focus:ring-red-500/10'
                : isTouched && !error
                ? 'border-emerald-400 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-500/10'
                : 'border-slate-300 focus:border-cyan-500 focus:ring-4 focus:ring-cyan-500/10'
            } ${className}`}
            {...props}
          />
          {rightElement && (
            <div className="absolute inset-y-0 right-0 pr-3.5 flex items-center">
              {rightElement}
            </div>
          )}
        </div>
        {error && (
          <p className="text-xs text-red-600 flex items-center gap-1 mt-1 animate-fade-in font-medium">
            <AlertCircle className="h-3 w-3 shrink-0" />
            <span>{error}</span>
          </p>
        )}
      </div>
    );
  }
);

FormInput.displayName = 'FormInput';
