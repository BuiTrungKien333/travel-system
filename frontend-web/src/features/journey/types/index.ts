export interface Province {
  id: string;
  code: string;
  name: string;
}

export interface JourneyStats {
  exploredProvinces: Province[]; // provinces that have Memory → Place → Province
  totalProvinces: number;        // always 63 for Vietnam
  totalTrips: number;
  totalPlaces: number;
  totalMemories: number;
  totalPhotos: number;
}

/**
 * Rule (BR-08 / FR-24):
 * A province is Explored ONLY when:
 *   Memory exists → linked to a Place → Place belongs to that Province
 * NOT when Place is only in Itinerary without any Memory.
 */
