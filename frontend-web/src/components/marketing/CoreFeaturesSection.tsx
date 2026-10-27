import { Sparkles, Map, CloudSun, Camera, Compass } from "lucide-react";

const features = [
  {
    icon: Sparkles,
    title: "Lịch trình thông minh",
    description:
      "AI đề xuất thứ tự ghé thăm tối ưu theo khoảng cách và thời gian mở cửa. Bạn luôn là người quyết định cuối cùng.",
  },
  {
    icon: Map,
    title: "Bản đồ tương tác trực quan",
    description:
      "Tuyến đường từng ngày hiển thị liền mạch trên Google Maps, tự động đồng bộ khi bạn xem hay chọn địa điểm.",
  },
  {
    icon: CloudSun,
    title: "Ngữ cảnh thời tiết",
    description:
      "Cung cấp thông tin thời tiết dự kiến từng ngày để bạn chủ động điều chỉnh hoạt động ngoài trời hay trong nhà.",
  },
  {
    icon: Camera,
    title: "Kho kỷ niệm & ảnh chụp",
    description:
      "Lưu giữ ảnh chụp gắn liền với từng địa điểm bạn đã ghé thăm. Xem lại theo dạng lưới hoặc dòng thời gian như một cuốn nhật ký.",
  },
  {
    icon: Compass,
    title: "Hành trình 63 tỉnh thành",
    description:
      "Ghi nhận bước chân khám phá khắp dải đất Việt Nam. Mỗi chuyến đi và kỷ niệm sẽ đánh dấu một vùng đất mới được chinh phục.",
  },
];

export function CoreFeaturesSection() {
  return (
    <section className="bg-background py-16 lg:py-24 border-b border-border/60">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">

        {/* Section Header */}
        <div className="max-w-2xl">
          <p className="text-xs uppercase tracking-widest text-primary font-semibold">
            Tính năng cốt lõi
          </p>
          <h2 className="text-2xl sm:text-3xl lg:text-4xl font-bold tracking-tight text-foreground mt-2">
            Được thiết kế cho trải nghiệm du lịch thực tế
          </h2>
          <p className="mt-3 text-sm sm:text-base text-muted-foreground leading-relaxed">
            Không rườm rà, không phức tạp. Mọi tính năng đều tập trung vào việc giúp bạn lên kế hoạch nhanh và tận hưởng chuyến đi trọn vẹn.
          </p>
        </div>

        {/* Feature Grid */}
        <div className="mt-12 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 sm:gap-10">
          {features.map((item, index) => (
            <div
              key={index}
              className="flex flex-col space-y-3 pt-4 border-t border-border/60"
            >
              <div className="flex items-center gap-3">
                <item.icon className="h-5 w-5 text-primary" />
                <h3 className="text-base font-semibold text-foreground">
                  {item.title}
                </h3>
              </div>
              <p className="text-sm text-muted-foreground leading-relaxed">
                {item.description}
              </p>
            </div>
          ))}
        </div>

      </div>
    </section>
  );
}
