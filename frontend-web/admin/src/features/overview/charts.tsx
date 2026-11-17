"use client";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  PieChart,
  Pie,
  Cell,
  Legend,
  LineChart,
  Line,
} from "recharts";
import { labels, type Point } from "./data";
const colors = [
  "#117B83",
  "#21B5C3",
  "#81C9BB",
  "#E9B56E",
  "#9BAEB6",
  "#93AED9",
];
export function ChartCard({
  title,
  note,
  sample = false,
  children,
}: {
  title: string;
  note: string;
  sample?: boolean;
  children: React.ReactNode;
}) {
  return (
    <section className="min-w-0 rounded-2xl border border-line bg-white p-6 shadow-panel">
      <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
        <h2 className="font-semibold text-ink">{title}</h2>
        {sample ? (
          <span className="badge bg-amber-50 text-amber-800 ring-amber-200">
            Dữ liệu mẫu
          </span>
        ) : null}
      </div>
      <p className="mb-6 text-xs leading-5 text-muted">{note}</p>
      {children}
    </section>
  );
}
export function Bars({ data }: { data: Point[] }) {
  const rows = data.map((p) => ({ ...p, name: labels[p.name] ?? p.name }));
  return (
    <>
      <div
        className="h-72"
        role="img"
        aria-label={rows.map((p) => `${p.name}: ${p.value}`).join(", ")}
      >
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={rows}
            layout="vertical"
            margin={{ left: 0, right: 24 }}
          >
            <CartesianGrid horizontal={false} stroke="#EDF1F1" />
            <XAxis type="number" allowDecimals={false} />
            <YAxis
              type="category"
              dataKey="name"
              width={128}
              tick={{ fontSize: 11 }}
              axisLine={false}
              tickLine={false}
            />
            <Tooltip />
            <Bar
              isAnimationActive={false}
              dataKey="value"
              name="Số lượng"
              fill={colors[0]}
              radius={[0, 5, 5, 0]}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </>
  );
}
export function Donut({ data }: { data: Point[] }) {
  const rows = data.map((p) => ({ ...p, name: labels[p.name] ?? p.name }));
  return (
    <div
      className="h-72"
      role="img"
      aria-label={rows.map((p) => `${p.name}: ${p.value}`).join(", ")}
    >
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            isAnimationActive={false}
            data={rows}
            dataKey="value"
            nameKey="name"
            label={({ percent }) => `${Math.round((percent ?? 0) * 100)}%`}
            labelLine={false}
            innerRadius={65}
            outerRadius={95}
            paddingAngle={3}
          >
            {rows.map((p, i) => (
              <Cell key={p.name} fill={colors[i % colors.length]} />
            ))}
          </Pie>
          <Tooltip />
          <Legend wrapperStyle={{ fontSize: 11 }} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
export function Trend({
  data,
}: {
  data: { name: string; users: number; trips: number }[];
}) {
  return (
    <div
      className="h-72"
      role="img"
      aria-label="Xu hướng tài khoản và chuyến đi mới theo ngày"
    >
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data}>
          <CartesianGrid stroke="#EDF1F1" vertical={false} />
          <XAxis
            dataKey="name"
            tickFormatter={(v) => String(v).slice(5)}
            minTickGap={26}
            tick={{ fontSize: 11 }}
          />
          <YAxis allowDecimals={false} width={35} />
          <Tooltip />
          <Legend />
          <Line
            isAnimationActive={false}
            type="monotone"
            dataKey="users"
            name="Tài khoản mới"
            stroke={colors[0]}
            strokeWidth={2.5}
            dot={false}
          />
          <Line
            isAnimationActive={false}
            type="monotone"
            dataKey="trips"
            name="Chuyến đi mới"
            stroke={colors[1]}
            strokeWidth={2.5}
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
