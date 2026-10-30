# Mobile Maps, Places and Weather Integration

## Providers

- Geoapify Geocoding API searches place names and addresses. Its API key stays on the backend.
- Geoapify Routing API returns ordered travel routes and encoded polylines. Its API key stays on the backend.
- `react-native-maps` renders Google Maps on Android and iOS. Its platform SDK keys are embedded in native builds and must be restricted to this app; they are not private server credentials.
- Weather uses the existing Open-Meteo backend provider and needs no API key.

Geoapify's results are based on OpenStreetMap/open data and may be stored under its terms, provided the required Geoapify and data-source attribution remains visible. The mobile search results and map overlay display `Powered by Geoapify · © OpenStreetMap contributors`.

## Backend configuration

Set `GEOAPIFY_API_KEY` in the backend environment. Enable Geocoding and Routing APIs for that key in Geoapify MyProjects. The maps endpoints return HTTP 503 when it is missing.

`GET /api/v1/places/search?query=...&latitude=...&longitude=...` performs a Vietnam-filtered search; coordinates are optional but must be supplied together. If supplied, they bias results by proximity. `radiusMeters` optionally sets the bias circle.

`POST /api/v1/routes` accepts origin, destination, optional intermediate points, and a travel mode (`DRIVE`, `WALK`, `BICYCLE`, or `TWO_WHEELER`). The API preserves waypoint order and returns distance in meters, duration in seconds, and a precision-5 encoded polyline.

## Mobile map setup

Set `GOOGLE_MAPS_ANDROID_API_KEY` and `GOOGLE_MAPS_IOS_API_KEY` in the mobile environment before building. Restrict the Android key to package `com.travel.mobile`, the debug/release signing SHA-1 fingerprints, and Maps SDK for Android. Restrict the iOS key to bundle ID `com.travel.mobile` and Maps SDK for iOS. Keep these separate from `GEOAPIFY_API_KEY`.

Rebuild the native app after changing either key (`npx expo run:android`, `npx expo run:ios`, or EAS development build). Expo's `react-native-maps` integration can also be tried in Expo Go.

## Weather

Mobile uses `GET /api/v1/public/weather/forecast`, based on coordinates from the selected trip day's first mapped stop. The backend defaults to Open-Meteo (`WEATHER_PROVIDER=open-meteo`). A day without a geocoded stop cannot request weather, and dates outside the provider's forecast range may have no result.

## Billing

Geoapify has plan-based quotas and credit usage. Check current plan limits and pricing before using the integration beyond development; set usage limits in the provider console. Google Maps Platform map rendering also uses its own billing and quota rules. Keep the Places/Routing server key separate from the Android/iOS map display keys.
