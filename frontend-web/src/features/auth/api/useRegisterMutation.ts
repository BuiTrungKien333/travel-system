import { useMutation } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import { useAuthStore } from '../store/useAuthStore';
import type { RegisterInput } from '../schema';
import type { RegisterResponse } from '../types';

export function useRegisterMutation() {
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: async (input: RegisterInput): Promise<RegisterResponse> => {
      const { fullName, email, password } = input;
      const { data } = await apiClient.post<RegisterResponse>('/auth/register', {
        fullName,
        email,
        password,
      });
      return data;
    },
    onSuccess: (data) => {
      setAuth(data.user, data.tokens.accessToken, data.tokens.refreshToken);
    },
  });
}
