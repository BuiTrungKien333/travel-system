export function isTripWorkspacePath(pathname: string) {
  return /^\/trips\/(?!new(?:\/|$))[^/]+(?:\/|$)/.test(pathname);
}

export const ROUTES = {
  // Public / marketing
  home: '/',
  travelGuides: '/travel-guides',

  // Auth
  login: '/auth/login',
  register: '/auth/register',

  // Trips
  trips: '/trips',
  tripNew: '/trips/new',
  trip: (tripId: string) => `/trips/${tripId}`,
  tripItinerary: (tripId: string) => `/trips/${tripId}/itinerary`,
  tripMap: (tripId: string) => `/trips/${tripId}/map`,
  tripMemories: (tripId: string) => `/trips/${tripId}/memories`,
  tripSettings: (tripId: string) => `/trips/${tripId}/settings`,

  // Journey & Memory
  journal: '/journal',
  journey: '/journey',
  memory: '/journal',
  memoryDetail: (memoryId: string) => `/memories/${memoryId}`,

  // Account
  account: '/account',
} as const;
