import { z } from 'zod';

export const createMemorySchema = z.object({
  placeId: z.string().optional(),
  note: z.string().max(1000, 'Ghi chú tối đa 1000 ký tự').optional(),
});

export type CreateMemoryInput = z.infer<typeof createMemorySchema>;

export const updateMemorySchema = z.object({
  note: z.string().max(1000, 'Ghi chú tối đa 1000 ký tự').optional(),
});

export type UpdateMemoryInput = z.infer<typeof updateMemorySchema>;
