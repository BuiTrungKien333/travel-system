# Hướng Dẫn Sử Dụng Bộ Test Postman: Travel Planning & AI Copilot Suite

Tài liệu này hướng dẫn chi tiết cách import và chạy bộ kiểm thử tự động API bằng **Postman** hoặc **Newman CLI** cho toàn bộ các module vừa hoàn thiện:
1. **Weather Module** (Dự báo thời tiết thực tế theo tọa độ GPS - Open-Meteo & Mock fallback)
2. **TSP Held-Karp Route Optimizer** (Thuật toán tối ưu đường đi ngắn nhất)
3. **Deterministic Rule Evaluator** (Thẩm định giờ đóng/mở cửa, thời tiết rủi ro ngoài trời, tính khả thi di chuyển)
4. **AI Travel Copilot & Unified Pipeline** (Gemini 3.8 Flash, HITL Human-In-The-Loop, Gatekeeper "Silent by default")

---

## 📁 Cấu Trúc Thư Mục Postman

```
travel-system/
└── postman/
    ├── Travel_Planning_AI_Weather_Collection.postman_collection.json  # Toàn bộ API requests + Test scripts
    ├── Travel_System_Local.postman_environment.json                 # Biến môi trường (baseUrl, token, cardId)
    └── README.md                                                    # Hướng dẫn chi tiết này
```

---

## 🚀 Hướng Dẫn Cài Đặt & Sử Dụng

### 1. Import vào Postman GUI
1. Mở ứng dụng **Postman**.
2. Chọn nút **Import** (góc trên bên trái).
3. Kéo thả 2 file sau vào giao diện Postman:
   - `postman/Travel_Planning_AI_Weather_Collection.postman_collection.json`
   - `postman/Travel_System_Local.postman_environment.json`
4. Ở góc trên bên phải của Postman, chọn Environment là: **`Travel System - Local Environment`**.
5. Đảm bảo Backend Spring Boot đang chạy ở cổng `8080` (`http://localhost:8080`).

---

### 2. Chạy tự động bằng Newman (CLI)
Nếu muốn chạy toàn bộ test tự động trong CI/CD hoặc terminal:
```bash
# Cài đặt newman (nếu chưa có)
npm install -g newman

# Chạy toàn bộ collection với environment
newman run postman/Travel_Planning_AI_Weather_Collection.postman_collection.json \
  -e postman/Travel_System_Local.postman_environment.json \
  --reporters cli
```

---

## 📋 Danh Sách Các Kịch Bản Kiểm Thử (14 Test Cases)

### Nhóm 1: Weather Module (`/api/v1/public/weather`)
| # | Tên Request | Method | Mục đích kiểm thử | Test Assertions |
|---|-------------|--------|-------------------|-----------------|
| 1.1 | **GET Current Weather (Đà Nẵng)** | `GET` | Lấy thời tiết hiện tại tại tọa độ (16.0592, 108.2464) | HTTP 200, có `temperature`, `condition`, `outdoorRiskLevel` thuộc SAFE/MODERATE/HIGH/EXTREME |
| 1.2 | **GET Weather Forecast 3 Days (Đà Nẵng)** | `GET` | Dự báo thời tiết 3 ngày | HTTP 200, `dailyForecasts` không rỗng, chứa nhiệt độ max/min và điều kiện thời tiết |
| 1.3 | **GET Current Weather (Hà Nội)** | `GET` | Lấy thời tiết hiện tại tọa độ Hà Nội (21.0285, 105.8542) | HTTP 200, kiểm tra tính linh hoạt đa tọa độ |

---

### Nhóm 2: TSP Route Optimization (Held-Karp) (`/api/v1/public/planning/tsp`)
| # | Tên Request | Method | Mục đích kiểm thử | Test Assertions |
|---|-------------|--------|-------------------|-----------------|
| 2.1 | **POST Optimize Route - Round Trip (Đà Nẵng)** | `POST` | Tối ưu hóa lộ trình 4 điểm xuất phát từ Novotel và quay về Novotel | HTTP 200, `isRoundTrip: true`, 5 điểm thứ tự (điểm đầu = điểm cuối), tổng km > 0 |
| 2.2 | **POST Optimize Route - One Way** | `POST` | Tối ưu lộ trình 1 chiều không quay về điểm xuất phát | HTTP 200, `isRoundTrip: false`, danh sách điểm có đúng N phần tử |
| 2.3 | **POST Optimize Route - Validation Error** | `POST` | Gửi danh sách chỉ có 1 địa điểm (< 2 điểm) | HTTP 400 Bad Request, trả về message lỗi xác thực hợp lệ |

---

### Nhóm 3: Deterministic Rule Evaluator (`/api/v1/public/planning/rules`)
| # | Tên Request | Method | Mục đích kiểm thử | Test Assertions |
|---|-------------|--------|-------------------|-----------------|
| 3.1 | **POST Evaluate Schedule - Clean** | `POST` | Lịch trình hoàn hảo, không có xung đột giờ mở cửa hay di chuyển | HTTP 200, `shouldTriggerAi: false`, `hasCriticalIssues: false`, `detectedIssues` rỗng |
| 3.2 | **POST Evaluate Schedule - Opening Hours Conflict** | `POST` | Đến bảo tàng lúc 18:30 trong khi địa điểm đóng cửa lúc 17:00 | HTTP 200, `shouldTriggerAi: true`, phát hiện issue loại `OPENING_HOURS`, mức độ HIGH |
| 3.3 | **POST Evaluate Schedule - Impossible Transit Time** | `POST` | Di chuyển 35km (lên Bà Nà Hills) nhưng thời gian dự kiến chỉ có 5 phút | HTTP 200, `shouldTriggerAi: true`, phát hiện issue loại `TRANSIT_TIME` |

---

### Nhóm 4: AI Travel Copilot Pipeline (Gemini 3.8 Flash) (`/api/v1/public/planning/itinerary`)
| # | Tên Request | Method | Mục đích kiểm thử | Test Assertions |
|---|-------------|--------|-------------------|-----------------|
| 4.1 | **POST Optimize & Audit - Clean Route** | `POST` | Chạy Held-Karp và thẩm định lịch trình không lỗi -> **Gatekeeper kích hoạt** | HTTP 200, **`aiTriggered: false`**, **`copilotAnalysis: null`** (Bảo vệ 0 token tiêu tốn) |
| 4.2 | **POST Optimize & Audit - Conflicted Route** | `POST` | Lộ trình xuất phát muộn gây đóng cửa -> **Gemini 3.8 Flash Copilot kích hoạt** | HTTP 200, **`aiTriggered: true`**, nhận về `greetingAndCaringReminder` và mảng `recommendationCards` (tự động lưu `copilotCardId`) |
| 4.3 | **POST HITL 1-Click Action - SWAP_PLACES** | `POST` | Người dùng chọn bấm "Áp dụng" hoán đổi vị trí 2 địa điểm | HTTP 200, danh sách `optimizedSchedule` được hoán đổi và tự động tái thẩm định |
| 4.4 | **POST HITL 1-Click Action - CHANGE_TIME** | `POST` | Người dùng chọn bấm tăng thời gian đệm di chuyển | HTTP 200, cập nhật `bufferAddMinutes` và thời gian biểu mới |
| 4.5 | **POST HITL 1-Click Action - DISMISS** | `POST` | Người dùng từ chối đề xuất của AI | HTTP 200, giữ nguyên lịch trình và trả về phản hồi chuẩn |

---

## 💡 Lưu Ý Quan Trọng Về Logic Nghiệp Vụ
1. **Gatekeeper Silent-by-Default**:
   - Ở request `4.1`, khi các địa điểm không vi phạm bất kỳ quy tắc nào, hệ thống **tuyệt đối không gọi AI**. `aiTriggered` sẽ luôn là `false`. Đây là tính năng chủ chốt giúp giảm 70-80% chi phí gọi Gemini API.
2. **Dynamic Chaining Trong Postman**:
   - Khi chạy request `4.2`, script kiểm thử tự động lưu `copilotCardId` vào biến môi trường:
     ```javascript
     pm.environment.set('copilotCardId', firstCard.cardId);
     ```
   - Nhờ đó, các request `4.3`, `4.4`, `4.5` có thể chạy ngay lập tức mà không cần copy paste ID thủ công.
3. **Mô hình Google Gemini**:
   - Toàn bộ backend sử dụng cấu hình mặc định là `gemini-3.8-flash`. Khi gọi thực tế với API key hợp lệ, Gemini 3.8 Flash sẽ phân tích theo ngữ cảnh thời tiết và giờ đóng cửa; nếu không có mạng/key lỗi, hệ thống tự động kích hoạt `FallbackAiCopilotGenerator` đảm bảo uptime 100%.
