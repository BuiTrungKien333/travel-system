"use client";
import { useState } from "react";
import { useForm } from "react-hook-form";
import {
  Search,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Settings2,
  Users,
  ShieldCheck,
  LockKeyhole,
  Check,
  X,
} from "lucide-react";
import type { Account } from "@/lib/contracts";
import { errorMessage } from "@/lib/errors";
import { Banner, Button, RoleBadge } from "@/components/ui";
import { useSession } from "@/features/auth/api";
import { useAccounts } from "./api";
import { AccountDialog } from "./account-dialog";

const date = new Intl.DateTimeFormat("vi-VN", {
  dateStyle: "medium",
  timeZone: "Asia/Ho_Chi_Minh",
});

export function UsersScreen() {
  const session = useSession();
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Account>();
  const [saved, setSaved] = useState(false);
  const canManage = session.data?.permissions.includes("admin:users") ?? false;
  const query = useAccounts(session.data?.id, q, page, canManage);
  const { register, handleSubmit, setValue } = useForm<{ search: string }>({
    defaultValues: { search: "" },
  });

  const totalPages = Math.max(
    1,
    Math.ceil((query.data?.totalElements ?? 0) / 20),
  );

  if (!canManage)
    return (
      <Banner error>
        Tài khoản quản trị hiện tại chưa được cấp quyền quản lý tài khoản.
      </Banner>
    );

  function closeDialog() {
    setSelected(undefined);
    void query.refetch();
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-5">
        <div>
          <p className="eyebrow">QUYỀN TRUY CẬP & BẢO MẬT</p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight text-ink">
            Quản lý tài khoản
          </h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-muted">
            Tra cứu tài khoản, cập nhật vai trò và kiểm soát quyền truy cập hệ
            thống.
          </p>
        </div>
        <Button
          variant="secondary"
          busy={query.isFetching}
          onClick={() => void query.refetch()}
        >
          <RefreshCw size={15} />
          Tải lại
        </Button>
      </div>
      <div className="flex flex-wrap items-center gap-x-6 gap-y-3 rounded-xl border border-teal-100 bg-teal-50/70 px-5 py-4 text-xs leading-5 text-teal-700">
        <span className="flex items-center gap-2">
          <ShieldCheck size={15} />
          Thay đổi quyền được ghi lịch sử
        </span>
        <span className="flex items-center gap-2">
          <LockKeyhole size={15} />
          Không tự khóa hoặc đổi quyền của mình
        </span>
      </div>
      {saved ? (
        <Banner>
          Đã cập nhật tài khoản và thu hồi các phiên đăng nhập cũ.
        </Banner>
      ) : null}
      <section
        aria-labelledby="accounts-title"
        className="overflow-hidden rounded-2xl border border-line bg-white shadow-panel"
      >
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-line px-5 py-5 sm:px-6">
          <div className="flex items-center gap-3">
            <span className="grid size-10 place-items-center rounded-xl bg-canvas text-muted">
              <Users size={18} />
            </span>
            <div>
              <h2 id="accounts-title" className="font-semibold text-ink">
                Danh sách tài khoản
              </h2>
              <p className="mt-1 text-xs text-muted" aria-live="polite">
                {query.data
                  ? `${query.data.totalElements.toLocaleString("vi-VN")} tài khoản${q ? " khớp tìm kiếm" : " trong hệ thống"}`
                  : "Đang lấy dữ liệu từ hệ thống"}
              </p>
            </div>
          </div>
          <form
            role="search"
            className="flex w-full gap-2 sm:w-auto"
            onSubmit={handleSubmit(({ search }) => {
              setQ(search.trim());
              setPage(0);
              setSaved(false);
            })}
          >
            <div className="relative flex-1 sm:w-72">
              <label className="sr-only" htmlFor="account-search">
                Tìm tên hoặc email
              </label>
              <Search
                className="pointer-events-none absolute left-3.5 top-3.5 text-muted"
                size={16}
              />
              <input
                id="account-search"
                type="search"
                placeholder="Tìm tên hoặc email…"
                maxLength={120}
                className="input pl-10"
                {...register("search")}
              />
            </div>
            <Button type="submit" variant="secondary">
              Tìm
            </Button>
          </form>
        </div>
        {q ? (
          <div className="flex items-center gap-3 border-b border-line bg-canvas/50 px-6 py-3 text-sm text-muted">
            <span>
              Kết quả cho{" "}
              <strong className="font-medium text-ink">“{q}”</strong>
            </span>
            <button
              type="button"
              onClick={() => {
                setValue("search", "");
                setQ("");
                setPage(0);
              }}
              className="grid size-8 place-items-center rounded-lg hover:bg-white"
              aria-label="Xóa bộ lọc tìm kiếm"
            >
              <X size={14} />
            </button>
          </div>
        ) : null}
        {query.isError ? (
          <div className="space-y-4 px-6 py-10">
            <Banner error>{errorMessage(query.error)}</Banner>
            <Button variant="secondary" onClick={() => void query.refetch()}>
              Thử lại
            </Button>
          </div>
        ) : query.isPending ? (
          <div
            role="status"
            aria-label="Đang tải danh sách tài khoản"
            className="space-y-5 px-6 py-8"
          >
            {Array.from({ length: 6 }, (_, i) => (
              <div key={i} className="flex animate-pulse items-center gap-4">
                <span className="size-10 rounded-full bg-canvas" />
                <span className="h-9 w-full rounded-lg bg-canvas" />
              </div>
            ))}
          </div>
        ) : query.data.content.length === 0 ? (
          <div className="grid justify-items-center px-6 py-16 text-center">
            <Users className="mb-4 text-muted" size={30} />
            <h3 className="font-semibold text-ink">
              Không có tài khoản phù hợp
            </h3>
            <p className="mt-2 max-w-sm text-sm leading-6 text-muted">
              {q
                ? "Thử tên hoặc email khác, hoặc xóa bộ lọc để xem toàn bộ tài khoản."
                : "Danh sách hiện chưa có tài khoản để hiển thị."}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[780px] text-left text-sm">
              <caption className="sr-only">
                Danh sách tài khoản và quyền truy cập hiện tại
              </caption>
              <thead>
                <tr className="border-b border-line bg-canvas/50 text-xs uppercase tracking-[.08em] text-muted">
                  <th scope="col" className="px-6 py-4 font-semibold">
                    Tài khoản
                  </th>
                  <th scope="col" className="px-4 py-4 font-semibold">
                    Vai trò
                  </th>
                  <th scope="col" className="px-4 py-4 font-semibold">
                    Trạng thái
                  </th>
                  <th scope="col" className="px-4 py-4 font-semibold">
                    Email
                  </th>
                  <th scope="col" className="px-4 py-4 font-semibold">
                    Ngày tạo
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-4 text-right font-semibold"
                  >
                    Thao tác
                  </th>
                </tr>
              </thead>
              <tbody>
                {query.data.content.map((account) => (
                  <tr
                    key={account.id}
                    className="border-b border-line/70 last:border-0 hover:bg-canvas/40"
                  >
                    <td className="px-6 py-5">
                      <div className="flex items-center gap-3">
                        <span
                          className="grid size-10 shrink-0 place-items-center rounded-full border border-line bg-canvas font-semibold text-muted"
                          aria-hidden="true"
                        >
                          {account.fullName.trim().slice(0, 1).toUpperCase()}
                        </span>
                        <div>
                          <div className="flex items-center gap-2 font-semibold text-ink">
                            {account.fullName}
                            {account.id === session.data?.id ? (
                              <span className="text-xs font-medium text-teal-600">
                                BẠN
                              </span>
                            ) : null}
                          </div>
                          <p className="mt-1 text-xs text-muted">
                            {account.email}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-5">
                      <RoleBadge role={account.role} />
                    </td>
                    <td className="px-4 py-5">
                      <span
                        className={`badge ${account.locked ? "bg-amber-50 text-amber-800 ring-amber-200" : "bg-emerald-50 text-emerald-800 ring-emerald-100"}`}
                      >
                        {account.locked ? (
                          <LockKeyhole size={12} />
                        ) : (
                          <Check size={12} />
                        )}
                        {account.locked ? "Đã khóa" : "Không khóa"}
                      </span>
                    </td>
                    <td className="px-4 py-5">
                      <span
                        className={`inline-flex items-center gap-1.5 text-xs ${account.verified ? "text-teal-600" : "text-muted"}`}
                      >
                        {account.verified ? <Check size={14} /> : null}
                        {account.verified ? "Đã xác minh" : "Chưa xác minh"}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-5 text-xs text-muted">
                      {date.format(new Date(account.createdAt))}
                    </td>
                    <td className="px-6 py-5 text-right">
                      <Button
                        variant="secondary"
                        className="ml-auto"
                        disabled={account.id === session.data?.id}
                        aria-label={`Quản lý tài khoản ${account.email}`}
                        title={
                          account.id === session.data?.id
                            ? "Không thể tự thay đổi quyền của mình"
                            : undefined
                        }
                        onClick={() => {
                          setSaved(false);
                          setSelected(account);
                        }}
                      >
                        <Settings2 size={14} />
                        <span>Quản lý</span>
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="flex flex-wrap items-center justify-between gap-4 border-t border-line px-6 py-4 text-xs text-muted">
          <p>
            {query.data
              ? `${query.data.totalElements === 0 ? 0 : page * 20 + 1}–${Math.min((page + 1) * 20, query.data.totalElements)} / ${query.data.totalElements.toLocaleString("vi-VN")} tài khoản`
              : "20 tài khoản mỗi trang"}
          </p>
          <nav
            aria-label="Phân trang tài khoản"
            className="flex items-center gap-3"
          >
            <Button
              variant="secondary"
              aria-label="Trang trước"
              disabled={page === 0 || query.isFetching}
              onClick={() => setPage(page - 1)}
            >
              <ChevronLeft size={15} />
            </Button>
            <span>
              Trang {page + 1} / {totalPages}
            </span>
            <Button
              variant="secondary"
              aria-label="Trang sau"
              disabled={
                page + 1 >= totalPages || query.isFetching || !query.data
              }
              onClick={() => setPage(page + 1)}
            >
              <ChevronRight size={15} />
            </Button>
          </nav>
        </div>
      </section>
      <p className="text-xs leading-5 text-muted">
        Thời gian hiển thị theo múi giờ Việt Nam. Quyền quản trị không cấp quyền
        truy cập chuyến đi hoặc nhật ký riêng của người dùng.
      </p>
      {selected && session.data ? (
        <AccountDialog
          account={selected}
          owner={session.data.id}
          onClose={closeDialog}
          onSaved={() => {
            setSelected(undefined);
            setSaved(true);
          }}
        />
      ) : null}
    </div>
  );
}
