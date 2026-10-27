import { CheckCircle2, Car, Calendar } from "lucide-react";

export function DetailedFeaturesShowcase() {
  const sampleTimeline = [
    {
      time: "08:00",
      title: "Bún chả cá Hòn 113",
      category: "Ăn sáng · Đặc sản",
      duration: "1 giờ",
      distance: "Khởi hành",
      visited: true,
    },
    {
      time: "09:30",
      title: "Bãi biển Mỹ Khê",
      category: "Bãi biển · Tự do",
      duration: "2 giờ 15 phút",
      distance: "4.5 km · 12 phút",
      visited: true,
    },
    {
      time: "12:00",
      title: "Cơm Niêu Nhà Đỏ 2",
      category: "Ẩm thực địa phương",
      duration: "1 giờ 30 phút",
      distance: "2.1 km · 8 phút",
      visited: false,
    },
    {
      time: "15:00",
      title: "Ngũ Hành Sơn & Làng đá Non Nước",
      category: "Tham quan văn hóa",
      duration: "2 giờ",
      distance: "7.8 km · 15 phút",
      visited: false,
    },
  ];

  return (
    <section className="bg-muted/20 py-16 lg:py-24 border-b border-border/60">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">

          {/* Left Text Narrative */}
          <div className="lg:col-span-5 space-y-6">
            <div className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary uppercase tracking-wider">
              <Calendar className="h-3.5 w-3.5" />
              <span>Giao diện thực tế</span>
            </div>

            <h2 className="text-2xl sm:text-3xl lg:text-4xl font-bold tracking-tight text-foreground leading-snug">
              Nhịp điệu hành trình rõ ràng, trực quan theo từng ngày
            </h2>

            <p className="text-sm sm:text-base text-muted-foreground leading-relaxed">
              Mỗi ngày là một trang hành trình liền mạch: từ giờ bắt đầu, thời lượng gợi ý cho tới khoảng cách lái xe giữa các chặng. Không còn cảm giác bối rối khi tìm đường hay lo lắng muộn giờ.
            </p>

            <div className="pt-2 border-t border-border/60 grid grid-cols-2 gap-4">
              <div>
                <p className="text-xl font-bold text-foreground">01</p>
                <p className="text-xs text-muted-foreground mt-0.5">Kéo thả sắp xếp linh hoạt</p>
              </div>
              <div>
                <p className="text-xl font-bold text-foreground">02</p>
                <p className="text-xs text-muted-foreground mt-0.5">Tự động tính chặng di chuyển</p>
              </div>
            </div>
          </div>

          {/* Right High-Fidelity UI Card */}
          <div className="lg:col-span-7 bg-background rounded-xl border border-border shadow-sm p-6 sm:p-8">
            <div className="flex items-center justify-between pb-5 border-b border-border/60">
              <div>
                <span className="text-xs text-primary font-semibold">Ngày 1 trong 4 ngày</span>
                <h3 className="text-lg font-bold text-foreground mt-0.5">Khám phá ẩm thực & bờ biển</h3>
              </div>
              <span className="text-xs text-muted-foreground bg-muted/60 px-2.5 py-1 rounded-md font-medium">
                4 địa điểm · 15.4 km
              </span>
            </div>

            {/* Simulated Timeline */}
            <div className="mt-6 space-y-4">
              {sampleTimeline.map((place, idx) => (
                <div key={idx} className="relative flex gap-4 items-start group">
                  {/* Time column */}
                  <div className="w-14 shrink-0 text-right pt-0.5">
                    <span className="text-xs font-semibold text-foreground font-mono">{place.time}</span>
                  </div>

                  {/* Dot & line */}
                  <div className="relative flex flex-col items-center">
                    <div className={`h-3 w-3 rounded-full mt-1 shrink-0 ${
                      place.visited ? 'bg-primary ring-4 ring-primary/10' : 'bg-muted-foreground/30'
                    }`} />
                    {idx < sampleTimeline.length - 1 && (
                      <div className="w-px h-16 bg-border/80 my-1" />
                    )}
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0 bg-muted/20 hover:bg-muted/40 transition-colors rounded-lg p-3 border border-border/50">
                    <div className="flex items-center justify-between gap-2">
                      <h4 className="text-sm font-semibold text-foreground truncate">
                        {place.title}
                      </h4>
                      {place.visited && (
                        <span className="flex items-center gap-1 text-[11px] font-medium text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded">
                          <CheckCircle2 className="h-3 w-3" />
                          Đã ghé
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                      {place.category} · {place.duration}
                    </p>
                    {place.distance !== "Khởi hành" && (
                      <div className="flex items-center gap-1.5 text-[11px] text-muted-foreground/80 mt-2 pt-2 border-t border-border/40">
                        <Car className="h-3 w-3 text-primary/70" />
                        <span>Chặng tiếp theo: {place.distance}</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

        </div>

      </div>
    </section>
  );
}
