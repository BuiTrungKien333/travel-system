export class ApiError extends Error {
  constructor(
    public readonly code: string,
    public readonly status = 500,
  ) {
    super(code);
    this.name = "ApiError";
  }
}
const messages: Record<string, string> = {
  AUTH_001: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
  AUTH_002: "Tài khoản không có quyền truy cập chức năng này.",
  AUTH_004: "Email hoặc mật khẩu không đúng.",
  AUTH_007: "Tài khoản đã bị khóa hoặc không còn hoạt động.",
  AUTH_008: "Vui lòng xác minh email trước khi đăng nhập.",
  ADMIN_001:
    "Tài khoản đã thay đổi. Tải lại danh sách và kiểm tra lại trước khi xác nhận.",
  ADMIN_002: "Không thể tự đổi quyền hoặc khóa tài khoản quản trị đang dùng.",
  ADMIN_003: "Hệ thống phải còn ít nhất một quản trị viên đang hoạt động.",
  ADMIN_004: "Cần xác minh email trước khi cấp quyền quản trị.",
  COMMON_003: "Thông tin chưa hợp lệ. Vui lòng kiểm tra lại.",
  CSRF: "Yêu cầu không hợp lệ. Tải lại trang và thử lại.",
  RATE_LIMIT: "Bạn gửi quá nhiều yêu cầu. Vui lòng thử lại sau.",
  UPSTREAM: "Chưa kết nối được hệ thống. Vui lòng thử lại.",
  INVALID_RESPONSE: "Hệ thống trả về dữ liệu chưa hợp lệ. Vui lòng thử lại.",
  LOGOUT_INCOMPLETE:
    "Đã đóng phiên trên trình duyệt. Chưa xác nhận được việc thu hồi phiên ở backend.",
};
export function errorMessage(error: unknown) {
  if (error instanceof ApiError)
    return error.status === 429
      ? messages.RATE_LIMIT
      : (messages[error.code] ??
          "Không thể hoàn tất thao tác. Vui lòng thử lại.");
  return "Không thể kết nối. Vui lòng kiểm tra mạng và thử lại.";
}
