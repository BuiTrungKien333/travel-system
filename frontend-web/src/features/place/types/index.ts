export interface Place {
  id: string;
  providerPlaceId?: string;  // Google Places ID
  name: string;
  address?: string;
  latitude: number;
  longitude: number;
  provinceId?: string;
  provinceName?: string;
  category?: string;
  imageUrl?: string;
  createdAt?: string;
}

export interface TripPlace {
  id: string;
  tripId: string;
  placeId: string;
  place: Place;
  priority?: number;
  estimatedDuration?: number; // minutes
  notes?: string;
  createdAt: string;
}

export interface AddPlaceRequest {
  tripId: string;
  providerPlaceId?: string;
  name: string;
  address?: string;
  latitude: number;
  longitude: number;
  category?: string;
}
