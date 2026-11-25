import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import type { LoginResponse } from "@/features/auth/types";
const baseURL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";
const apiClient = axios.create({ baseURL, timeout: 15_000 });
const refreshClient = axios.create({ baseURL, timeout: 15_000 });
let refreshPromise: Promise<LoginResponse> | null = null;
export interface ApiErrorPayload {
  status: number;
  code: string;
  message: string;
  errors?: Record<string, string>;
}
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly errors?: Record<string, string>;
  constructor(payload: ApiErrorPayload) {
    super(payload.message);
    this.name = "ApiError";
    this.status = payload.status;
    this.code = payload.code;
    this.errors = payload.errors;
  }
}
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token && !config.url?.startsWith("/auth/"))
    config.headers.Authorization = `Bearer ${token}`;
  return config;
});
apiClient.interceptors.response.use(
  (response) => {
    if (response.data?.success === true && "data" in response.data)
      response.data = response.data.data;
    return response;
  },
  async (error: AxiosError<ApiErrorPayload>) => {
    const config = error.config as
      (InternalAxiosRequestConfig & { retried?: boolean }) | undefined;
    const session = useAuthStore.getState();
    if (
      error.response?.status === 401 &&
      config &&
      !config.retried &&
      !config.url?.startsWith("/auth/") &&
      session.refreshToken
    ) {
      config.retried = true;
      // A delayed 401 may arrive after another request already refreshed the token.
      if (
        session.token &&
        config.headers.Authorization !== `Bearer ${session.token}`
      ) {
        return apiClient(config);
      }
      const originalRefreshToken = session.refreshToken;
      try {
        refreshPromise ??= refreshClient
          .post("/auth/refresh", { refreshToken: originalRefreshToken })
          .then((response) => response.data.data as LoginResponse)
          .finally(() => {
            refreshPromise = null;
          });
        const auth = await refreshPromise;
        // Do not restore an old session after logout or account switching.
        const current = useAuthStore.getState();
        if (
          current.refreshToken !== originalRefreshToken &&
          current.refreshToken !== auth.refreshToken
        )
          throw error;
        current.setAuth(auth.user, auth.accessToken, auth.refreshToken);
        config.headers.Authorization = `Bearer ${auth.accessToken}`;
        return apiClient(config);
      } catch (refreshError) {
        if (
          useAuthStore.getState().refreshToken === originalRefreshToken &&
          axios.isAxiosError(refreshError) &&
          [400, 401, 403].includes(refreshError.response?.status ?? 0)
        ) {
          useAuthStore.getState().clearAuth();
        }
        return Promise.reject(refreshError);
      }
    }
    if (error.response?.status === 401 && !config?.url?.startsWith("/auth/"))
      session.clearAuth();
    const payload = error.response?.data;
    return Promise.reject(
      new ApiError({
        status: error.response?.status ?? 0,
        code: payload?.code ?? error.code ?? "NETWORK_ERROR",
        message:
          payload?.message ??
          (error.response
            ? error.message
            : "Không thể kết nối máy chủ. Vui lòng thử lại."),
        errors: payload?.errors,
      }),
    );
  },
);
export default apiClient;
