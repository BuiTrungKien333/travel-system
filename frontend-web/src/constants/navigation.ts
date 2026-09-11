import { ROUTES } from './routes';

export const HEADER_NAV_LINKS = [
  {
    name: 'Chuyến đi',
    label: 'Chuyến đi của tôi',
    href: ROUTES.trips,
  },
  {
    name: 'Hành trình',
    label: 'Hành trình Việt Nam',
    href: ROUTES.journey,
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
  { label: 'Bản đồ tương tác', href: '/#showcase' },
  { label: 'Kỷ niệm hành trình', href: '/#showcase' },
  { label: 'Hành trình 63 tỉnh thành', href: ROUTES.journey },
];
