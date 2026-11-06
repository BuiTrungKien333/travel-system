export interface GuideItem {
  id: string;
  title: string;
  destination: string;
  description: string;
  image: string;
  verified: boolean;
  author: {
    name: string;
    avatar: string;
    initials?: string;
  };
  likes: number;
  views: number;
}

export const GUIDE_QUICK_CHIPS = [
  'Japan',
  'New York City',
  'Paris',
  'Vietnam',
  'London',
  'Bali',
  'Đà Nẵng',
  'Hà Giang',
  'Phú Quốc',
];

export const INITIAL_GUIDES_DATA: GuideItem[] = [
  {
    id: 'guide-1',
    title: 'Japan: Video Game Guide & Pop Culture Journey',
    destination: 'Japan',
    description:
      "quest-like fashion guide with all the places I've personally been to and loved across Tokyo, Osaka and Kyoto.",
    image:
      'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=600&auto=format&fit=crop&q=80',
    verified: true,
    author: {
      name: 'Panda in Japan',
      avatar:
        'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=60&auto=format&fit=crop&q=80',
    },
    likes: 17,
    views: 1800,
  },
  {
    id: 'guide-2',
    title: 'Wanderlog community: Top places in Japan',
    destination: 'Japan',
    description:
      'Explore the top places in Japan recommended by the Wanderlog community: Fushimi Inari, Dotonbori and more.',
    image:
      'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=600&auto=format&fit=crop&q=80',
    verified: false,
    author: {
      name: 'Wanderlog staff',
      avatar:
        'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=60&auto=format&fit=crop&q=80',
    },
    likes: 2,
    views: 2900,
  },
  {
    id: 'guide-3',
    title: 'The Best of New York City in 4 Days',
    destination: 'New York City',
    description:
      'A thorough first-timer guide covering Central Park, The Met, Brooklyn Bridge and iconic skyline vistas.',
    image:
      'https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=600&auto=format&fit=crop&q=80',
    verified: true,
    author: {
      name: 'Wanderlog staff',
      avatar:
        'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=60&auto=format&fit=crop&q=80',
    },
    likes: 6,
    views: 5200,
  },
  {
    id: 'guide-4',
    title: '3 Days in Oahu, Hawaii: Beaches, Hikes & Food',
    destination: 'Oahu',
    description:
      'Short and sweet 3-day itinerary highlighting North Shore food trucks, Lanikai Beach and Diamond Head crater hike.',
    image:
      'https://images.unsplash.com/photo-1542259009477-d625272157b7?w=600&auto=format&fit=crop&q=80',
    verified: false,
    author: {
      name: 'Travel with Chloe',
      avatar:
        'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=60&auto=format&fit=crop&q=80',
    },
    likes: 2,
    views: 318,
  },
  {
    id: 'guide-5',
    title: 'Paris for Art Lovers: Louvre, Orsay & Montmartre',
    destination: 'Paris',
    description:
      'Museum passes, hidden courtyards, and quintessential Parisian cafes in the Latin Quarter and Marais.',
    image:
      'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=600&auto=format&fit=crop&q=80',
    verified: true,
    author: {
      name: 'Claire Dupont',
      avatar:
        'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=60&auto=format&fit=crop&q=80',
    },
    likes: 12,
    views: 940,
  },
  {
    id: 'guide-6',
    title: '2 Weeks in Scotland: Castles, Highlands & Skye',
    destination: 'Scotland',
    description:
      'The ultimate Scottish road trip guide: Edinburgh, Loch Ness, Isle of Skye and the scenic Glen Coe pass.',
    image:
      'https://images.unsplash.com/photo-1506377247377-2a5b3b417ebb?w=600&auto=format&fit=crop&q=80',
    verified: false,
    author: {
      name: 'Highland Wanderer',
      avatar:
        'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=60&auto=format&fit=crop&q=80',
    },
    likes: 8,
    views: 412,
  },
  {
    id: 'guide-7',
    title: 'The Ultimate Iceland Ring Road Route (7 Days)',
    destination: 'Iceland',
    description:
      'Waterfalls, black sand beaches, glacial lagoons and Northern Lights viewpoints along Route 1.',
    image:
      'https://images.unsplash.com/photo-1504893524553-b855bce32c67?w=600&auto=format&fit=crop&q=80',
    verified: true,
    author: {
      name: 'Erik Thor',
      avatar:
        'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=60&auto=format&fit=crop&q=80',
    },
    likes: 29,
    views: 3100,
  },
  {
    id: 'guide-8',
    title: 'Wanderlog community: Top places in London',
    destination: 'London',
    description:
      'Curated community favorites across London: West End theatre, Soho eateries, and Thames scenic walks.',
    image:
      'https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=600&auto=format&fit=crop&q=80',
    verified: false,
    author: {
      name: 'Wanderlog staff',
      avatar:
        'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=60&auto=format&fit=crop&q=80',
    },
    likes: 3,
    views: 1200,
  },
  {
    id: 'guide-9',
    title: 'Amsterdam Canals, Bikes & Vintage Flea Markets',
    destination: 'Amsterdam',
    description:
      'Jordaan neighborhood strolls, museum square tips and the coziest brown cafes by the canals.',
    image:
      'https://images.unsplash.com/photo-1512470876302-972faa2aa9a4?w=600&auto=format&fit=crop&q=80',
    verified: false,
    author: {
      name: 'Marthe Maru',
      avatar:
        'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=60&auto=format&fit=crop&q=80',
    },
    likes: 1,
    views: 107,
  },
  {
    id: 'guide-10',
    title: "Abby's Peru Trip — 6 Weeks Overlanding",
    destination: 'Peru',
    description:
      '6 Weeks overlanding Peru. My route, high-altitude acclimation tips, and secret spots advice.',
    image:
      'https://images.unsplash.com/photo-1526392060635-9d6019884377?w=600&auto=format&fit=crop&q=80',
    verified: false,
    author: {
      name: 'Abigail B',
      avatar:
        'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=60&auto=format&fit=crop&q=80',
    },
    likes: 1,
    views: 17,
  },
  {
    id: 'guide-11',
    title: 'Madeira, Portugal, and Morocco Coastal Tour',
    destination: 'Lisbon',
    description:
      'Wonderful trip to Madeira, Lisbon, and Marrakech! Full tram routes and scenic hiking miradouros.',
    image:
      'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=600&auto=format&fit=crop&q=80',
    verified: false,
    author: {
      name: 'Daniel Slack',
      avatar:
        'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=60&auto=format&fit=crop&q=80',
    },
    likes: 0,
    views: 17,
  },
  {
    id: 'guide-12',
    title: 'Đà Nẵng & Hội An 4N3Đ: Ẩm thực & Biển xanh',
    destination: 'Vietnam',
    description:
      'Lịch trình chi tiết 4N3Đ: Tắm biển Mỹ Khê, ngắm hoàng hôn Bà Nà Hills, thả đèn hoa đăng Hội An và ăn sập chợ đêm.',
    image:
      'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=600&auto=format&fit=crop&q=80',
    verified: true,
    author: {
      name: 'Minh Anh',
      avatar:
        'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=60&auto=format&fit=crop&q=80',
    },
    likes: 1420,
    views: 45820,
  },
];

export const MORE_GUIDES_DATA: GuideItem[] = [
  {
    id: 'guide-13',
    title: 'Phú Quốc: Thiên đường Đảo Ngọc & Sunset Sanato 3N2Đ',
    destination: 'Vietnam',
    description:
      'Kinh nghiệm lặn ngắm san hô Hòn Thơm, trải nghiệm Grand World và thưởng thức hải sản Hàm Ninh ngon bổ rẻ.',
    image:
      'https://images.unsplash.com/photo-1540206351-d6465b3ac5c1?w=600&auto=format&fit=crop&q=80',
    verified: true,
    author: {
      name: 'Hoàng Nam',
      avatar:
        'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=60&auto=format&fit=crop&q=80',
    },
    likes: 890,
    views: 32400,
  },
  {
    id: 'guide-14',
    title: 'Hà Nội & Tràng An: Nghìn Năm Văn Hiến 4N3Đ',
    destination: 'Vietnam',
    description:
      'Khám phá 36 phố phường, cafe trứng hồ Gươm và chèo thuyền ngoạn cảnh danh thắng Tràng An Ninh Bình.',
    image:
      'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=600&auto=format&fit=crop&q=80',
    verified: true,
    author: {
      name: 'Thảo Vy',
      avatar:
        'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=60&auto=format&fit=crop&q=80',
    },
    likes: 1150,
    views: 41200,
  },
  {
    id: 'guide-15',
    title: 'Sa Pa & Hà Giang: Mùa Vàng Ruộng Bậc Thang',
    destination: 'Vietnam',
    description:
      'Trekking Fansipan, ngắm hoa tam giác mạch và chinh phục đỉnh đèo Mã Pí Lèng hùng vĩ bậc nhất.',
    image:
      'https://images.unsplash.com/photo-1528127269322-539801943592?w=600&auto=format&fit=crop&q=80',
    verified: true,
    author: {
      name: 'Đức Tuấn',
      avatar:
        'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=60&auto=format&fit=crop&q=80',
    },
    likes: 980,
    views: 29800,
  },
  {
    id: 'guide-16',
    title: 'London in 4 Days: Museums, Royal Parks & Pubs',
    destination: 'London',
    description:
      'A complete 4-day itinerary covering Big Ben, British Museum, Borough Market, and scenic Thames river walks.',
    image:
      'https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=600&auto=format&fit=crop&q=80',
    verified: true,
    author: {
      name: 'Oliver Smith',
      avatar:
        'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=60&auto=format&fit=crop&q=80',
    },
    likes: 640,
    views: 18900,
  },
];

export const DESTINATION_TAGS_DATA = [
  'Japan',
  'New York City',
  'Paris',
  'London',
  'Scotland',
  'Tokyo',
  'Iceland',
  'Oahu',
  'Amsterdam',
  'San Francisco',
  'Bali',
  'Orlando',
  'Barcelona',
  'Rome',
  'Singapore',
  'Seoul',
  'Italy',
  'Ireland',
  'Mexico City',
  'Spain',
  'Hong Kong',
  'Los Angeles',
  'Vietnam',
  'South Korea',
  'New Zealand',
  'Las Vegas',
  'Lisbon',
  'Milan',
  'Toronto',
  'Da Nang',
  'Phu Quoc',
  'Ha Noi',
  'Sa Pa',
  'Da Lat',
  'Bangkok',
  'Sydney',
  'Kyoto',
  'Berlin',
];
