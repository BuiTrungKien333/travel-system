# Itinerary API and storage contract

The itinerary API stores one current schedule per trip. If a database contains more than one legacy `itineraries` row for a trip, the row with the most recent `updated_at` is the current schedule; older rows are left untouched.

## Representation

- A day is a `LocalDate` (`YYYY-MM-DD`) and is stored on each `itinerary_items.date` row. The read response groups rows by that date.
- `sequence` is zero-based and contiguous within a day. The order in the response is the itinerary order.
- `arrivalTime` is a local `LocalDateTime` at the destination, without a timezone. This preserves arrivals that pass midnight while `date` remains the day the traveler assigned the stop to.
- `durationMinutes` is stored explicitly. It is not recomputed from `start_time`/`end_time`, since those legacy `TIME` columns cannot represent a duration that crosses midnight. The legacy columns are kept in sync for existing database consumers.
- `note` is attached to the scheduled stop. A place must already be linked to the trip through `trip_places` before it can appear in the itinerary.
- A place can occur at most once per planned day; it may be revisited on another day. An empty day has no item row and is omitted from the read response.
- Saving is a full replacement of all itinerary days/stops. An empty `days` array clears the current schedule. Saving sets the itinerary status to `DRAFT`; applying a proposal sets it to `FINAL`.
- If an itinerary has no row yet, `GET` returns an empty `DRAFT` response with `id: null`; it does not create data as a side effect.

## Endpoints

All routes are authenticated and scoped to the trip owner.

| Method | Route | Behavior |
| --- | --- | --- |
| `GET` | `/api/v1/trips/{tripId}/itinerary` | Read current days and ordered stops |
| `PUT` | `/api/v1/trips/{tripId}/itinerary` | Atomically replace the current schedule |
| `POST` | `/api/v1/trips/{tripId}/itinerary/optimize` | Optimize the saved schedule and return a proposal without saving it |
| `POST` | `/api/v1/trips/{tripId}/itinerary/apply-optimization` | Validate and persist a previously returned proposal |

The optimize operation starts each day at the first saved arrival time, or `08:00` when no stop has an arrival time. It uses the existing Held-Karp and multi-day audit pipeline and returns both the proposal and the itinerary `sourceUpdatedAt` timestamp. Apply sends that timestamp as `expectedUpdatedAt`; if another save happened after optimization, the API returns `409 TRIP_008` instead of overwriting the newer schedule. A valid proposal must contain the same set of places on their original planned dates; apply updates order, arrival, duration, and route-leg metrics while preserving each stop's note.

For apply, send `sourceUpdatedAt` as `expectedUpdatedAt` and pass the returned `proposal` unchanged unless the user edits it.

## Save request example

```json
{
  "days": [
    {
      "date": "2026-11-12",
      "stops": [
        {
          "placeId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
          "sequence": 0,
          "arrivalTime": "2026-11-12T08:30:00",
          "durationMinutes": 90,
          "note": "Mua vé trước"
        }
      ]
    }
  ]
}
```
