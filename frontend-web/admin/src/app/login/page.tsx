import { LoginForm } from "@/features/auth/login-form";
export const metadata = { title: "Đăng nhập" };
export default async function Login({
  searchParams,
}: {
  searchParams: Promise<{ notice?: string }>;
}) {
  return <LoginForm notice={(await searchParams).notice} />;
}
