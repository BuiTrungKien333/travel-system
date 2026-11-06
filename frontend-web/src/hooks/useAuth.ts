import { useAuthStore } from '@/features/auth/store/useAuthStore';

/**
 * Global auth state hook.
 * Reads from persisted Zustand auth store.
 */
export function useAuth() {
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const clearAuth = useAuthStore((s) => s.clearAuth);

  return { user, isAuthenticated, clearAuth };
}
