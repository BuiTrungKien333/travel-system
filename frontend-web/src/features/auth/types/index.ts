export interface User {
  id: string;
  fullName: string;
  email: string;
  avatarUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TravelPreference {
  interests: string[];
  pace: 'Relaxed' | 'Balanced' | 'Active';
  budgetLevel?: 'Budget' | 'Moderate' | 'Comfortable';
  updatedAt: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface LoginResponse extends AuthTokens {
  user: User;
}

export interface RegisterResponse {
  verificationRequired: boolean;
  resendAfterSeconds: number;
}
