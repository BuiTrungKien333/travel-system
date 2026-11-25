import { LoaderCircle } from "lucide-react";
import type {
  ButtonHTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
} from "react";

export function Button({
  children,
  busy = false,
  variant = "primary",
  className = "",
  disabled,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  busy?: boolean;
  variant?: "primary" | "secondary" | "danger";
}) {
  return (
    <button
      {...props}
      disabled={disabled || busy}
      className={`button button-${variant} ${className}`}
      aria-busy={busy || undefined}
    >
      {busy ? (
        <LoaderCircle size={16} className="animate-spin" aria-hidden="true" />
      ) : null}
      {children}
    </button>
  );
}

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={`input ${props.className ?? ""}`} />;
}

export function Banner({
  children,
  error = false,
}: {
  children: ReactNode;
  error?: boolean;
}) {
  return (
    <div
      role={error ? "alert" : "status"}
      className={`rounded-xl border px-4 py-3 text-sm ${error ? "border-red-200 bg-red-50 text-red-800" : "border-teal-100 bg-teal-50 text-teal-700"}`}
    >
      {children}
    </div>
  );
}

export function RoleBadge({ role }: { role: "USER" | "ADMIN" | "GUEST" }) {
  return (
    <span
      className={`badge ${role === "ADMIN" ? "bg-teal-50 text-teal-700 ring-teal-100" : "bg-slate-50 text-slate-600 ring-slate-200"}`}
    >
      {role === "ADMIN"
        ? "Quản trị viên"
        : role === "USER"
          ? "Người dùng"
          : "Khách"}
    </span>
  );
}

export function Brand({ light = false }: { light?: boolean }) {
  return (
    <div
      className={`flex items-center gap-2.5 ${light ? "text-white" : "text-ink"}`}
    >
      <span
        className={`grid size-10 place-items-center rounded-xl text-xl font-bold ${light ? "bg-white/15" : "bg-teal-600 text-white"}`}
        aria-hidden="true"
      >
        W
      </span>
      <span className="text-2xl font-bold tracking-tight">
        WeGo
        <span
          className={`ml-2 border-l pl-2 text-xs font-semibold uppercase tracking-[.12em] ${light ? "border-white/25 text-teal-100" : "border-line text-muted"}`}
        >
          Admin
        </span>
      </span>
    </div>
  );
}
