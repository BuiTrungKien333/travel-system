"use client";

import {
  ResponsiveContainer,
  BarChart,
  Bar,
  LabelList,
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

const tooltipProps = {
  contentStyle: { padding: "6px 8px", fontSize: 12 },
  labelStyle: { marginBottom: 2, fontSize: 12 },
  itemStyle: { paddingTop: 1, paddingBottom: 1, fontSize: 12 },
};

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

export function Bars({
  data,
  multiColor = false,
}: {
  data: Point[];
  multiColor?: boolean;
}) {
  const rows = data.map((p) => ({ ...p, name: labels[p.name] ?? p.name }));
  const palette = [
    "#117B83",
    "#E9B56E",
    "#93AED9",
    "#81C9BB",
    "#9BAEB6",
    "#21B5C3",
  ];

  return (
    <>
      <div
        className={multiColor ? undefined : "h-72"}
        style={
          multiColor
            ? { height: Math.max(rows.length * 44 + 40, 140) }
            : undefined
        }
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
              tick={{ fontSize: 12 }}
              axisLine={false}
              tickLine={false}
            />
            <Tooltip {...tooltipProps} />
            <Bar
              isAnimationActive={false}
              dataKey="value"
              name="Số lượng"
              fill={colors[0]}
              barSize={multiColor ? 26 : undefined}
              radius={[0, 5, 5, 0]}
            >
              {multiColor
                ? rows.map((p, i) => (
                    <Cell key={p.name} fill={palette[i % palette.length]} />
                  ))
                : null}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </>
  );
}

export function TopPlacesBars({ data }: { data: Point[] }) {
  const rows = data
    .slice(0, 10)
    .map((p) => ({ ...p, name: labels[p.name] ?? p.name }));
  const shorten = (value: string) =>
    value.length > 18 ? `${value.slice(0, 17)}…` : value;

  return (
    <div
      className="h-80"
      role="img"
      aria-label={rows.map((p) => `${p.name}: ${p.value}`).join(", ")}
    >
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={rows}
          margin={{ top: 18, right: 12, left: 0, bottom: 8 }}
        >
          <CartesianGrid vertical={false} stroke="#EDF1F1" />
          <XAxis
            dataKey="name"
            interval={0}
            angle={-35}
            textAnchor="end"
            height={88}
            tick={{ fontSize: 10, fill: "#475569" }}
            tickFormatter={shorten}
            axisLine={false}
            tickLine={false}
          />
          <YAxis
            allowDecimals={false}
            width={32}
            tick={{ fontSize: 11 }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip {...tooltipProps} />
          <Bar
            isAnimationActive={false}
            dataKey="value"
            name="Số lượng"
            fill={colors[0]}
            maxBarSize={32}
            radius={[5, 5, 0, 0]}
          >
            <LabelList
              dataKey="value"
              position="top"
              style={{ fontSize: 10, fontWeight: 600, fill: "#117B83" }}
            />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function TopProvincesBars({ data }: { data: Point[] }) {
  const rows = data.slice(0, 5);
  const max = Math.max(...rows.map((p) => p.value), 1);

  return (
    <ol
      className="space-y-3.5"
      role="img"
      aria-label={rows
        .map((p, i) => `${i + 1}. ${p.name}: ${p.value}`)
        .join(", ")}
    >
      {rows.map((p, index) => {
        const width = p.value > 0 ? Math.max((p.value / max) * 100, 4) : 0;

        return (
          <li key={`${p.name}-${index}`} className="flex items-center gap-3">
            <span className="w-5 shrink-0 text-right tabular-nums text-xs font-semibold text-muted">
              {index + 1}.
            </span>
            <ProvinceThumb name={p.name} index={index} />
            <div className="min-w-0 flex-1 space-y-1.5">
              <div className="flex items-baseline justify-between gap-2 text-sm">
                <span className="truncate font-medium text-ink" title={p.name}>
                  {p.name}
                </span>
                <span className="shrink-0 tabular-nums text-xs font-semibold text-teal-800">
                  {p.value.toLocaleString("vi-VN")} chuyến
                </span>
              </div>
              <div className="h-2.5 w-full overflow-hidden rounded-full bg-canvas">
                <div
                  className="h-full rounded-full"
                  style={{
                    width: `${width}%`,
                    backgroundColor: colors[0],
                  }}
                />
              </div>
            </div>
          </li>
        );
      })}
    </ol>
  );
}

function ProvinceThumb({ name, index }: { name: string; index: number }) {
  const key = name.toLowerCase();
  const [bgTop, bgBottom, accent] = key.includes("đà nẵng")
    ? ["#0EA5E9", "#0F766E", "#FDE68A"]
    : key.includes("huế")
      ? ["#8B5CF6", "#4C1D95", "#FCD34D"]
      : key.includes("lâm đồng")
        ? ["#10B981", "#065F46", "#FEF08A"]
        : key.includes("khánh hòa")
          ? ["#06B6D4", "#0E7490", "#FED7AA"]
          : key.includes("hà nội")
            ? ["#EF4444", "#991B1B", "#FDE68A"]
            : [colors[index % colors.length], "#0F172A", "#E2E8F0"];
  const gradId = `prov-grad-${index}`;

  return (
    <div
      className="h-10 w-10 shrink-0 overflow-hidden rounded-lg border border-line"
      aria-hidden="true"
    >
      <svg viewBox="0 0 40 40" className="h-full w-full">
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={bgTop} />
            <stop offset="100%" stopColor={bgBottom} />
          </linearGradient>
        </defs>
        <rect width="40" height="40" fill={`url(#${gradId})`} />
        <circle cx="29" cy="11" r="4" fill={accent} opacity="0.9" />
        <path
          d="M0 29 Q10 21 20 26 T40 24 L40 40 L0 40 Z"
          fill="#0F172A"
          opacity="0.28"
        />
        <path
          d="M0 33 Q14 27 25 31 T40 30 L40 40 L0 40 Z"
          fill="#FFFFFF"
          opacity="0.22"
        />
      </svg>
    </div>
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
          <Tooltip {...tooltipProps} />
          <Legend wrapperStyle={{ fontSize: 12 }} />
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
            tick={{ fontSize: 12 }}
          />
          <YAxis allowDecimals={false} width={35} />
          <Tooltip {...tooltipProps} />
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
