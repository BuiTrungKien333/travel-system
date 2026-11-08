import axios, { AxiosError } from "axios";
import { mockService } from "./mock-service";

// ─── Configuration ────────────────────────────────────────────────────────────
// By default, enable mock mode for independent frontend development.
// Set NEXT_PUBLIC_ENABLE_MOCK=false in .env.local when connecting to real Spring Boot backend.
const USE_MOCK = process.env.NEXT_PUBLIC_ENABLE_MOCK !== "false";

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15_000,
});

// ─── Request Interceptor (Mock & Real Auth) ───────────────────────────────────
apiClient.interceptors.request.use(async (config) => {
  // If Mock mode is active, handle mock adapter directly
  if (USE_MOCK) {
    const url = config.url ?? "";
    const method = (config.method ?? "get").toLowerCase();
    const data = typeof config.data === "string" ? JSON.parse(config.data || "{}") : config.data || {};

    // Simulate minor network delay (100–250ms) for realistic UX
    await new Promise((r) => setTimeout(r, 150));

    let mockResponse: unknown = null;

    // 1. Auth routes
    if (url === "/auth/login" && method === "post") {
      mockResponse = await mockService.login(data.email);
    } else if (url === "/auth/register" && method === "post") {
      mockResponse = await mockService.register();
    } else if (url === "/auth/logout" && method === "post") {
      mockResponse = { success: true };
    } else if (url === "/me" && method === "get") {
      mockResponse = await mockService.getMe();
    }

    // 2. Trips routes
    else if (url === "/trips" && method === "get") {
      mockResponse = await mockService.getTrips();
    } else if (url === "/trips" && method === "post") {
      mockResponse = await mockService.createTrip(data);
    } else if (url.match(/^\/trips\/([^/]+)$/) && method === "get") {
      const tripId = url.replace("/trips/", "");
      mockResponse = await mockService.getTrip(tripId);
    } else if (url.match(/^\/trips\/([^/]+)$/) && method === "put") {
      const tripId = url.replace("/trips/", "");
      mockResponse = await mockService.updateTrip(tripId, data);
    } else if (url.match(/^\/trips\/([^/]+)$/) && method === "delete") {
      const tripId = url.replace("/trips/", "");
      mockResponse = await mockService.deleteTrip(tripId);
    }

    // 3. Itinerary & AI Planning routes
    else if (url.match(/^\/trips\/([^/]+)\/itinerary$/) && method === "get") {
      const tripId = url.replace("/trips/", "").replace("/itinerary", "");
      mockResponse = await mockService.getItinerary(tripId);
    } else if (url.match(/^\/trips\/([^/]+)\/itinerary$/) && method === "put") {
      const tripId = url.replace("/trips/", "").replace("/itinerary", "");
      mockResponse = await mockService.updateItinerary(tripId, data);
    } else if (url.match(/^\/trips\/([^/]+)\/planning$/) && method === "post") {
      const tripId = url.replace("/trips/", "").replace("/planning", "");
      mockResponse = await mockService.generatePlanning(tripId);
    }

    // 4. Memories routes
    else if (url.match(/^\/trips\/([^/]+)\/memories$/) && method === "get") {
      const tripId = url.replace("/trips/", "").replace("/memories", "");
      mockResponse = await mockService.getMemories(tripId);
    } else if (url.match(/^\/trips\/([^/]+)\/memories$/) && method === "post") {
      const tripId = url.replace("/trips/", "").replace("/memories", "");
      mockResponse = await mockService.createMemory(tripId, data);
    } else if (url.match(/^\/memories\/([^/]+)$/) && method === "delete") {
      const memoryId = url.replace("/memories/", "");
      mockResponse = await mockService.deleteMemory(memoryId);
    }

    // 5. Journey progress route
    else if (url === "/me/journey" && method === "get") {
      mockResponse = await mockService.getJourney();
    }

    // 6. Memory Hub aggregate routes
    else if (url === "/me/memories" && method === "get") {
      mockResponse = await mockService.getAllMemories();
    } else if (url === "/me/trips-with-memories" && method === "get") {
      mockResponse = await mockService.getTripsWithMemories();
    }

    if (mockResponse !== null) {
      // Short-circuit the actual network request and return mock response
      config.adapter = async () => ({
        data: mockResponse,
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      });
    }
  }

  // Real backend token injection
  if (typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("auth-storage");
      if (raw) {
        const parsed = JSON.parse(raw);
        const token: string | undefined = parsed?.state?.token;
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      }
    } catch {
      // Ignore
    }
  }

  return config;
});

// ─── Error Handling ───────────────────────────────────────────────────────────
export interface ApiErrorPayload {
  status: number;
  code: string;
  message: string;
  errors?: Array<{ field: string; message: string }>;
}

export class ApiError extends Error {
  public readonly status: number;
  public readonly code: string;
  public readonly errors?: ApiErrorPayload["errors"];

  constructor(payload: ApiErrorPayload) {
    super(payload.message);
    this.name = "ApiError";
    this.status = payload.status;
    this.code = payload.code;
    this.errors = payload.errors;
  }
}

apiClient.interceptors.response.use(
  (response) => {
    if (response.data?.success === true && 'data' in response.data) {
      response.data = response.data.data;
    }
    return response;
  },
  (error: AxiosError<ApiErrorPayload>) => {
    const data = error.response?.data;
    if (data && "code" in data) {
      return Promise.reject(new ApiError({ ...data, status: error.response?.status ?? 0 }));
    }

    const isNetworkError =
      error.code === "ERR_NETWORK" ||
      error.code === "ECONNABORTED" ||
      !error.response;

    const message = isNetworkError
      ? "Không thể kết nối đến máy chủ backend (http://localhost:8080)."
      : error.message ?? "Đã xảy ra lỗi không xác định.";

    return Promise.reject(
      new ApiError({
        status: error.response?.status ?? 0,
        code: error.code ?? "NETWORK_ERROR",
        message,
      })
    );
  }
);

export default apiClient;
