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
    void isTouched;
    return (
      <div className="space-y-1.5">
        <label htmlFor={id} className="block text-xs font-semibold uppercase tracking-wider text-foreground">
          {label} {required && <span className="text-destructive">*</span>}
        </label>
        <div className="relative">
          {Icon && (
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-muted-foreground">
              <Icon className="h-4 w-4" />
            </div>
          )}
          <input
            id={id}
            ref={ref}
            className={`w-full rounded-lg border bg-background ${
              Icon ? 'pl-10' : 'pl-3.5'
            } ${
              rightElement ? 'pr-10' : 'pr-3.5'
            } py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none transition-colors ${
              error
                ? 'border-destructive focus:border-destructive focus:ring-1 focus:ring-destructive'
                : 'border-input focus:border-primary focus:ring-1 focus:ring-primary'
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
          <p className="text-xs text-destructive flex items-center gap-1 mt-1 font-medium">
            <AlertCircle className="h-3 w-3 shrink-0" />
            <span>{error}</span>
          </p>
        )}
      </div>
    );
  }
);

FormInput.displayName = 'FormInput';
