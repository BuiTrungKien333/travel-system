export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex-1 flex items-center justify-center bg-gradient-to-b from-sky-50/60 via-cyan-50/20 to-sky-100/60 py-14 px-4 min-h-[75vh]">
      <div className="w-full max-w-md">
        <div className="rounded-3xl border border-cyan-100 bg-white p-7 sm:p-8 shadow-xl shadow-cyan-950/5">
          {children}
        </div>
      </div>
    </div>
  );
}
