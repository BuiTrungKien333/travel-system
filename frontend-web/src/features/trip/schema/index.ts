import { z } from 'zod';

const baseTripSchema = z.object({
  name: z
    .string()
    .min(1, 'Tên chuyến đi không được để trống')
    .max(100, 'Tên chuyến đi tối đa 100 ký tự'),
  destination: z.string().min(1, 'Điểm đến không được để trống'),
  startDate: z.string().min(1, 'Ngày khởi hành không được để trống'),
  endDate: z.string().min(1, 'Ngày kết thúc không được để trống'),
  transportation: z.string().optional(),
  preferences: z.array(z.string()).optional(),
  budget: z.coerce.number().min(0).optional(),
  constraints: z.string().optional(),
  description: z.string().optional(),
});

export const createTripSchema = baseTripSchema.refine(
  (data) => {
    if (!data.startDate || !data.endDate) return true;
    return new Date(data.startDate) <= new Date(data.endDate);
  },
  {
    message: 'Ngày kết thúc phải sau ngày khởi hành',
    path: ['endDate'],
  }
);

export type CreateTripInput = z.infer<typeof createTripSchema>;

export const updateTripSchema = baseTripSchema.partial();
export type UpdateTripInput = z.infer<typeof updateTripSchema>;
