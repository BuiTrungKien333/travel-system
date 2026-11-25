"use client";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useUpdateTripMutation } from "../api/useUpdateTripMutation";
import type { TripOverview } from "../types";

const schema = z.object({
  name: z.string().trim().min(1).max(120),
  description: z.string().max(1000),
});
export function TripSettingsForm({ trip }: { trip: TripOverview }) {
  const mutation = useUpdateTripMutation(trip.id);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: { name: trip.name, description: trip.description ?? "" },
  });
  return (
    <form
      className="space-y-4 rounded-xl border p-5"
      onSubmit={handleSubmit((input) =>
        mutation.mutate({ ...input, version: trip.version }),
      )}
    >
      <label className="block text-sm">
        Tên chuyến đi
        <input
          {...register("name")}
          className="mt-1 block w-full rounded border p-2"
        />
      </label>
      {errors.name && (
        <p role="alert" className="text-destructive">
          Tên chuyến đi cần từ 1 đến 120 ký tự.
        </p>
      )}
      <label className="block text-sm">
        Ghi chú
        <textarea
          {...register("description")}
          className="mt-1 block w-full rounded border p-2"
        />
      </label>
      {errors.description && (
        <p role="alert" className="text-destructive">
          Ghi chú tối đa 1.000 ký tự.
        </p>
      )}
      {mutation.error && (
        <p role="alert" className="text-destructive">
          {mutation.error.message}
        </p>
      )}
      {mutation.isSuccess && <p role="status">Đã lưu chuyến đi.</p>}
      <button
        disabled={mutation.isPending}
        className="rounded bg-primary px-4 py-2 text-primary-foreground"
      >
        {mutation.isPending ? "Đang lưu..." : "Lưu thay đổi"}
      </button>
      {trip.mode === "LIVE" && trip.status !== "COMPLETED" && (
        <button
          type="button"
          disabled={mutation.isPending}
          className="ml-3 underline"
          onClick={() => {
            if (window.confirm("Kết thúc chuyến đi này?"))
              mutation.mutate({ status: "COMPLETED", version: trip.version });
          }}
        >
          Kết thúc chuyến đi
        </button>
      )}
    </form>
  );
}
