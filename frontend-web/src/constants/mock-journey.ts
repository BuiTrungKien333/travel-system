import type { JourneyStats } from '@/features/journey/types';

export const INITIAL_MOCK_JOURNEY: JourneyStats = {
  totalProvinces: 63,
  totalTrips: 3,
  totalPlaces: 18,
  totalMemories: 19,
  totalPhotos: 42,
  exploredProvinces: [
    { id: 'prov-danang', code: '48', name: 'Đà Nẵng' },
    { id: 'prov-quangnam', code: '49', name: 'Quảng Nam' },
    { id: 'prov-kiengiang', code: '91', name: 'Kiên Giang' },
    { id: 'prov-hanoi', code: '01', name: 'Hà Nội' },
    { id: 'prov-ninhbinh', code: '37', name: 'Ninh Bình' },
    { id: 'prov-quangninh', code: '22', name: 'Quảng Ninh' },
    { id: 'prov-hue', code: '46', name: 'Thừa Thiên Huế' },
    { id: 'prov-khanhhoa', code: '56', name: 'Khánh Hòa' },
    { id: 'prov-lamdong', code: '68', name: 'Lâm Đồng' },
    { id: 'prov-hcm', code: '79', name: 'TP. Hồ Chí Minh' },
    { id: 'prov-cantho', code: '92', name: 'Cần Thơ' },
  ],
};

export interface VietnamMapRegion {
  name: string;
  badge: string;
  provinces: string[];
}

export const VIETNAM_MAP_REGIONS: VietnamMapRegion[] = [
  {
    name: 'Miền Bắc',
    badge: '25 tỉnh/thành',
    provinces: [
      'Hà Nội',
      'Hà Giang',
      'Cao Bằng',
      'Bắc Kạn',
      'Tuyên Quang',
      'Lào Cai',
      'Điện Biên',
      'Lai Châu',
      'Sơn La',
      'Yên Bái',
      'Hòa Bình',
      'Thái Nguyên',
      'Lạng Sơn',
      'Quảng Ninh',
      'Bắc Giang',
      'Phú Thọ',
      'Vĩnh Phúc',
      'Bắc Ninh',
      'Hải Dương',
      'Hải Phòng',
      'Hưng Yên',
      'Thái Bình',
      'Hà Nam',
      'Nam Định',
      'Ninh Bình',
    ],
  },
  {
    name: 'Miền Trung & Tây Nguyên',
    badge: '19 tỉnh/thành',
    provinces: [
      'Thanh Hóa',
      'Nghệ An',
      'Hà Tĩnh',
      'Quảng Bình',
      'Quảng Trị',
      'Thừa Thiên Huế',
      'Đà Nẵng',
      'Quảng Nam',
      'Quảng Ngãi',
      'Bình Định',
      'Phú Yên',
      'Khánh Hòa',
      'Ninh Thuận',
      'Bình Thuận',
      'Kon Tum',
      'Gia Lai',
      'Đắk Lắk',
      'Đắk Nông',
      'Lâm Đồng',
    ],
  },
  {
    name: 'Miền Nam & ĐBSCL',
    badge: '19 tỉnh/thành',
    provinces: [
      'Bình Phước',
      'Tây Ninh',
      'Bình Dương',
      'Đồng Nai',
      'Bà Rịa - Vũng Tàu',
      'TP. Hồ Chí Minh',
      'Long An',
      'Tiền Giang',
      'Bến Tre',
      'Trà Vinh',
      'Vĩnh Long',
      'Đồng Tháp',
      'An Giang',
      'Kiên Giang',
      'Cần Thơ',
      'Hậu Giang',
      'Sóc Trăng',
      'Bạc Liêu',
      'Cà Mau',
    ],
  },
];

export interface JourneyMilestoneItem {
  target: number;
  label: string;
  icon: string;
}

export const JOURNEY_MILESTONES_DATA: JourneyMilestoneItem[] = [
  { target: 5, label: 'Bước chân đầu tiên', icon: '🌱' },
  { target: 10, label: 'Nhà thám hiểm', icon: '🧭' },
  { target: 20, label: 'Lữ khách dày dạn', icon: '🎒' },
  { target: 32, label: 'Nửa đường chinh phục', icon: '⛰️' },
  { target: 50, label: 'Huyền thoại du lịch', icon: '⭐' },
  { target: 63, label: 'Chinh phục Việt Nam', icon: '👑' },
];
