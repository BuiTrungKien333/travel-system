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
  trend: z.array(
    z.object({ name: z.string(), users: z.number(), trips: z.number() }),
  ),
  tripStatuses: z.array(point),
  topPlaces: z.array(point),
  topProvinces: z.array(point),
  tags: z.array(point),
  rag: z.array(point),
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
