import type { TripStatus } from '@/lib/constants';

export type { TripStatus };

export interface Trip {
  id: string;
  userId: string;
  name: string;
  destination: string;
  startDate: string;  // ISO date string YYYY-MM-DD
  endDate: string;    // ISO date string YYYY-MM-DD
  transportation?: string;
  preferences?: string[];
  budget?: number;
  constraints?: string;
  status: TripStatus;
  coverImage?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TripSummary {
  id: string;
  name: string;
  destination: string;
  startDate: string;
  endDate: string;
  status: TripStatus;
  coverImage?: string;
  placeCount: number;
  memoryCount: number;
}

export interface TripOverview extends Trip {
  placeCount: number;
  visitedCount: number;
  memoryCount: number;
}

export interface CreateTripRequest {
  name: string;
  destination: string;
  startDate: string;
  endDate: string;
  transportation?: string;
  preferences?: string[];
  budget?: number;
  constraints?: string;
  description?: string;
}

export interface UpdateTripRequest extends Partial<CreateTripRequest> {
  status?: TripStatus;
}
