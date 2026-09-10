export const ROUTES = {
  // Public / marketing
  home: '/',

  // Auth
  login: '/auth/login',
  register: '/auth/register',

  // Trips
  trips: '/trips',
  tripNew: '/trips/new',
  trip: (tripId: string) => `/trips/${tripId}`,
  tripItinerary: (tripId: string) => `/trips/${tripId}/itinerary`,
  tripMemories: (tripId: string) => `/trips/${tripId}/memories`,
  tripSettings: (tripId: string) => `/trips/${tripId}/settings`,

<<<<<<< HEAD
  // Journey & Memory
  journal: '/journal',
  journey: '/journey',
  memory: '/journal',
  memoryDetail: (memoryId: string) => `/memories/${memoryId}`,
=======
  // Journey
  journey: '/journey',
>>>>>>> 93f4980 (fix ui)

  // Account
  account: '/account',
} as const;
