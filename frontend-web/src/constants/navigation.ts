import { ROUTES } from './routes';

export const HEADER_NAV_LINKS = [
  {
    name: 'Travel Guides',
    label: 'Cẩm nang',
    href: ROUTES.travelGuides,
  },
  {
    name: 'Journal',
    label: 'Nhật ký hành trình',
    href: ROUTES.journal,
  },
  {
    name: 'About Us',
    label: 'Về chúng tôi',
    href: '/#about',
  },
  {
    name: 'Contact',
    label: 'Liên hệ',
    href: '/#contact',
  },
];

export const FOOTER_PRODUCT_LINKS = [
  { label: 'Lập lịch trình AI', href: '/#features' },
  { label: 'Tối ưu lộ trình Held-Karp', href: '/#showcase' },
  { label: 'Quản lý đặt phòng & vé', href: '/#showcase' },
  { label: 'Cộng tác nhóm trực tiếp', href: '/#showcase' },
  { label: 'Chế độ ngoại tuyến & PDF', href: '/#showcase' },
  { label: 'Bản đồ 63 tỉnh thành', href: ROUTES.memory },
];
