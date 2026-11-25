"use client";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useDropzone } from "react-dropzone";
import { useCreateMemoryMutation } from "../api/useCreateMemoryMutation";
import { useTripPlacesQuery } from "@/features/place/api/useTripPlacesQuery";
import { useMemoryUIStore } from "../store/useMemoryUIStore";
import { ApiError } from "@/lib/api";
const schema = z.object({ note: z.string().max(2000), placeId: z.string() });
export function MemoryUploadForm({ tripId }: { tripId: string }) {
  const mutation = useCreateMemoryMutation(tripId);
  const close = useMemoryUIStore((s) => s.setIsUploadOpen);
  const [clientRequestId, setClientRequestId] = useState(() =>
    crypto.randomUUID(),
  );
  const [files, setFiles] = useState<File[]>([]);
  const { data: places } = useTripPlacesQuery(tripId);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<z.infer<typeof schema>>({
    resolver: zodResolver(schema),
    defaultValues: { note: "", placeId: "" },
  });
  const definiteRejection =
    mutation.error instanceof ApiError &&
    [400, 403, 404, 422].includes(mutation.error.status);
  const uncertainFailure = mutation.isError && !definiteRejection;
  const dropzone = useDropzone({
    accept: {
      "image/jpeg": [],
      "image/png": [],
      "image/webp": [],
      "image/gif": [],
    },
    maxFiles: 10,
    maxSize: 10 * 1024 * 1024,
    disabled: mutation.isPending || uncertainFailure,
    onDropAccepted: setFiles,
  });
  return (
    <form
      className="space-y-4 rounded-xl border p-5"
      onSubmit={handleSubmit((input) => {
        if (!input.note.trim() && !files.length) {
          setError("note", {
            message: "Hãy nhập ghi chú hoặc chọn ít nhất một ảnh.",
          });
          return;
        }
        const requestId = definiteRejection
          ? crypto.randomUUID()
          : clientRequestId;
        if (definiteRejection) setClientRequestId(requestId);
        mutation.mutate(
          { ...input, files, clientRequestId: requestId },
          { onSuccess: () => close(false) },
        );
      })}
    >
      <h3 className="font-semibold">Thêm kỷ niệm</h3>
      <fieldset
        disabled={mutation.isPending || uncertainFailure}
        className="space-y-3"
      >
        <label className="block text-sm">
          Địa điểm (tùy chọn)
          <select
            {...register("placeId")}
            className="block w-full rounded border p-2"
          >
            <option value="">Không gắn địa điểm</option>
            {places?.map((p) => (
              <option key={p.placeId} value={p.placeId}>
                {p.place.name}
              </option>
            ))}
          </select>
        </label>
        <label className="block text-sm">
          Ghi chú
          <textarea
            {...register("note")}
            className="block w-full rounded border p-2"
          />
        </label>
        {errors.note && <p role="alert">{errors.note.message}</p>}
        <div
          {...dropzone.getRootProps()}
          className="cursor-pointer rounded border border-dashed p-5"
        >
          <input {...dropzone.getInputProps()} />
          Chọn hoặc thả tối đa 10 ảnh (10 MB/ảnh).
        </div>
        {dropzone.fileRejections.length > 0 && (
          <p role="alert" className="text-destructive">
            Ảnh không đúng định dạng, quá 10 MB hoặc vượt quá 10 ảnh.
          </p>
        )}
        <ul>
          {files.map((file, i) => (
            <li key={i} className="text-sm">
              {file.name}
            </li>
          ))}
        </ul>
      </fieldset>
      {mutation.error && (
        <p role="alert" className="text-destructive">
          {mutation.error.message}{" "}
          {uncertainFailure
            ? "Kỷ niệm có thể đã được lưu; giữ nguyên dữ liệu khi thử lại."
            : "Bạn có thể sửa thông tin rồi lưu lại."}
        </p>
      )}
      <div className="flex gap-3">
        <button
          disabled={mutation.isPending}
          className="rounded bg-primary px-4 py-2 text-primary-foreground"
        >
          {mutation.isPending
            ? "Đang lưu..."
            : mutation.isError
              ? "Thử lại"
              : "Lưu kỷ niệm"}
        </button>
        <button
          type="button"
          disabled={mutation.isPending}
          onClick={() => close(false)}
        >
          Đóng
        </button>
      </div>
    </form>
  );
}
