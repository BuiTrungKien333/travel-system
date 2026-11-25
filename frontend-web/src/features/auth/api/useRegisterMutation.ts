import { useMutation } from '@tanstack/react-query';
import apiClient from '@/lib/api';
import type { RegisterInput } from '../schema';
import type { RegisterResponse } from '../types';

export function useRegisterMutation() {
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
  });
}
