import { useCallback, useEffect, useRef } from "react";
import { useMutation } from "@tanstack/react-query";
import apiClient from "@/lib/api";
import {
  navigationPayload,
  requireGeoapifyRoute,
  type TripNavigationInput,
  type NavigationRoute,
} from "../navigation";

export function useTripNavigationMutation(tripId: string) {
  const active = useRef<AbortController | null>(null);
  useEffect(
    () => () => {
      active.current?.abort();
    },
    [tripId],
  );
  const mutation = useMutation({
    mutationFn: async (input: TripNavigationInput) => {
      active.current?.abort();
      const controller = new AbortController();
      active.current = controller;
      if (!navigator.geolocation)
        throw new Error(
          "Trình duyệt không hỗ trợ vị trí hiện tại. Vui lòng dùng trình duyệt có hỗ trợ định vị.",
        );
      const position = await new Promise<GeolocationPosition>(
        (resolve, reject) =>
          navigator.geolocation.getCurrentPosition(
            resolve,
            (error) =>
              reject(
                new Error(
                  error.code === 1
                    ? "Cần cho phép truy cập vị trí để tìm đường từ nơi bạn đang đứng."
                    : "Chưa xác định được vị trí hiện tại. Vui lòng thử lại.",
                ),
              ),
            { enableHighAccuracy: true, timeout: 15_000, maximumAge: 30_000 },
          ),
      );
      if (controller.signal.aborted)
        throw new Error("Đã hủy yêu cầu chỉ đường.");
      const origin = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
      };
      const { data } = await apiClient.post<NavigationRoute>(
        "/navigation/routes",
        navigationPayload(tripId, input, origin),
        { signal: controller.signal, timeout: 45_000 },
      );
      if (controller.signal.aborted)
        throw new Error("Đã hủy yêu cầu chỉ đường.");
      return requireGeoapifyRoute(data);
    },
  });
  const resetMutation = mutation.reset;
  const reset = useCallback(() => {
    active.current?.abort();
    resetMutation();
  }, [resetMutation]);
  return { ...mutation, reset };
}
