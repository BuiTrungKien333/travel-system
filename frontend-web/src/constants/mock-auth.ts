import type { User } from '@/features/auth/types';

export const MOCK_USER: User = {
  id: 'usr-001',
  fullName: 'Nguyễn Minh Quân',
  email: 'minhquan@example.com',
  avatarUrl: undefined, // Default avatar will render
  createdAt: '2026-01-15T08:00:00Z',
  updatedAt: '2026-08-20T08:00:00Z',
};
