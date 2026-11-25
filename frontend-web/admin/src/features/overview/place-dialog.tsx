"use client";
import { cloneElement, useEffect, useId, useRef, useState, type ReactElement, type ReactNode } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Trash2, X } from "lucide-react";
import Image from "next/image";
import { Banner, Button } from "@/components/ui";
import type { CatalogPlace, PlaceRequest, ProvinceOption } from "./data";
import { errorMessage } from "@/lib/errors";
import { uploadCatalogImage } from "./hooks";
import { z } from "zod";

const placeFormSchema = z
  .object({
    name: z.string().trim().min(1, "Nhập tên địa điểm").max(255),
    address: z.string().trim().min(1, "Nhập địa chỉ").max(2000),
    category: z.string().max(100),
    latitude: z.coerce.number().finite().min(-90).max(90),
    longitude: z.coerce.number().finite().min(-180).max(180),
    provinceCode: z.string().regex(/^\d{2}$/, "Chọn tỉnh/thành"),
    openingTime: z.string(),
    closingTime: z.string(),
    weeklyOpeningHours: z.string().max(5000),
    placeType: z.enum(["INDOOR", "OUTDOOR", "HYBRID", "UNSPECIFIED"]),
    samplePhotoUrlsText: z.string().max(16384),
    description: z.string().trim().min(1, "Nhập mô tả").max(5000),
    tagsText: z.string().trim().min(1, "Nhập ít nhất một tag"),
    estimatedDurationMinutes: z.coerce.number().int().positive("Thời lượng phải lớn hơn 0"),
    sourceUrl: z.string().trim().url("Nhập URL nguồn hợp lệ").refine(
      (value) => /^https?:\/\//i.test(value),
      "Nguồn tham khảo phải bắt đầu bằng http:// hoặc https://",
    ),
    reviewedOn: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Chọn ngày kiểm tra nguồn"),
    verificationScope: z.string().trim().min(1, "Mô tả phạm vi đã xác minh").max(500),
    coordinatesQuality: z.string().regex(/^[A-Z][A-Z0-9_]{1,29}$/),
    hoursQuality: z.string().regex(/^[A-Z][A-Z0-9_]{1,29}$/),
  })
  .superRefine((values, context) => {
    if (!!values.openingTime !== !!values.closingTime) {
      context.addIssue({
        code: "custom",
        message: "Cần nhập cả giờ mở cửa và giờ đóng cửa.",
        path: [values.openingTime ? "closingTime" : "openingTime"],
      });
    }
    if (values.openingTime && values.closingTime && values.openingTime >= values.closingTime) {
      context.addIssue({
        code: "custom",
        message: "Giờ đóng cửa phải sau giờ mở cửa trong cùng ngày.",
        path: ["closingTime"],
      });
    }
    const photoUrls = values.samplePhotoUrlsText.split(/\r?\n/).map((url) => url.trim()).filter(Boolean);
    if (photoUrls.length > 8 || photoUrls.some((url) => !/^https:\/\//i.test(url) || !z.string().url().safeParse(url).success) || new Set(photoUrls).size !== photoUrls.length) {
      context.addIssue({ code: "custom", message: "Nhập tối đa 8 URL ảnh HTTPS hợp lệ, không trùng.", path: ["samplePhotoUrlsText"] });
    }
    const tags = values.tagsText.split(",").map((tag) => tag.trim().toUpperCase());
    if (
      tags.length > 20 ||
      tags.some((tag) => !/^[A-Z][A-Z0-9_]{1,29}$/.test(tag)) ||
      new Set(tags).size !== tags.length
    ) {
      context.addIssue({
        code: "custom",
        message: "Tag viết dạng CHỮ_IN_HOA, cách nhau bằng dấu phẩy; tối đa 20 tag và không trùng.",
        path: ["tagsText"],
      });
    }
  });

type PlaceFormValues = z.infer<typeof placeFormSchema>;
export type PlaceEditor = { kind: "create" } | { kind: "edit"; place: CatalogPlace };

function currentDate() {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const get = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((part) => part.type === type)?.value ?? "";
  return `${get("year")}-${get("month")}-${get("day")}`;
}

function timeInputValue(time: string | null) {
  return time?.slice(0, 5) ?? "";
}

function defaultValues(editor: PlaceEditor, provinces: ProvinceOption[]): PlaceFormValues {
  const place = editor.kind === "edit" ? editor.place : undefined;
  return {
    name: place?.name ?? "",
    address: place?.address ?? "",
    category: place?.category ?? "",
    latitude: place?.latitude ?? 16.0677,
    longitude: place?.longitude ?? 108.224,
    provinceCode: place?.provinceCode ?? provinces[0]?.code ?? "",
    openingTime: timeInputValue(place?.openingTime ?? null),
    closingTime: timeInputValue(place?.closingTime ?? null),
    weeklyOpeningHours: place?.weeklyOpeningHours ?? "",
    placeType: place?.placeType ?? "UNSPECIFIED",
    samplePhotoUrlsText: place?.samplePhotoUrls.join("\n") ?? "",
    description: place?.description ?? "",
    tagsText: place?.tags.join(", ") ?? "",
    estimatedDurationMinutes: place?.estimatedDurationMinutes ?? 60,
    sourceUrl: place?.sourceUrl ?? "",
    reviewedOn: place?.reviewedOn ?? currentDate(),
    verificationScope:
      place?.verificationScope ??
      "ADMIN_SUBMITTED; verify coordinates and opening hours before publication",
    coordinatesQuality: place?.coordinatesQuality ?? "APPROXIMATE_DEMO",
    hoursQuality: place?.hoursQuality ?? "UNKNOWN",
  };
}

function toPlaceRequest(values: PlaceFormValues, photoUrls = values.samplePhotoUrlsText.split(/\r?\n/).map((url) => url.trim()).filter(Boolean)): PlaceRequest {
  return {
    name: values.name.trim(),
    address: values.address.trim(),
    category: values.category.trim() || null,
    latitude: values.latitude,
    longitude: values.longitude,
    provinceCode: values.provinceCode,
    openingTime: values.openingTime || null,
    closingTime: values.closingTime || null,
    weeklyOpeningHours: values.weeklyOpeningHours.trim() || null,
    placeType: values.placeType,
    samplePhotoUrls: photoUrls,
    description: values.description.trim(),
    tags: values.tagsText.split(",").map((tag) => tag.trim().toUpperCase()),
    estimatedDurationMinutes: values.estimatedDurationMinutes,
    sourceUrl: values.sourceUrl.trim(),
    reviewedOn: values.reviewedOn,
    verificationScope: values.verificationScope.trim(),
    coordinatesQuality: values.coordinatesQuality,
    hoursQuality: values.hoursQuality,
  };
}

export function PlaceDialog({
  editor,
  provinces,
  busy,
  error,
  onClose,
  onSave,
}: {
  editor: PlaceEditor;
  provinces: ProvinceOption[];
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (input: PlaceRequest) => Promise<void>;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<PlaceFormValues>({
    resolver: zodResolver(placeFormSchema),
    defaultValues: defaultValues(editor, provinces),
  });
  const isEditing = editor.kind === "edit";
  const photoUrls = watch("samplePhotoUrlsText").split(/\r?\n/).map((url) => url.trim()).filter(Boolean);
  const [selectedPhotos, setSelectedPhotos] = useState<File[]>([]);
  const [previewPhotos, setPreviewPhotos] = useState<{ file: File; url: string }[]>([]);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const formBusy = busy || uploading;

  useEffect(() => {
    if (!selectedPhotos.length) {
      setPreviewPhotos([]);
      return;
    }
    const previews = selectedPhotos.map((file) => ({ file, url: URL.createObjectURL(file) }));
    setPreviewPhotos(previews);
    return () => previews.forEach(({ url }) => URL.revokeObjectURL(url));
  }, [selectedPhotos]);

  useEffect(() => {
    const node = dialog.current;
    node?.showModal();
    return () => {
      if (node?.open) node.close();
    };
  }, []);

  return (
    <dialog
      ref={dialog}
      aria-labelledby="place-editor-title"
      onCancel={(event) => {
        if (formBusy) event.preventDefault();
        else onClose();
      }}
      className="max-h-[90dvh] w-[calc(100%-1rem)] max-w-4xl overflow-y-auto overscroll-contain border border-line bg-white p-0 text-ink shadow-panel backdrop:bg-ink/40"
    >
      <form
        aria-labelledby="place-editor-title"
        onSubmit={handleSubmit(async (values) => {
          setUploadError(null);
          let nextPhotoUrls = values.samplePhotoUrlsText.split(/\r?\n/).map((url) => url.trim()).filter(Boolean);
          if (selectedPhotos.length) {
            setUploading(true);
            try {
              for (const photo of selectedPhotos) {
                nextPhotoUrls = [...nextPhotoUrls, (await uploadCatalogImage(photo)).url];
                setValue("samplePhotoUrlsText", nextPhotoUrls.join("\n"), { shouldDirty: true });
                setSelectedPhotos((current) => current.filter((item) => item !== photo));
              }
            } catch (uploadFailure) {
              setUploadError(errorMessage(uploadFailure));
              return;
            } finally {
              setUploading(false);
            }
          }
          await onSave(toPlaceRequest(values, nextPhotoUrls));
        })}
        className="flex max-h-[90dvh] flex-col"
      >
        <header className="sticky top-0 z-10 flex items-start justify-between border-b border-line bg-white px-5 py-4 sm:px-7">
          <div>
            <p className="eyebrow">CATALOG · HỒ SƠ ĐỊA ĐIỂM</p>
            <h1 id="place-editor-title" className="mt-1 text-xl font-semibold">
              {isEditing ? "Chỉnh sửa địa điểm" : "Thêm địa điểm"}
            </h1>
            <p className="mt-1 text-sm text-muted">
              {isEditing
                ? "Lưu thay đổi sẽ đưa hồ sơ về trạng thái chờ duyệt."
                : "Địa điểm mới sẽ ở trạng thái chờ duyệt và chưa được công khai."}
            </p>
          </div>
          <button
            type="button"
            aria-label="Đóng biểu mẫu"
            disabled={formBusy}
            onClick={onClose}
            className="grid size-10 shrink-0 place-items-center rounded-lg text-muted hover:bg-canvas disabled:opacity-40"
          >
            <X size={18} aria-hidden="true" />
          </button>
        </header>

        <div className="space-y-6 overflow-y-auto px-5 py-5 sm:px-7">
          {error ? <Banner error>{errorMessage(error)}</Banner> : null}
          <fieldset className="grid gap-4 sm:grid-cols-2">
            <legend className="mb-3 text-sm font-semibold">Thông tin cơ bản</legend>
            <Field label="Tên địa điểm" error={errors.name?.message}>
              <input autoFocus required maxLength={255} className="input" {...register("name")} />
            </Field>
            <Field label="Tỉnh/thành" error={errors.provinceCode?.message}>
              <select required className="input" {...register("provinceCode")}>
                <option value="">Chọn tỉnh/thành</option>
                {provinces.map((province) => (
                  <option key={province.code} value={province.code}>
                    {province.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Địa chỉ" error={errors.address?.message} className="sm:col-span-2">
              <input required maxLength={2000} className="input" {...register("address")} />
            </Field>
            <Field label="Danh mục" hint="Không bắt buộc">
              <input maxLength={100} className="input" placeholder="Ví dụ: Bảo tàng" {...register("category")} />
            </Field>
            <Field label="Loại địa điểm">
              <select className="input" {...register("placeType")}>
                <option value="UNSPECIFIED">Chưa xác định</option>
                <option value="INDOOR">Trong nhà</option>
                <option value="OUTDOOR">Ngoài trời</option>
                <option value="HYBRID">Kết hợp</option>
              </select>
            </Field>
          </fieldset>

          <fieldset className="grid gap-4 sm:grid-cols-2">
            <legend className="mb-3 text-sm font-semibold">Vị trí và giờ hoạt động</legend>
            <Field label="Vĩ độ" error={errors.latitude?.message}>
              <input required type="number" step="any" min="-90" max="90" className="input" {...register("latitude", { valueAsNumber: true })} />
            </Field>
            <Field label="Kinh độ" error={errors.longitude?.message}>
              <input required type="number" step="any" min="-180" max="180" className="input" {...register("longitude", { valueAsNumber: true })} />
            </Field>
            <Field label="Giờ mở cửa" error={errors.openingTime?.message}>
              <input type="time" className="input" {...register("openingTime")} />
            </Field>
            <Field label="Giờ đóng cửa" error={errors.closingTime?.message}>
              <input type="time" className="input" {...register("closingTime")} />
            </Field>
            <Field label="Giờ theo tuần" hint="Không bắt buộc · nhập JSON nếu có" className="sm:col-span-2">
              <textarea rows={3} maxLength={5000} className="input min-h-24 resize-y" placeholder='Ví dụ: {"monday":"08:00-17:00"}' {...register("weeklyOpeningHours")} />
            </Field>
          </fieldset>

          <fieldset className="space-y-3">
            <legend className="text-sm font-semibold">Ảnh mẫu địa điểm</legend>
            <input type="hidden" {...register("samplePhotoUrlsText")} />
            <div className="flex flex-wrap items-center gap-3">
              <input
                id="place-photo-file"
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                multiple
                disabled={formBusy}
                aria-label="Thêm ảnh địa điểm từ máy"
                className="block w-full max-w-md cursor-pointer text-sm text-muted file:mr-3 file:min-h-10 file:cursor-pointer file:rounded-lg file:border file:border-line file:bg-white file:px-4 file:py-2 file:text-sm file:font-semibold file:text-ink hover:file:bg-canvas disabled:cursor-not-allowed disabled:opacity-50"
                aria-describedby="place-photo-hint"
                onChange={(event) => {
                  const files = Array.from(event.target.files ?? []);
                  event.target.value = "";
                  if (!files.length) return;
                  const remaining = 8 - photoUrls.length - selectedPhotos.length;
                  if (files.length > remaining) {
                    setUploadError(`Chỉ có thể thêm tối đa ${Math.max(remaining, 0)} ảnh nữa (tối đa 8 ảnh mỗi địa điểm).`);
                    return;
                  }
                  if (files.some((file) => !/^image\/(jpeg|png|gif|webp)$/.test(file.type))) {
                    setUploadError("Chỉ nhận ảnh JPEG, PNG, GIF hoặc WebP.");
                    return;
                  }
                  if (files.some((file) => file.size > 10 * 1024 * 1024)) {
                    setUploadError("Mỗi ảnh tối đa 10 MB.");
                    return;
                  }
                  setUploadError(null);
                  setSelectedPhotos((current) => [...current, ...files]);
                }}
              />
            </div>
            <p id="place-photo-hint" className="text-xs text-muted">Có thể thêm ảnh từ máy · JPEG, PNG, GIF hoặc WebP · tối đa 10 MB mỗi ảnh · tối đa 8 ảnh.</p>
            {errors.samplePhotoUrlsText?.message ? <p className="text-xs text-red-700">{errors.samplePhotoUrlsText.message}</p> : null}
            {uploadError ? <Banner error>{uploadError}</Banner> : null}
            {photoUrls.length || previewPhotos.length ? (
              <div className="flex flex-wrap gap-3" aria-label="Ảnh mẫu địa điểm">
                {photoUrls.map((url, index) => (
                  <div key={url} className="relative w-fit overflow-hidden border border-line bg-canvas">
                    <Image src={url} alt={`Ảnh mẫu ${index + 1}`} width={160} height={112} unoptimized className="h-28 w-40 object-cover" />
                    <button type="button" disabled={formBusy} aria-label={`Xóa ảnh ${index + 1}`} onClick={() => setValue("samplePhotoUrlsText", photoUrls.filter((_, itemIndex) => itemIndex !== index).join("\n"), { shouldDirty: true, shouldValidate: true })} className="absolute right-1 top-1 grid size-8 place-items-center bg-white text-red-700 disabled:opacity-50">
                      <Trash2 size={16} aria-hidden="true" />
                    </button>
                    <p className="px-2 py-1 text-xs text-muted">Ảnh {index + 1}</p>
                  </div>
                ))}
                {previewPhotos.map(({ file, url }, index) => (
                  <div key={`${file.name}-${file.size}-${file.lastModified}`} className="relative w-fit overflow-hidden border border-dashed border-teal-600 bg-canvas">
                    <Image src={url} alt={`Ảnh mới ${index + 1}`} width={160} height={112} unoptimized className="h-28 w-40 object-cover" />
                    <button type="button" disabled={formBusy} aria-label={`Bỏ ảnh mới ${index + 1}`} onClick={() => setSelectedPhotos((current) => current.filter((item) => item !== file))} className="absolute right-1 top-1 grid size-8 place-items-center bg-white text-red-700 disabled:opacity-50">
                      <Trash2 size={16} aria-hidden="true" />
                    </button>
                    <p className="px-2 py-1 text-xs text-muted">Sẽ tải lên khi lưu</p>
                  </div>
                ))}
              </div>
            ) : <p className="text-xs text-muted">Chưa có ảnh mẫu.</p>}
            {uploading ? <p role="status" className="text-sm text-teal-700">Đang tải ảnh lên Cloudinary…</p> : null}
          </fieldset>

          <fieldset className="grid gap-4 sm:grid-cols-2">
            <legend className="mb-3 text-sm font-semibold">Thông tin dùng cho tìm kiếm và duyệt</legend>
            <Field label="Mô tả" error={errors.description?.message} className="sm:col-span-2">
              <textarea required maxLength={5000} rows={4} className="input min-h-28 resize-y" {...register("description")} />
            </Field>
            <Field label="Tags" hint="Viết CHỮ_IN_HOA, cách nhau bằng dấu phẩy" error={errors.tagsText?.message} className="sm:col-span-2">
              <input required maxLength={620} className="input" placeholder="BEACH, SCENERY, RELAX" {...register("tagsText")} />
            </Field>
            <Field label="Thời lượng tham quan (phút)" error={errors.estimatedDurationMinutes?.message}>
              <input required type="number" min="1" step="1" className="input" {...register("estimatedDurationMinutes", { valueAsNumber: true })} />
            </Field>
            <Field label="Nguồn tham khảo" error={errors.sourceUrl?.message}>
              <input required type="url" maxLength={1000} className="input" placeholder="https://…" {...register("sourceUrl")} />
            </Field>
            <Field label="Ngày kiểm tra nguồn" error={errors.reviewedOn?.message}>
              <input required type="date" className="input" {...register("reviewedOn")} />
            </Field>
            <Field label="Phạm vi đã xác minh" error={errors.verificationScope?.message} className="sm:col-span-2">
              <textarea required maxLength={500} rows={2} className="input min-h-20 resize-y" {...register("verificationScope")} />
            </Field>
            <Field label="Chất lượng tọa độ">
              <select className="input" {...register("coordinatesQuality")}>
                <option value="VERIFIED">Đã xác minh</option>
                <option value="APPROXIMATE">Ước lượng</option>
                <option value="APPROXIMATE_DEMO">Tọa độ demo</option>
                <option value="UNKNOWN">Chưa rõ</option>
              </select>
            </Field>
            <Field label="Chất lượng giờ hoạt động">
              <select className="input" {...register("hoursQuality")}>
                <option value="VERIFIED">Đã xác minh</option>
                <option value="ESTIMATED">Ước lượng</option>
                <option value="UNKNOWN">Chưa rõ</option>
              </select>
            </Field>
          </fieldset>
        </div>

        <footer className="sticky bottom-0 flex flex-wrap items-center justify-between gap-3 border-t border-line bg-white px-5 py-4 sm:px-7">
          <p className="text-xs text-muted">Điền các trường bắt buộc; ảnh mẫu có thể bỏ trống.</p>
          <div className="flex gap-2">
            <Button type="button" variant="secondary" disabled={formBusy} onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" busy={formBusy} disabled={!provinces.length}>
              {isEditing ? "Lưu và gửi duyệt lại" : "Tạo hồ sơ chờ duyệt"}
            </Button>
          </div>
        </footer>
      </form>
    </dialog>
  );
}

function Field({
  label,
  hint,
  error,
  className = "",
  children,
}: {
  label: string;
  hint?: string;
  error?: string;
  className?: string;
  children: ReactNode;
}) {
  const id = useId();
  return (
    <div className={`space-y-1.5 ${className}`}>
      <label htmlFor={id} className="label">{label}</label>
      {cloneElement(children as ReactElement<{ id?: string; "aria-describedby"?: string; "aria-invalid"?: boolean }>, {
        id,
        "aria-invalid": !!error || undefined,
        "aria-describedby": error ? `${id}-error` : undefined,
      })}
      {hint ? <p className="text-xs text-muted">{hint}</p> : null}
      {error ? <p id={`${id}-error`} className="text-xs text-red-700">{error}</p> : null}
    </div>
  );
}
