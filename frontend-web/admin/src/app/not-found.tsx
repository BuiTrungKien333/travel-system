import Link from "next/link";

export default function NotFound() {
  return (
    <main className="mx-auto max-w-lg space-y-4 px-6 py-24">
      <h1 className="text-2xl font-semibold">Không tìm thấy trang</h1>
      <Link href="/users" className="button button-primary">
        Về quản lý tài khoản
      </Link>
    </main>
  );
}
