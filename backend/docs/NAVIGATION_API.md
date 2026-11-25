# Navigation API — S1 contract

Status: S1 backend slice implemented and tested, 2026-11-23. S0 source/coverage/account gates
remain unresolved; implementing this contract does not certify nationwide alerts
or enable production navigation. Existing `/api/v1/routes` and trip day route
responses remain unchanged.

## Auth and configuration

All `/api/v1/navigation/**` operations require a valid JWT. Trip mode additionally
checks ownership and the saved day's stop snapshot, before and after provider I/O.

Navigation provider calls are disabled by default. Required deployment settings:

- `NAVIGATION_ENABLED=true`
- `NAVIGATION_FREE_PLAN_CONFIRMED=true`
- `NAVIGATION_CREDIT_LIMIT`: positive app reservation limit, at most 2500.
- `NAVIGATION_CREDITS_PER_CALL`: conservative reservation per routing HTTP call,
  default 64; must cover `2 * number_of_legs + 10` before requests are sent.
- Existing `GEOAPIFY_API_KEY`.

Reservations are persisted for a rolling 24-hour window, serialized with a
PostgreSQL transaction advisory lock. One route request reserves both `balanced`
and `short` calls atomically; a timeout/error does not refund an uncertain charge.
The extra 10 credits per call budgets the long-distance component up to 5000 km;
responses beyond that distance or with a computed cost above the reservation
exhaust the window for subsequent requests and fail closed. This is a conservative
application budget, not an invoice or a proof of the vendor's account hard cap.
Other legacy endpoints/SDK traffic remain outside this reservation ledger; S0's
end-to-end no-billing gate must still be established before enabling deployment.

## POST /api/v1/navigation/routes

Quick request:

```json
{
  "origin": {"latitude": 16.06, "longitude": 108.22},
  "vehicle": "TWO_WHEELER",
  "destinations": [{"name": "Destination", "latitude": 16.08, "longitude": 108.24}]
}
```

`vehicle` is required, `DRIVE` or `TWO_WHEELER`. There must be 1–24 quick
destinations, excluding origin; repeated points are preserved. Quick mode cannot
include trip-only fields. Names are optional, at most 200 characters. Coordinates
must be finite and inside the supported box (latitude 7–24, longitude 102–114); results must
report only `vn` country codes. Bounds are a request limit, not a legal boundary
dataset or proof that every route is safe/legal for the selected vehicle.

Trip request replaces `destinations` with:

```json
{
  "trip": {
    "tripId": "11000000-0000-4000-8000-000000000001",
    "date": "2026-12-01",
    "nextStopIndex": 0,
    "expectedPlaceIds": ["21000000-0000-4000-8000-000000000001"]
  }
}
```

`origin` is the user's current location. The server derives stops from the saved
day, validates the complete `expectedPlaceIds` order, and selects the suffix from
`nextStopIndex`. If the day returns to its saved origin, that return is appended
without replacing the live departure point. No stop is auto-visited and no
itinerary/trip record is updated. A changed saved snapshot returns 409 instead of
publishing the stale result. The client must still discard responses for a prior
account, session, vehicle, or request.

Response: normal `ApiResponse.data`, containing `routeId`, `generatedAt`, `vehicle`,
ordered `waypoints` (origin first), `placeIds` for the selected saved suffix (empty
for quick mode), `distanceMeters`, `durationSeconds`, `provider`, `preference`,
`dataAvailability` and ordered `legs`. Each leg contains metrics, explicit
`geometry` positions `{latitude,longitude}` and ordered `steps`; a step contains
leg-local `fromIndex/toIndex`, metrics, and nullable `maneuver` metadata
`{type,streets,roundaboutExit}`. Geometry indexes are never global across legs.
Provider route-detail segments often have no maneuver: null means no supplied
instruction for that segment. The recorded fixture has 56 detail steps and eight
maneuvers. The final destination maneuver is required. S2 must format the supplied
maneuvers and keep their geometry indexes; it must not invent turns for null steps.

Leg and route distances remain the provider's totals. Real multi-leg detailed
responses can accumulate small distance differences between the leg total and
the sum of road-detail steps. Accept at most `max(1 metre, step count metres,
0.5% of leg distance)` for that comparison only; time and route-to-leg totals
keep their existing per-item rounding tolerance. Geometry bounds, country,
vehicle, waypoint order and destination-step checks remain mandatory. The mobile
contract uses the same bounded detail-distance tolerance. A translated,
label-free three-leg response and a deliberately corrupted distance are covered
by `NavigationRoutingIntegrationTest`; no live key or saved itinerary is in the fixture.

Select the valid candidate with lower unrounded seconds; ties use lower meters;
exact ties remain stable. Both candidates use the same vehicle, waypoints and
`optimize_stops=false`. `preference=FASTEST_AVAILABLE` means fastest among the two
returned candidates, not a globally fastest route or a live traffic guarantee.
Failing either provider request fails this operation without silently degrading.

Only maneuver metadata is returned, not raw English instructions. S2 owns the
tested Vietnamese formatter. Availability fields `speedLimits` and `roadAlerts`
remain `UNKNOWN`; raw `speed`, `max_speed` or unverified `speed_limit` are not
exposed as a legal vehicle-specific limit. Source/attribution is retained.

## GET /api/v1/navigation/places

`query` is required, length 2–200. Optional `latitude/longitude` must be supplied
together and valid. Uses authenticated Geoapify search restricted to Vietnam,
normalizes existing `PlaceSearchResult` values, and reserves one credit before
provider I/O. No request is made on invalid auth/input/config/quota.

## Errors and compatibility

| HTTP | Code / meaning | Retry |
| --- | --- | --- |
| 400 | existing validation error / invalid trip day | Correct the request. |
| 401 | existing unauthorized code | Reauthenticate. |
| 404 | existing trip/resource not found | Do not retry blindly. |
| 409 | `NAV_003` saved itinerary changed | Refresh and ask client to use the new snapshot. |
| 429 | `NAV_002` app/provider quota exhausted | Wait for window/config resolution; no automatic upstream retry. |
| 502 | `PLACE_005` invalid/unusable provider response | Show route unavailable; do not cache as READY. |
| 503 | `NAV_001` configuration/budget unavailable or `PLACE_004` provider failure | Keep previous local route; bounded user retry after resolution. |

Provider URL, key, raw exception text and raw body are omitted from public errors
and logs. No DB transaction is held during HTTP calls. Navigation usage stores
provider, timestamp and reserved credits only, not owner/location/itinerary data.

Canonical schema gains V20 and recognized legacy history V27 for the same ledger.
Released migration files/checksums are unchanged. S1 adds no production data import
or automatic repair and does not migrate the local development database.

## S4 safety extension

`NavigationResponse` có thêm `safety` (nullable). Trường này chỉ chứa dữ liệu đã duyệt,
gắn đúng `routeId`, vehicle và leg; mỗi zone/event có source và validity window.
`dataAvailability.speedLimits/roadAlerts` có thể là `PARTIAL`, luôn `UNKNOWN` nếu không có dữ liệu tương ứng.
Không dùng `max_speed`/tốc độ ETA của router làm giới hạn pháp lý.
Xem [nguồn OSM và cấu hình](NAVIGATION_OSM_SAFETY.md). Không có endpoint hoặc migration mới.
