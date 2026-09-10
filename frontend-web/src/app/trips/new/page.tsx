"use client";

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useState } from 'react';
import { ArrowLeft, Loader2, ChevronDown, ChevronUp } from 'lucide-react';
import { createTripSchema, type CreateTripInput } from '@/features/trip/schema';
import { useCreateTripMutation } from '@/features/trip/api/useCreateTripMutation';
import {
  ROUTES,
  TRANSPORTATION_MODES,
  TRANSPORTATION_LABELS,
  TRAVEL_INTERESTS,
  TRAVEL_INTEREST_LABELS,
} from '@/constants';

export default function NewTripPage() {
  const router = useRouter();
  const createTripMutation = useCreateTripMutation();
  const [showAdvanced, setShowAdvanced] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isValid },
  } = useForm<CreateTripInput>({
    resolver: zodResolver(createTripSchema),
    defaultValues: {
      preferences: [],
    },
    mode: "onChange",
  });

  const selectedPreferences = watch('preferences') ?? [];

  const togglePreference = (interest: string) => {
    const next = selectedPreferences.includes(interest)
      ? selectedPreferences.filter((p) => p !== interest)
      : [...selectedPreferences, interest];
    setValue('preferences', next);
  };

  const onSubmit = (data: CreateTripInput) => {
    createTripMutation.mutate(
      {
        ...data,
      },
      {
        onSuccess: (trip) => {
          router.push(ROUTES.trip(trip.id));
        },
      }
    );
  };

  return (
    <div className="min-h-screen bg-background">
      <main className="mx-auto max-w-2xl px-4 sm:px-6 py-8 sm:py-12">
        
        {/* Back Link & Header */}
        <div className="mb-8 space-y-3">
          <Link
            href={ROUTES.trips}
            className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Quay lại danh sách chuyến đi</span>
          </Link>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
            Lên kế hoạch chuyến đi mới
          </h1>
          <p className="text-sm text-muted-foreground">
            Chỉ cần điểm đến và ngày dự kiến. Bạn có thể thêm địa điểm và tối ưu lịch trình sau.
          </p>
        </div>

        {/* Creation Form */}
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="rounded-xl border border-border bg-card p-6 sm:p-8 space-y-6 shadow-xs"
        >
          {/* Core Fields */}
          <div className="space-y-4">
            <div className="space-y-1.5">
              <label htmlFor="name" className="text-xs font-semibold uppercase tracking-wider text-foreground">
                Tên chuyến đi <span className="text-destructive">*</span>
              </label>
              <input
                id="name"
                type="text"
                placeholder="Ví dụ: Khám phá Đà Nẵng 4 ngày 3 đêm"
                className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-colors"
                {...register('name')}
              />
              {errors.name && (
                <p className="text-xs text-destructive">{errors.name.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <label htmlFor="destination" className="text-xs font-semibold uppercase tracking-wider text-foreground">
                Điểm đến <span className="text-destructive">*</span>
              </label>
              <input
                id="destination"
                type="text"
                placeholder="Ví dụ: Đà Nẵng, Hội An"
                className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-colors"
                {...register('destination')}
              />
              {errors.destination && (
                <p className="text-xs text-destructive">{errors.destination.message}</p>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label htmlFor="startDate" className="text-xs font-semibold uppercase tracking-wider text-foreground">
                  Ngày bắt đầu <span className="text-destructive">*</span>
                </label>
                <input
                  id="startDate"
                  type="date"
                  className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-colors"
                  {...register('startDate')}
                />
                {errors.startDate && (
                  <p className="text-xs text-destructive">{errors.startDate.message}</p>
                )}
              </div>

              <div className="space-y-1.5">
                <label htmlFor="endDate" className="text-xs font-semibold uppercase tracking-wider text-foreground">
                  Ngày kết thúc <span className="text-destructive">*</span>
                </label>
                <input
                  id="endDate"
                  type="date"
                  className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-colors"
                  {...register('endDate')}
                />
                {errors.endDate && (
                  <p className="text-xs text-destructive">{errors.endDate.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Progressive Disclosure: Optional Details */}
          <div className="pt-4 border-t border-border/60">
            <button
              type="button"
              onClick={() => setShowAdvanced(!showAdvanced)}
              className="flex items-center justify-between w-full py-2 text-xs font-semibold text-muted-foreground hover:text-foreground transition-colors"
            >
              <span>Tùy chọn bổ sung (phương tiện, sở thích, ngân sách)</span>
              {showAdvanced ? (
                <ChevronUp className="h-4 w-4" />
              ) : (
                <ChevronDown className="h-4 w-4" />
              )}
            </button>

            {showAdvanced && (
              <div className="pt-4 space-y-4">
                <div className="space-y-1.5">
                  <label htmlFor="transportation" className="text-xs font-medium text-foreground">
                    Phương tiện di chuyển chính
                  </label>
                  <select
                    id="transportation"
                    className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-colors"
                    {...register('transportation')}
                  >
                    <option value="">Chưa xác định</option>
                    {TRANSPORTATION_MODES.map((mode) => (
                      <option key={mode} value={mode}>
                        {TRANSPORTATION_LABELS[mode]}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-medium text-foreground">Sở thích trải nghiệm</label>
                  <div className="flex flex-wrap gap-2">
                    {TRAVEL_INTERESTS.map((interest) => {
                      const isSelected = selectedPreferences.includes(interest);
                      return (
                        <button
                          key={interest}
                          type="button"
                          onClick={() => togglePreference(interest)}
                          className={`rounded-md px-3 py-1.5 text-xs font-medium border transition-colors ${
                            isSelected
                              ? 'bg-primary/10 border-primary text-primary font-semibold'
                              : 'bg-background border-border text-muted-foreground hover:bg-muted/50'
                          }`}
                        >
                          {TRAVEL_INTEREST_LABELS[interest]}
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label htmlFor="budget" className="text-xs font-medium text-foreground">
                    Ngân sách dự kiến (VNĐ)
                  </label>
                  <input
                    id="budget"
                    type="number"
                    placeholder="Không bắt buộc"
                    className="w-full rounded-lg border border-input bg-background px-3.5 py-2.5 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-colors"
                    {...register('budget')}
                  />
                </div>
              </div>
            )}
          </div>

          {/* Submission Error */}
          {createTripMutation.error && (
            <div className="rounded-lg bg-destructive/10 p-3 border border-destructive/20 text-xs text-destructive">
              {createTripMutation.error.message}
            </div>
          )}

          {/* Action Buttons */}
          <div className="pt-4 flex items-center justify-end gap-3 border-t border-border/60">
            <Link
              href={ROUTES.trips}
              className="rounded-lg px-4 py-2 text-sm font-medium text-muted-foreground hover:bg-muted/60 transition-colors"
            >
              Hủy
            </Link>
            <button
              type="submit"
              disabled={createTripMutation.isPending || !isValid}
              className="rounded-lg bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:pointer-events-none transition-colors flex items-center gap-2 shadow-xs"
            >
              {createTripMutation.isPending && (
                <Loader2 className="h-4 w-4 animate-spin" />
              )}
              {createTripMutation.isPending ? "Đang tạo..." : "Bắt đầu chuyến đi"}
            </button>
          </div>
        </form>

      </main>
    </div>
  );
}
