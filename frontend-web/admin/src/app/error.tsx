"use client";
import { Button } from "@/components/ui";

export default function ErrorPage({ reset }: { reset: () => void }) {
  return (
    <main className="mx-auto max-w-lg space-y-4 px-6 py-24">
      <h1 className="text-2xl font-semibold">Chưa thể mở trang quản trị</h1>
      <p className="text-sm text-muted">
        Vui lòng thử lại hoặc kiểm tra kết nối hệ thống.
      </p>
      <Button onClick={reset}>Thử lại</Button>
    </main>
  );
}
