import { z } from "zod";

export const idSchema = z
  .string()
  .regex(/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i);

export const roleSchema = z.enum(["GUEST", "USER", "ADMIN"]);

export const loginSchema = z
  .object({
    email: z.string().trim().email("Email không hợp lệ").max(255),
    password: z
      .string()
      .min(1, "Nhập mật khẩu")
      .max(50, "Mật khẩu tối đa 50 ký tự")
      .refine(
        (v) => new TextEncoder().encode(v).length <= 72,
        "Mật khẩu tối đa 72 byte UTF-8",
      ),
  })
  .strict();

export const sessionSchema = z.object({
  id: idSchema,
  fullName: z.string(),
  email: z.string().email(),
  role: z.literal("ADMIN"),
  permissions: z.array(z.string()).max(100),
});

export type AdminSession = z.infer<typeof sessionSchema>;

export const authSchema = z.object({
  accessToken: z.string().min(1).max(8192),
  refreshToken: z.string().min(1).max(8192),
  expiresIn: z.number().int().positive(),
  user: z.object({ role: roleSchema }),
});

export const accountSchema = z.object({
  id: idSchema,
  fullName: z.string(),
  email: z.string().email(),
  role: roleSchema,
  locked: z.boolean(),
  verified: z.boolean(),
  version: z.number().int().nonnegative().safe(),
  createdAt: z.string().datetime(),
});

export type Account = z.infer<typeof accountSchema>;

export const accountPageSchema = z.object({
  content: z.array(accountSchema).max(100),
  page: z.number().int().nonnegative(),
  size: z.number().int().min(1).max(100),
  totalElements: z.number().int().nonnegative().safe(),
});

export const changeAccountSchema = z
  .object({
    role: roleSchema,
    locked: z.boolean(),
    version: z.number().int().nonnegative().safe(),
  })
  .strict();

export type AccountChange = z.infer<typeof changeAccountSchema>;

export function envelope<T extends z.ZodTypeAny>(schema: T) {
  return z.object({ success: z.literal(true), data: schema });
}
