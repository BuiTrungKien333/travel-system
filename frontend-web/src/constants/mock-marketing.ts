import {
  Sparkles,
  Map,
  Users,
  CalendarDays,
  DollarSign,
  Camera,
  Globe,
  Heart,
  Zap,
} from 'lucide-react';

export const MARKETING_CORE_FEATURES = [
  {
    icon: Sparkles,
    iconBg: 'bg-cyan-500/10 text-cyan-600 border-cyan-200/60',
    badge: 'Công nghệ AI',
    title: 'Tự động tối ưu lịch trình với AI',
    description:
      'Thuật toán Held–Karp giải bài toán người du lịch thông minh, sắp xếp thứ tự các điểm đến tiết kiệm thời gian nhất và tránh đi đường vòng.',
  },
  {
    icon: Map,
    iconBg: 'bg-sky-500/10 text-sky-600 border-sky-200/60',
    badge: 'Google Maps',
    title: 'Bản đồ tương tác & Đo khoảng cách',
    description:
      'Hiển thị tất cả điểm tham quan trên bản đồ số. Tự động tính toán khoảng cách và thời gian di chuyển theo ô tô, xe máy hoặc đi bộ.',
  },
  {
    icon: Users,
    iconBg: 'bg-teal-500/10 text-teal-600 border-teal-200/60',
    badge: 'Cộng tác nhóm',
    title: 'Lên kế hoạch cùng bạn bè thời gian thực',
    description:
      'Mời bạn bè tham gia chỉnh sửa lịch trình, bình chọn quán ăn, thêm ghi chú và phân chia công việc đồng thời như dùng Google Docs.',
  },
  {
    icon: CalendarDays,
    iconBg: 'bg-indigo-500/10 text-indigo-600 border-indigo-200/60',
    badge: 'Tất cả trong một',
    title: 'Quản lý đặt phòng & Vé máy bay',
    description:
      'Tập trung toàn bộ mã xác nhận vé máy bay, khách sạn, vé tour và nhà hàng vào một dòng thời gian duy nhất, không lo thất lạc.',
  },
  {
    icon: DollarSign,
    iconBg: 'bg-amber-500/10 text-amber-600 border-amber-200/60',
    badge: 'Chi tiêu thông minh',
    title: 'Theo dõi ngân sách & Tự động chia tiền',
    description:
      'Ghi lại chi phí chuyến đi theo thời gian thực, phân loại khoản chi và tự động tính toán số tiền mỗi người cần thanh toán.',
  },
  {
    icon: Camera,
    iconBg: 'bg-rose-500/10 text-rose-600 border-rose-200/60',
    badge: 'Kỷ niệm & Bản đồ',
    title: 'Lưu giữ kỷ niệm & Bản đồ 63 tỉnh thành',
    description:
      'Tô màu bản đồ Việt Nam với những tỉnh thành bạn đã khám phá. Lưu trữ album ảnh, đánh giá và cảm xúc sau mỗi chuyến đi.',
  },
];

export const POPULAR_DESTINATIONS_DATA = [
  {
    name: 'Đà Nẵng & Hội An',
    region: 'Miền Trung',
    image: 'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=800',
    tripsPlanned: '14,200+',
    rating: '4.95',
    highlights: ['Cầu Vàng', 'Phố cổ Hội An', 'Bãi biển Mỹ Khê', 'Chùa Linh Ứng'],
  },
  {
    name: 'Hà Giang',
    region: 'Đông Bắc Bộ',
    image: 'https://images.unsplash.com/photo-1528127269322-539801943592?w=800',
    tripsPlanned: '8,400+',
    rating: '4.98',
    highlights: ['Đèo Mã Pí Lèng', 'Sông Nho Quế', 'Cột cờ Lũng Cú', 'Tam giác mạch'],
  },
  {
    name: 'Phú Quốc',
    region: 'Đồng bằng Sông Cửu Long',
    image: 'https://images.unsplash.com/photo-1589394815804-964ed0be2eb5?w=800',
    tripsPlanned: '11,800+',
    rating: '4.91',
    highlights: ['Hòn Thơm', 'Bãi Sao', 'Sunset Sanato', 'Chợ đêm Phú Quốc'],
  },
  {
    name: 'Ninh Bình',
    region: 'Đồng bằng Sông Hồng',
    image: 'https://images.unsplash.com/photo-1570168007204-dfb528c6958f?w=800',
    tripsPlanned: '9,300+',
    rating: '4.93',
    highlights: ['Quần thể Tràng An', 'Đỉnh Hang Múa', 'Chùa Bái Đính', 'Tam Cốc'],
  },
  {
    name: 'Đà Lạt',
    region: 'Tây Nguyên',
    image: 'https://images.unsplash.com/photo-1583417319070-4a69db38a482?w=800',
    tripsPlanned: '16,500+',
    rating: '4.92',
    highlights: ['Hồ Tuyền Lâm', 'Đồi Chè Cầu Đất', 'Thung Lũng Tình Yêu', 'Chợ Đêm'],
  },
  {
    name: 'Sa Pa',
    region: 'Tây Bắc Bộ',
    image: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800',
    tripsPlanned: '10,100+',
    rating: '4.94',
    highlights: ['Đỉnh Fansipan', 'Bản Cát Cát', 'Đèo Ô Quy Hồ', 'Thung Lũng Mường Hoa'],
  },
];

export const TESTIMONIAL_REVIEWS = [
  {
    name: 'Minh Anh & Hoàng Hải',
    role: 'Travel Blogger • 45 tỉnh thành đã đi',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120',
    rating: 5,
    title: 'Chấm dứt hoàn toàn thời kỳ lên kế hoạch bằng Excel!',
    content:
      'Trước đây mỗi chuyến du lịch nhóm 6-8 người là ác mộng với bảng tính Excel và hàng trăm tin nhắn lạc đề. Với Tripzy, cả nhóm cùng vào kéo thả trên bản đồ, vote quán ăn và chia tiền cực kỳ minh bạch. Chuyến đi Đà Nẵng vừa rồi mượt mà hơn mong đợi rất nhiều!',
    tag: 'Du lịch nhóm',
  },
  {
    name: 'Trần Đức Nam',
    role: 'Nhiếp ảnh gia tự do & Du khách phượt',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120',
    rating: 5,
    title: 'AI tối ưu lộ trình tiết kiệm hơn 2 tiếng mỗi ngày',
    content:
      'Thuật toán AI sắp xếp tuyến đường thông minh đáng kinh ngạc. Mình đi Hà Giang 5 ngày, ứng dụng tự gợi ý các chặng ngắm bình minh và hoàng hôn đúng thời điểm, tránh đi ngược đường giúp tiết kiệm rất nhiều xăng xe và thời gian.',
    tag: 'Phượt & Nhiếp ảnh',
  },
  {
    name: 'Nguyễn Lê Thảo Vy',
    role: 'Quản lý Marketing • Du lịch gia đình',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=120',
    rating: 5,
    title: 'Cứu cánh cho chuyến đi có trẻ nhỏ và người lớn tuổi',
    content:
      'Đi du lịch cùng bố mẹ và bé 3 tuổi cần lịch trình nhẹ nhàng, có thời gian nghỉ trưa. Tripzy cho phép chọn chế độ "Nhẹ nhàng", tự động gom các điểm gần nhau và lưu toàn bộ vé máy bay, phòng resort ở một nơi.',
    tag: 'Du lịch gia đình',
  },
];

export const PRESS_LOGOS_DATA = [
  { name: 'VnExpress', desc: 'Ứng dụng du lịch đột phá công nghệ AI tại Việt Nam' },
  { name: 'Forbes Vietnam', desc: 'Top nền tảng số đổi mới sáng tạo cho giới trẻ' },
  { name: 'Travel + Leisure', desc: 'Trợ lý du lịch không thể thiếu cho du khách tự túc' },
  { name: 'Tuổi Trẻ', desc: 'Giải pháp chuyển đổi số du lịch 63 tỉnh thành' },
  { name: 'Zing News', desc: 'Xu hướng du lịch thông minh và bền vững' },
];

export const FAQ_DATA = [
  {
    q: 'Ứng dụng Tripzy có thực sự miễn phí không?',
    a: 'Hoàn toàn miễn phí! Bạn có thể tạo không giới hạn số lượng chuyến đi, mời bạn bè cùng chỉnh sửa, sử dụng tính năng tối ưu lịch trình AI và xuất file PDF mà không phải trả bất kỳ khoản phí nào.',
  },
  {
    q: 'Tính năng AI tối ưu lịch trình hoạt động như thế nào?',
    a: 'Tripzy tích hợp thuật toán Held-Karp kết hợp dữ liệu giao thông thực tế từ Google Maps. Sau khi bạn chọn các địa điểm muốn đến, AI sẽ tự động phân tích khoảng cách và đề xuất thứ tự ghé thăm tối ưu nhất giúp bạn tiết kiệm 30-50% thời gian di chuyển và tránh đi đường vòng.',
  },
  {
    q: 'Tôi có thể sử dụng ứng dụng khi không có mạng Internet (offline) không?',
    a: 'Có! Tripzy tự động lưu trữ dữ liệu chuyến đi trên thiết bị của bạn. Khi đến các vùng núi cao hoặc hải đảo sóng yếu, bạn vẫn có thể mở xem toàn bộ lịch trình, số điện thoại khách sạn, mã vé máy bay và ghi chú.',
  },
  {
    q: 'Làm thế nào để cộng tác và lên lịch trình cùng bạn bè?',
    a: 'Rất đơn giản, bạn chỉ cần nhấn nút "Chia sẻ" và gửi đường link cho bạn bè. Mọi người có thể cùng mở trên điện thoại hoặc máy tính để thêm địa điểm, bình chọn quán ăn và ghi chú theo thời gian thực như trên Google Docs.',
  },
  {
    q: 'Tôi có thể xuất lịch trình sang Google Maps hoặc in ra giấy không?',
    a: 'Được chứ! Tripzy hỗ trợ xuất file PDF khổ A4 thiết kế đẹp mắt để bạn in ra giấy hoặc lưu vào điện thoại. Ngoài ra bạn cũng có thể đồng bộ toàn bộ danh sách điểm ghim sang Google Maps chỉ với 1 thao tác.',
  },
  {
    q: 'Dữ liệu địa điểm tại 63 tỉnh thành Việt Nam có chính xác không?',
    a: 'Dữ liệu trên Tripzy được cập nhật liên tục từ Google Maps API kết hợp với đánh giá thực tế từ cộng đồng du lịch Việt Nam, đảm bảo giờ mở cửa, giá vé và địa chỉ luôn chuẩn xác.',
  },
];

export const ABOUT_VALUES_DATA = [
  {
    icon: Sparkles,
    title: 'Trí tuệ nhân tạo vì du khách',
    desc: 'Ứng dụng thuật toán AI Held-Karp giúp giải bài toán tối ưu tuyến đường phức tạp chỉ trong vài giây.',
  },
  {
    icon: Globe,
    title: 'Bao phủ 63 tỉnh thành Việt Nam',
    desc: 'Dữ liệu địa điểm chi tiết, chuẩn xác từ hải đảo Phú Quốc, Côn Đảo đến rẻo cao Sa Pa, Hà Giang.',
  },
  {
    icon: Heart,
    title: 'Cộng đồng & Trải nghiệm tự do',
    desc: 'Giúp việc lên kế hoạch nhóm trở nên vui vẻ, minh bạch và gắn kết các thành viên.',
  },
  {
    icon: Zap,
    title: 'Nhanh chóng & Tiết kiệm thời gian',
    desc: 'Thay thế hàng giờ tìm kiếm rời rạc bằng một giao diện trực quan duy nhất.',
  },
];
