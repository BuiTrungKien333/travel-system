'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { createTripSchema, type CreateTripInput } from '@/features/trip/schema';
import { useCreateTripMutation } from '@/features/trip/api/useCreateTripMutation';
import {
  ROUTES,
  TRANSPORTATION_MODES,
  TRANSPORTATION_LABELS,
  TRAVEL_INTERESTS,
  TRAVEL_INTEREST_LABELS,
} from '@/lib/constants';

export default function NewTripPage() {
  const router = useRouter();
  const createTripMutation = useCreateTripMutation();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CreateTripInput>({
    resolver: zodResolver(createTripSchema),
    defaultValues: {
      preferences: [],
    },
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
        name: data.name,
        destination: data.destination,
        startDate: data.startDate,
        endDate: data.endDate,
        transportation: data.transportation,
        preferences: data.preferences,
        budget: data.budget,
        constraints: data.constraints,
        description: data.description,
      },
      {
        onSuccess: (trip) => {
          router.push(ROUTES.trip(trip.id));
        },
      }
    );
  };

  return (
    <div className="min-h-screen bg-muted/20">
      <header className="border-b bg-card px-4 h-14 flex items-center gap-3">
        <Link
          href={ROUTES.trips}
          className="p-1.5 rounded hover:bg-muted transition-colors"
          aria-label="Quay lại"
        >
          <ArrowLeft className="h-4 w-4 text-muted-foreground" />
        </Link>
        <h1 className="font-semibold text-sm">Tạo chuyến đi mới</h1>
      </header>

      <main className="mx-auto max-w-xl px-4 py-8">
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="rounded-xl border bg-card p-6 shadow-sm space-y-6"
        >
          {/* Step 1: Core details (Fast first-use / UX-01) */}
          <div className="space-y-4">
            <h2 className="font-semibold text-base">Thông tin cơ bản</h2>

            <div className="space-y-1">
              <label htmlFor="name" className="text-sm font-medium">
                Tên chuyến đi <span className="text-destructive">*</span>
              </label>
              <input
                id="name"
                type="text"
                placeholder="Ví dụ: Khám phá Đà Nẵng 3 ngày"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                {...register('name')}
              />
              {errors.name && (
                <p className="text-xs text-destructive">{errors.name.message}</p>
              )}
            </div>

            <div className="space-y-1">
              <label htmlFor="destination" className="text-sm font-medium">
                Điểm đến <span className="text-destructive">*</span>
              </label>
              <input
                id="destination"
                type="text"
                placeholder="Ví dụ: Đà Nẵng, Hội An"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                {...register('destination')}
              />
              {errors.destination && (
                <p className="text-xs text-destructive">{errors.destination.message}</p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label htmlFor="startDate" className="text-sm font-medium">
                  Ngày bắt đầu <span className="text-destructive">*</span>
                </label>
                <input
                  id="startDate"
                  type="date"
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  {...register('startDate')}
                />
                {errors.startDate && (
                  <p className="text-xs text-destructive">{errors.startDate.message}</p>
                )}
              </div>

              <div className="space-y-1">
                <label htmlFor="endDate" className="text-sm font-medium">
                  Ngày kết thúc <span className="text-destructive">*</span>
                </label>
                <input
                  id="endDate"
                  type="date"
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  {...register('endDate')}
                />
                {errors.endDate && (
                  <p className="text-xs text-destructive">{errors.endDate.message}</p>
                )}
              </div>
            </div>
          </div>

          <div className="border-t pt-4 space-y-4">
            <h2 className="font-semibold text-base">Tùy chọn (để AI hỗ trợ tốt hơn)</h2>

            <div className="space-y-1">
              <label htmlFor="transportation" className="text-sm font-medium">
                Phương tiện di chuyển
              </label>
              <select
                id="transportation"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                {...register('transportation')}
              >
                <option value="">Chọn phương tiện</option>
                {TRANSPORTATION_MODES.map((mode) => (
                  <option key={mode} value={mode}>
                    {TRANSPORTATION_LABELS[mode]}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1">
              <label className="text-sm font-medium">Sở thích du lịch</label>
              <div className="flex flex-wrap gap-2 pt-1">
                {TRAVEL_INTERESTS.map((interest) => {
                  const isSelected = selectedPreferences.includes(interest);
                  return (
                    <button
                      key={interest}
                      type="button"
                      onClick={() => togglePreference(interest)}
                      className={`rounded-full px-3 py-1 text-xs font-medium border transition-colors ${
                        isSelected
                          ? 'bg-primary text-primary-foreground border-primary'
                          : 'bg-muted/50 text-muted-foreground hover:bg-muted'
                      }`}
                    >
                      {TRAVEL_INTEREST_LABELS[interest]}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="space-y-1">
              <label htmlFor="budget" className="text-sm font-medium">
                Ngân sách dự kiến (VNĐ)
              </label>
              <input
                id="budget"
                type="number"
                placeholder="Ví dụ: 5000000"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                {...register('budget')}
              />
            </div>
          </div>

          {createTripMutation.error && (
            <p className="rounded-md bg-destructive/10 px-3 py-2 text-xs leading-relaxed text-destructive">
              {createTripMutation.error.message}
            </p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <Link
              href={ROUTES.trips}
              className="rounded-md border px-4 py-2 text-sm font-medium text-muted-foreground hover:bg-muted transition"
            >
              Hủy
            </Link>
            <button
              type="submit"
              disabled={createTripMutation.isPending}
              className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-60 flex items-center gap-2 transition"
            >
              {createTripMutation.isPending && (
                <Loader2 className="h-4 w-4 animate-spin" />
              )}
              Tạo chuyến đi
            </button>
          </div>
        </form>
      </main>
    </div>
  );
}
