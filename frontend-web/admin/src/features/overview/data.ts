import { z } from "zod";

export const point = z.object({
  name: z.string(),
  value: z.number().nonnegative(),
});

export type Point = z.infer<typeof point>;

export const overviewSchema = z.object({
  asOf: z.string().datetime(),
  days: z.number(),
  users: z.number(),
  newUsers: z.number(),
  newTrips: z.number(),
  drafts: z.number(),
  pendingPlaces: z.number(),
  places: z.number().default(0),
  trips: z.number().default(0),
  checkIns: z.number().default(0),
  trend: z.array(
    z.object({ name: z.string(), users: z.number(), trips: z.number() }),
  ),
  tripStatuses: z.array(point),
  topPlaces: z.array(point),
  topProvinces: z.array(point),
  tags: z.array(point),
  rag: z.array(point),
});

export const integrationHealthSchema = z.object({
  asOf: z.string().datetime(),
  intervalMillis: z.number().positive(),
  checking: z.boolean(),
  integrations: z.array(
    z.object({
      key: z.string(),
      name: z.string(),
      status: z.enum(["UP", "DOWN", "NOT_CONFIGURED", "MOCK", "NOT_CHECKED"]),
      checkedAt: z.string().datetime().nullable(),
      lastSuccessAt: z.string().datetime().nullable(),
      durationMillis: z.number().nonnegative(),
      detailCode: z.string().nullable(),
      stale: z.boolean(),
    }),
  ),
});

export const integrationCheckSchema = z.object({
  started: z.boolean(),
  checking: z.boolean(),
});

export const knowledgeSchema = z.object({
  content: z.array(
    z.object({
      id: z.string(),
      name: z.string(),
      province: z.string().nullable(),
      tags: z.array(z.string()),
      sourceUrl: z.string(),
      verificationScope: z.string(),
      approved: z.boolean(),
      version: z.number(),
      indexStatus: z.string(),
    }),
  ),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  model: z.string(),
  indexAvailable: z.boolean(),
});

export const catalogPlaceSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  address: z.string().nullable(),
  category: z.string().nullable(),
  latitude: z.number(),
  longitude: z.number(),
  provinceCode: z.string().nullable(),
  province: z.string().nullable(),
  openingTime: z.string().nullable(),
  closingTime: z.string().nullable(),
  weeklyOpeningHours: z.string().nullable(),
  placeType: z.enum(["INDOOR", "OUTDOOR", "HYBRID", "UNSPECIFIED"]),
  samplePhotoUrls: z.array(z.string().url()).max(8),
  description: z.string(),
  tags: z.array(z.string()),
  estimatedDurationMinutes: z.number().int().positive(),
  sourceUrl: z.string(),
  reviewedOn: z.string(),
  verificationScope: z.string(),
  coordinatesQuality: z.string(),
  hoursQuality: z.string(),
  approved: z.boolean(),
  archivedAt: z.string().datetime().nullable(),
  version: z.number().int().positive(),
});

export const catalogPageSchema = z.object({
  content: z.array(catalogPlaceSchema).max(100),
  page: z.number().int().nonnegative(),
  size: z.number().int().min(1).max(100),
  totalElements: z.number().int().nonnegative().safe(),
});

export const provinceOptionsSchema = z.array(
  z.object({ code: z.string().regex(/^\d{2}$/), name: z.string() }),
);

export type ProvinceOption = z.infer<typeof provinceOptionsSchema>[number];

export const placeRequestSchema = z
  .object({
    name: z.string().trim().min(1).max(255),
    address: z.string().trim().min(1).max(2000),
    category: z.string().trim().max(100).nullable(),
    latitude: z.number().finite().min(-90).max(90),
    longitude: z.number().finite().min(-180).max(180),
    provinceCode: z.string().regex(/^\d{2}$/),
    openingTime: z
      .string()
      .regex(/^([01]\d|2[0-3]):[0-5]\d$/)
      .nullable(),
    closingTime: z
      .string()
      .regex(/^([01]\d|2[0-3]):[0-5]\d$/)
      .nullable(),
    weeklyOpeningHours: z.string().max(5000).nullable(),
    placeType: z.enum(["INDOOR", "OUTDOOR", "HYBRID", "UNSPECIFIED"]),
    samplePhotoUrls: z
      .array(
        z
          .string()
          .url()
          .refine((value) => /^https:\/\//i.test(value)),
      )
      .max(8)
      .refine((urls) => new Set(urls).size === urls.length),
    description: z.string().trim().min(1).max(5000),
    tags: z
      .array(z.string().regex(/^[A-Z][A-Z0-9_]{1,29}$/))
      .min(1)
      .max(20)
      .refine((tags) => new Set(tags).size === tags.length),
    estimatedDurationMinutes: z.number().int().positive(),
    sourceUrl: z
      .string()
      .trim()
      .url()
      .refine((value) => /^https?:\/\//i.test(value)),
    reviewedOn: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
    verificationScope: z.string().trim().min(1).max(500),
    coordinatesQuality: z.string().regex(/^[A-Z][A-Z0-9_]{1,29}$/),
    hoursQuality: z.string().regex(/^[A-Z][A-Z0-9_]{1,29}$/),
  })
  .strict();

export type CatalogPlace = z.infer<typeof catalogPlaceSchema>;
export type PlaceRequest = z.infer<typeof placeRequestSchema>;

export const uploadedImageSchema = z.object({
  url: z
    .string()
    .url()
    .refine((value) => /^https:\/\//i.test(value)),
  publicId: z.string().min(1),
  mimeType: z.string(),
  fileSize: z.number().nonnegative(),
  width: z.number().positive(),
  height: z.number().positive(),
});

export const ragSearchSchema = z.object({
  retrievalMode: z.string(),
  embeddingModel: z.string(),
  fallbackReason: z.string().nullable(),
  matches: z.array(
    z.object({
      place: z.object({
        placeId: z.string(),
        name: z.string(),
        description: z.string(),
        tags: z.array(z.string()),
        latitude: z.number(),
        longitude: z.number(),
        placeType: z.string(),
        openingTime: z.string().nullable(),
        closingTime: z.string().nullable(),
        estimatedDurationMinutes: z.number(),
        sourceUrl: z.string(),
        reviewedOn: z.string().nullable(),
        verificationScope: z.string(),
        coordinatesQuality: z.string(),
        hoursQuality: z.string(),
        contentVersion: z.number(),
      }),
      rankScore: z.number(),
      lexicalMatch: z.boolean(),
      semanticMatch: z.boolean(),
    }),
  ),
});

export type Entry = z.infer<typeof knowledgeSchema>["content"][number];

export const samples = {
  topPlaces: [
    { name: "Bảo tàng Chăm", value: 42 },
    { name: "Biển Mỹ Khê", value: 36 },
    { name: "Chợ Hàn", value: 28 },
    { name: "Ngũ Hành Sơn", value: 19 },
  ],
  topProvinces: [
    { name: "Đà Nẵng", value: 58 },
    { name: "Huế", value: 34 },
    { name: "Lâm Đồng", value: 22 },
    { name: "Khánh Hòa", value: 15 },
    { name: "Hà Nội", value: 11 },
  ],
  tags: [
    { name: "HISTORY", value: 18 },
    { name: "MUSEUM", value: 14 },
    { name: "BEACH", value: 10 },
    { name: "LOCAL", value: 8 },
  ],
  tripStatuses: [
    { name: "PLANNING", value: 18 },
    { name: "READY", value: 12 },
    { name: "ACTIVE", value: 5 },
    { name: "COMPLETED", value: 24 },
    { name: "CANCELLED", value: 2 },
  ],
  rag: [
    { name: "CURRENT", value: 24 },
    { name: "STALE", value: 6 },
    { name: "MISSING", value: 4 },
    { name: "OUT_OF_SCOPE", value: 8 },
  ],
  ai: [
    { name: "Nháp hợp lệ", value: 38 },
    { name: "Cần thêm thông tin", value: 8 },
    { name: "Xung đột", value: 5 },
    { name: "Lỗi xử lý", value: 3 },
  ],
};

export const labels: Record<string, string> = {
  READY_WITH_WARNINGS: "Hợp lệ, có cảnh báo",
  NEEDS_STRUCTURED_INPUT: "Cần nhập biểu mẫu",
  NEEDS_MORE_INPUT: "Cần thêm thông tin",
  CONFLICTS_DETECTED: "Xung đột lịch",
  FAILED: "Lỗi xử lý",
  PLANNING: "Đang lên kế hoạch",
  READY: "Sẵn sàng",
  ACTIVE: "Đang diễn ra",
  COMPLETED: "Đã hoàn thành",
  CANCELLED: "Đã hủy",
  CURRENT: "Embedding hiện hành",
  STALE: "Cần cập nhật",
  MISSING: "Chưa index",
  OUT_OF_SCOPE: "Ngoài phạm vi RAG",
};
