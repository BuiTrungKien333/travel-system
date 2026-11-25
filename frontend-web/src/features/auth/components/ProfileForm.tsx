"use client";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useProfileQuery,
  useUpdateProfileMutation,
} from "../api/useProfileQuery";
const schema = z.object({
  fullName: z.string().trim().min(1, "Vui lòng nhập tên").max(100),
  avatarUrl: z.union([z.string().url(), z.literal("")]).optional(),
});
export function ProfileForm() {
  const { data, isLoading, error } = useProfileQuery();
  const mutation = useUpdateProfileMutation();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    values: {
      fullName: data?.fullName ?? "",
      avatarUrl: data?.avatarUrl ?? "",
    },
  });
  return (
    <form
      onSubmit={handleSubmit((input) => mutation.mutate(input))}
      className="space-y-3 rounded-xl border p-4"
    >
      <label className="block text-sm">
        Tên hiển thị
        <input
          {...register("fullName")}
          className="mt-1 block w-full rounded border p-2"
        />
      </label>
      {errors.fullName && <p role="alert">{errors.fullName.message}</p>}
      <label className="block text-sm">
        URL ảnh đại diện
        <input
          {...register("avatarUrl")}
          className="mt-1 block w-full rounded border p-2"
        />
      </label>
      {errors.avatarUrl && <p role="alert">{errors.avatarUrl.message}</p>}
      {(error || mutation.error) && (
        <p role="alert" className="text-destructive">
          {(error || mutation.error)?.message}
        </p>
      )}
      {mutation.isSuccess && <p role="status">Đã cập nhật hồ sơ.</p>}
      <button
        disabled={isLoading || mutation.isPending || !data}
        className="rounded bg-primary px-4 py-2 text-primary-foreground"
      >
        {mutation.isPending ? "Đang lưu..." : "Lưu hồ sơ"}
      </button>
    </form>
  );
}
