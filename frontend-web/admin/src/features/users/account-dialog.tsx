"use client";
import { useEffect, useRef } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ShieldCheck, X } from "lucide-react";
import {
  changeAccountSchema,
  type Account,
  type AccountChange,
} from "@/lib/contracts";
import { ApiError, errorMessage } from "@/lib/errors";
import { Banner, Button, RoleBadge } from "@/components/ui";
import { useChangeAccount } from "./api";

const roleNames = {
  USER: "Người dùng",
  ADMIN: "Quản trị viên",
  GUEST: "Khách",
};

export function AccountDialog({
  account,
  owner,
  onClose,
  onSaved,
}: {
  account: Account;
  owner: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const mutation = useChangeAccount(owner);
  const { register, handleSubmit, watch } = useForm<AccountChange>({
    resolver: zodResolver(changeAccountSchema),
    defaultValues: {
      role: account.role,
      locked: account.locked,
      version: account.version,
    },
  });
  const role = watch("role");
  const locked = watch("locked");
  const unchanged = role === account.role && locked === account.locked;
  const conflict =
    mutation.error instanceof ApiError && mutation.error.code === "ADMIN_001";
  useEffect(() => {
    const node = dialog.current;
    node?.showModal();
    return () => node?.close();
  }, []);

  return (
    <dialog
      ref={dialog}
      aria-labelledby="change-title"
      onCancel={(event) => {
        if (mutation.isPending) event.preventDefault();
        else onClose();
      }}
      className="w-[calc(100%-2rem)] max-w-lg rounded-2xl border border-line bg-white p-0 text-ink shadow-panel backdrop:bg-ink/40"
    >
      <form
        onSubmit={handleSubmit(async (change) => {
          try {
            await mutation.mutateAsync({ id: account.id, change });
            onSaved();
          } catch {
            /* The mutation error is displayed in this dialog. */
          }
        })}
      >
        <div className="flex items-start justify-between border-b border-line px-6 py-5">
          <div>
            <p className="eyebrow">QUẢN LÝ QUYỀN TRUY CẬP</p>
            <h2 id="change-title" className="mt-2 text-xl font-semibold">
              Cập nhật tài khoản
            </h2>
          </div>
          <button
            type="button"
            aria-label="Đóng hộp thoại"
            disabled={mutation.isPending}
            onClick={onClose}
            className="grid size-10 place-items-center rounded-lg text-muted hover:bg-canvas disabled:opacity-40"
          >
            <X size={20} />
          </button>
        </div>
        <div className="space-y-5 px-6 py-5">
          <div className="rounded-xl border border-line bg-canvas p-4">
            <p className="font-semibold">{account.fullName}</p>
            <p className="mt-1 break-all text-sm text-muted">{account.email}</p>
            <div className="mt-3">
              <RoleBadge role={account.role} />
            </div>
          </div>
          <div>
            <label className="label" htmlFor="account-role">
              Vai trò mới
            </label>
            <select
              id="account-role"
              className="input"
              disabled={mutation.isPending || conflict}
              {...register("role")}
            >
              <option value="USER">Người dùng</option>
              <option value="GUEST">Khách</option>
              <option value="ADMIN" disabled={!account.verified}>
                Quản trị viên
              </option>
            </select>
            {!account.verified ? (
              <p className="mt-2 text-xs leading-5 text-muted">
                Email chưa xác minh: chưa thể cấp quyền quản trị.
              </p>
            ) : null}
          </div>
          <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-line p-4">
            <input
              type="checkbox"
              className="mt-1 size-4 accent-teal-600"
              disabled={mutation.isPending || conflict}
              {...register("locked")}
            />
            <span>
              <span className="block text-sm font-semibold">
                Khóa tài khoản
              </span>
              <span className="mt-1 block text-xs leading-5 text-muted">
                Tài khoản bị khóa sẽ không thể đăng nhập hoặc tiếp tục phiên
                hiện tại.
              </span>
            </span>
          </label>
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-900">
            <p className="font-semibold">Xác nhận thay đổi</p>
            <p>
              {roleNames[account.role]} → {roleNames[role]}
              <br />
              {account.locked ? "Đã khóa" : "Không khóa"} →{" "}
              {locked ? "Đã khóa" : "Không khóa"}
            </p>
            <p className="mt-2 text-xs leading-5">
              Nếu thay đổi được lưu, các phiên đăng nhập của tài khoản này sẽ bị
              thu hồi. Người dùng cần đăng nhập lại.
            </p>
          </div>
          {mutation.isError ? (
            <Banner error>
              {errorMessage(mutation.error)}
              {conflict ? (
                <p className="mt-2">
                  Đóng hộp thoại và tải lại danh sách để xem trạng thái mới.
                </p>
              ) : null}
            </Banner>
          ) : null}
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line bg-canvas/50 px-6 py-4">
          <p className="flex items-center gap-1.5 text-xs text-muted">
            <ShieldCheck size={14} />
            Thao tác được ghi lịch sử
          </p>
          <div className="flex gap-2">
            <Button
              type="button"
              variant="secondary"
              disabled={mutation.isPending}
              onClick={onClose}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              disabled={unchanged || conflict}
              busy={mutation.isPending}
            >
              Lưu thay đổi
            </Button>
          </div>
        </div>
      </form>
    </dialog>
  );
}
